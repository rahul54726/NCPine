## NPCine API Gateway – Overview

This module is the **entry point** for the NPCine backend, built with **Spring Boot 4 / Spring Cloud Gateway (MVC)**.  
It currently:
- **Routes** incoming HTTP traffic:
  - `/auth/**` → `user-service` at `http://localhost:8080`
  - `/bookings/**` → `booking-service` at `http://localhost:8082`
- **Secures** most routes using a **JWT-based authentication filter**.
- Exposes **actuator health/info** endpoints for basic monitoring.

### Tech stack (gateway)
- **Language**: Java 21
- **Frameworks**: Spring Boot, Spring Cloud Gateway (server-webmvc)
- **Security**: Spring Security, custom `JwtAuthenticationFilter`, `JwtUtil` with `jjwt`
- **Observability**: Spring Boot Actuator (health, info)

## Current Architecture

At a high level:
- **Clients** call the **API Gateway** on port `8081`.
- The gateway evaluates **route predicates** (paths) and forwards to the target service.
- A **JWT filter** sits in front of protected routes and blocks unauthorized traffic.

High-level flow:

```mermaid
flowchart LR
    Client -->|HTTP| APIGW[API Gateway (8081)]
    APIGW -->|/auth/**| USER[User Service (8080)]
    APIGW -->|/bookings/**| BOOK[Booking Service (8082)]
    APIGW -->|/actuator/**| ACT[Gateway Actuator Endpoints]
```

### Request lifecycle (simplified)
1. **Client** sends a request to the gateway (e.g. `GET /bookings/...`).
2. `SecurityConfig` configures the Spring Security filter chain.
3. `JwtAuthenticationFilter` runs for all routes except `/auth/**` and `/actuator/**`:
   - Reads `Authorization: Bearer <token>` header.
   - If missing or invalid, responds with **401 Unauthorized**.
   - If valid, continues the filter chain.
4. Spring Cloud Gateway MVC matches the request against configured routes:
   - `/auth/**` → `user-service`
   - `/bookings/**` → `booking-service`
5. The gateway forwards the request and returns the downstream response to the client.

## Authentication & Authorization Design

- **JWT validation**:
  - `JwtUtil` uses a shared symmetric secret (`jwt.secret` property) to validate tokens.
  - Validates the signature and (indirectly) expiry/claims using `jjwt`.
- **Filter behavior**:
  - Bypasses `/auth/**` and `/actuator/**` so login and health checks stay open.
  - Requires a valid `Bearer` token on other routes (including `/bookings/**`).
  - Currently only **validates** the token; it does **not yet** populate a `SecurityContext` with user details.

## Production-Readiness – Key Improvements

Below are **concrete enhancements** that would move this gateway closer to production grade.

### 1. Configuration & Secrets
- **Externalize secrets**:
  - Move `jwt.secret` out of `application.properties` into **environment variables**, Kubernetes **Secrets**, or a **secret manager** (Vault, AWS Secrets Manager, etc.).
  - Use **different secrets per environment** (dev / staging / prod).
- **Profile-specific config**:
  - Create `application-dev.properties`, `application-prod.properties` and override:
    - Target URIs for services.
    - Logging levels.
    - Security-related toggles (CORS, debug logs, etc.).
- **Avoid localhost in prod**:
  - Replace `http://localhost:8080/8082` with:
    - Service discovery (Eureka/Consul/Kubernetes service names), or
    - Environment-driven hostnames (`${USER_SERVICE_URL}`, `${BOOKING_SERVICE_URL}`).

### 2. Security Hardening
- **Align security rules**:
  - Today `/bookings/**` is `permitAll` in `SecurityConfig` but still enforced by `JwtAuthenticationFilter`.  
    Decide clearly whether bookings should be **public** or **protected**, and make both places consistent.
- **Populate `SecurityContext`**:
  - After validating the JWT, extract user information (e.g. email, roles) and set an `Authentication` object in the `SecurityContextHolder`.
  - This lets downstream services rely on `Principal`/`SecurityContext` if they sit behind the gateway.
- **Validate more JWT claims**:
  - Check **issuer**, **audience**, **expiration**, and possibly **roles/scopes** explicitly.
- **CORS configuration**:
  - Add explicit CORS rules (allowed origins, methods, headers) instead of relying on defaults.
- **HTTPS & headers**:
  - Terminate TLS at the gateway or at an ingress in front of it.
  - Add security headers (HSTS, X-Content-Type-Options, X-Frame-Options) via filters if the gateway is internet-facing.

### 3. Observability & Diagnostics
- **Reduce log level**:
  - `logging.level.org.springframework.cloud.gateway=TRACE` is good for local debugging but too noisy for prod.
  - Use `INFO`/`WARN` in production and enable TRACE selectively via profiles or log configuration.
- **Structured logging & correlation IDs**:
  - Add a filter that generates/propagates a **correlation ID** (`X-Request-ID`), logging it on every request and forwarding it to downstream services.
  - Prefer JSON/structured logs if your log stack supports it (e.g. ELK, Loki, CloudWatch).
- **Metrics & health**:
  - Expose additional actuator endpoints (metrics, prometheus, loggers) *secured* behind auth or network boundaries.
  - Configure readiness/liveness probes if running in Kubernetes.

### 4. Resilience & Traffic Control
- **Timeouts & error handling**:
  - Configure **connect and read timeouts** on routes so slow downstream services don’t hang clients.
  - Standardize error responses from the gateway (JSON body with `code`, `message`, `traceId`).
- **Circuit breakers & retries**:
  - Use Spring Cloud CircuitBreaker (with Resilience4j) to:
    - Open circuits around unstable downstream services.
    - Apply **retries** only for idempotent operations (e.g. GETs).
- **Rate limiting / throttling**:
  - Add rate limiting per API key, IP, or user to protect your backend from abuse.

### 5. Routing & API Design
- **Versioned APIs**:
  - Introduce versioned paths (`/api/v1/...`) at the gateway level for smoother evolution.
- **Request/response shaping**:
  - If needed, add pre/post filters to:
    - Enforce common headers.
    - Transform error payloads into a unified format.
- **Static rules vs. discovery**:
  - Consider integrating with **service discovery** so you don’t hardcode host/port and can scale backends dynamically.

### 6. Testing & Quality
- **Integration tests**:
  - Write tests that:
    - Verify routing: `/auth/**` and `/bookings/**` go to the correct URIs.
    - Verify security: protected routes return **401** without JWT and **200** with valid JWT.
  - Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with WebTestClient/RestTemplate.
- **JWT utility tests**:
  - Add tests for `JwtUtil` to cover:
    - Valid tokens.
    - Expired tokens.
    - Tokens with wrong signature/issuer/audience.

## Summary of Current State

- **You already have:**
  - A working Spring Cloud Gateway (MVC) app on port `8081`.
  - Path-based routing to **user service** (`/auth/**`) and **booking service** (`/bookings/**`).
  - A JWT-based authentication filter that blocks unauthorized access to non-public paths.
  - Basic actuator health/info endpoints and a clean, minimal codebase.
- **To reach production grade**, focus next on:
  - Moving secrets and URLs out of source, adding observability/correlation IDs,  
  - Hardening security (JWT claims, CORS, HTTPS, consistent rules),  
  - Adding resilience features (timeouts, circuit breakers, rate limits), and  
  - Strengthening tests around routing and security behavior.

