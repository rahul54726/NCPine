# Booking Service — Overview & Production Readiness

## What This Service Does (Current State)

**Booking Service** is the cinema seat-booking backend for **Cinebook**. It lets authenticated users lock seats for a show, confirm a booking (after payment intent), or release seats. It is designed to work with a separate API gateway and user service that issue JWTs.

### Core Capabilities

| Feature | Description |
|--------|-------------|
| **Seat locking** | User locks one or more seats for a show. Locks are held in **Redis** (5 min TTL) and reflected in **PostgreSQL** (`Seat.status = LOCKED`). Uses DB pessimistic locking to avoid double-booking races. |
| **Booking confirmation** | User confirms previously locked seats. Service creates **CONFIRMED** booking rows, sets `Seat.status = BOOKED`, and releases Redis locks. |
| **Seat release** | User voluntarily releases locked seats. Redis keys are removed and `Seat.status` set back to `AVAILABLE`. |
| **Expiry of stale locks** | A scheduler runs every 30 seconds and expires **Booking** rows with status `LOCKED` and `expiresAt < now`, then frees the corresponding seats and Redis keys. |

### API Endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| `GET` | `/bookings/health` | No | Health check (service name + time). |
| `POST` | `/bookings/lock-seats` | JWT | Lock seats for a show (body: `showTimeId`, `seatNumbers`). |
| `POST` | `/bookings/confirm` | JWT | Confirm booking for locked seats (same body). |
| `POST` | `/bookings/release-seats` | JWT | Release locked seats (same body). |

All booking endpoints use **JWT**; the authenticated user is taken from `Principal.getName()` (email). `/actuator/**` is unauthenticated for monitoring.

### Tech Stack

- **Java 21**, **Spring Boot 4.0.3**, **Maven**
- **PostgreSQL** (e.g. `cinebook_booking_db` on port 5433): `bookings`, `seats`, `showtimes`, `idempotency_records`
- **Redis** (e.g. localhost:6379): distributed seat locks with key pattern `seat:lock:{showTimeId}:{seatNumber}`
- **Spring Security**: stateless JWT filter; no form login or HTTP Basic
- **Spring Data JPA**: entities `Booking`, `Seat`, `Showtime`, `IdempotencyRecord`; pessimistic lock on seat select for lock flow

### Data Model (Summary)

- **Booking**: `userEmail`, `showtimeId`, `seatNumber`, `amount` (currently fixed 250.0), `status` (e.g. LOCKED, CONFIRMED, EXPIRED), `createdAt`, `expiresAt`
- **Seat**: `showTimeId`, `seatNumber`, `status` (AVAILABLE, LOCKED, BOOKED)
- **Showtime**: `movieId`, `screenId`, `startTime`, `endTime`
- **IdempotencyRecord**: `idempotencyKey`, `userEmail`, `responseHash`, `createdAt` (implemented but **not used** in any endpoint yet)

### Flow (As Implemented)

1. **Lock**  
   `lock-seats` → for each seat: pessimistic lock row, check not BOOKED/LOCKED, set Redis key (userEmail, 5 min TTL), set `Seat.status = LOCKED`, save.  
   **Note:** No **Booking** rows with status `LOCKED` are created in this flow (`createLockedBookings` exists but is never called).

2. **Confirm**  
   `confirm` → load seats by `showTimeId` + `seatNumbers`, ensure each is LOCKED, set `Seat.status = BOOKED`, create new **Booking** rows with status CONFIRMED, then release Redis locks for those seats.

3. **Release**  
   `release-seats` → load seats, if LOCKED set to AVAILABLE and delete Redis key.

4. **Expiry**  
   Scheduler selects `Booking` with `status = LOCKED` and `expiresAt < now`, marks them EXPIRED, frees the corresponding seat and Redis lock.  
   **Gap:** With the current API, no LOCKED bookings are ever created, so the scheduler has no such rows to expire. Redis keys expire by TTL, but `Seat` rows can remain LOCKED unless the user calls `release-seats`.

---

## Gaps / Inconsistencies to Fix

1. **Lock flow and expiry**  
   Either call `createLockedBookings` after a successful lock (so the scheduler can expire them and free seats), or change the scheduler to expire based on **Seat** (e.g. LOCKED seats with a lock timestamp or “lock expires at”) so that abandoned locks are cleaned without LOCKED booking rows.

2. **Idempotency**  
   `IdempotencyService` and `IdempotencyRecord` are in place but not used. For production, critical operations (e.g. confirm, and optionally lock-seats) should accept an idempotency key (e.g. header `Idempotency-Key`) and return a cached response for duplicates.

3. **Confirm ownership**  
   Confirm does not check that the seats are locked by the same user (e.g. via `SeatLockService.isSeatLockedByUser` or stored user on a booking/lock). A user could try to confirm another user’s lock; this should be validated and rejected.

---

## What You Can Do Next (Production-Grade Improvements)

### Reliability & correctness

- **Wire lock → LOCKED bookings and expiry**  
  After a successful `lock-seats`, create LOCKED booking rows (e.g. call `createLockedBookings`) with `expiresAt = now + 5 minutes` so the scheduler can expire them and set seats back to AVAILABLE when users abandon the flow.
- **Use idempotency**  
  For `POST /bookings/confirm` (and optionally `POST /bookings/lock-seats`), require or support `Idempotency-Key`. Store key + response (or hash) in `IdempotencyRecord` and return the same response for repeated requests with the same key.
- **Validate lock ownership on confirm**  
  Before confirming, ensure every seat is locked by the current user (e.g. Redis value or DB) and reject with 403/409 if not.

### Security & configuration

- **Secrets and config**  
  Move JWT secret, DB password, and Redis credentials out of `application.yml` into environment variables or a secret manager (e.g. Spring Cloud Config, Vault). Use placeholders like `${JWT_SECRET}`.
- **Tighten security rules**  
  Prefer explicit deny for unknown paths (e.g. allow only `/actuator/**` and `/bookings/**`, deny `anyRequest()`), and consider rate limiting on booking endpoints.
- **Remove debug logging**  
  Remove `System.out.println` and raw auth header logging from `JwtAuthenticationFilter` (and any similar code) in production.

### API & validation

- **Request validation**  
  Add Bean Validation on `LockSeatsRequest` (`@NotBlank` showTimeId, `@NotEmpty` seatNumbers, size limits) and use `@Valid` in the controller. Return 400 with clear messages for invalid input.
- **Structured error responses**  
  Replace ad-hoc strings with a small error DTO (e.g. `code`, `message`, `timestamp`) and use `@ControllerAdvice` for consistent error handling and status codes.
- **API versioning**  
  Consider a path prefix (e.g. `/api/v1/bookings`) or header for versioning so you can evolve the API without breaking clients.

### Operations & observability

- **Health checks**  
  Use Spring Boot Actuator’s health (e.g. `/actuator/health`) with DB and Redis health indicators so orchestrators (Kubernetes, etc.) can depend on real dependencies. Optionally expose a readiness probe that fails when DB/Redis are down.
- **Logging**  
  Use a logging framework (SLF4J/Logback) with structured logs (e.g. JSON), and log correlation IDs (from gateway or generated) so you can trace a request across services.
- **Metrics**  
  Expose metrics (e.g. `/actuator/prometheus` or Micrometer) for lock/confirm/release counts, latency, and errors. Add alerts for high failure rates or latency.
- **Tracing**  
  Add distributed tracing (e.g. Sleuth/Micrometer Tracing with OpenTelemetry) so lock/confirm flows can be traced across gateway and other services.

### Data & schema

- **JPA / DB**  
  In production, set `spring.jpa.hibernate.ddl-auto` to `validate` (or `none`) and use Flyway/Liquibase for versioned migrations instead of `update`. Turn off `show-sql` and use a proper logging level for SQL if needed.
- **Indexes**  
  Add indexes for hot queries (e.g. `(showtime_id, seat_number)`, `(status, expires_at)` for bookings) and align with your repository methods.

### Resilience & performance

- **Timeouts and connection pools**  
  Configure connection pool sizes and timeouts for PostgreSQL and Redis, and timeouts for outbound calls if you add any (e.g. payment or user service).
- **Circuit breaker**  
  If you call external services (payment, user), use Resilience4j or similar for circuit breakers and retries with backoff.
- **Duplicate dependency**  
  In `pom.xml`, `spring-boot-starter-security` is declared twice; remove the duplicate.

### Testing

- **Unit tests**  
  Add tests for `BookingService`, `SeatLockOrchestrator`, `SeatLockService`, and `IdempotencyService` (including idempotent behavior once wired).
- **Integration tests**  
  Add tests for `POST /bookings/lock-seats`, `confirm`, and `release-seats` with Testcontainers for PostgreSQL and Redis, and mock or test JWT generation.
- **Load / concurrency**  
  Add tests or scripts that simulate concurrent lock/confirm on the same seats to validate pessimistic locking and Redis behavior under load.

### Deployment & docs

- **Profile-based config**  
  Use `application-prod.yml` (or env-only) for production (no dev defaults, health details as needed, logging level, etc.).
- **README**  
  Add a short README: how to run (DB + Redis + app), required env vars, and main API contract (or link to OpenAPI).
- **OpenAPI**  
  Add SpringDoc OpenAPI or Swagger and describe endpoints, request/response bodies, and errors so clients and frontends stay in sync.

---

## Summary

The service already provides a solid base: JWT auth, Redis-based distributed locks, DB pessimistic locking, and a clear separation between lock, confirm, and release. To make it production-grade, focus first on: (1) aligning the lock flow with LOCKED bookings and expiry (or seat-based expiry), (2) using idempotency for confirm (and optionally lock), (3) validating lock ownership on confirm, (4) externalizing secrets and improving security/logging, (5) schema migrations and health checks, and (6) structured errors, validation, and tests.
