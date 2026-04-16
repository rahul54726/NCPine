# NPCine Interview Questions and Best Answers

## How to use this document
- Focus on understanding, not memorizing.
- Keep answers concise in interview (60-120 seconds each).
- Use NPCine-specific examples to show practical ownership.

## 1) Tell me about your project architecture.
**Best answer:**  
NPCine is a microservices-based cinema booking platform. The frontend talks only to an API Gateway (`:8081`), which routes requests to domain services: user-service (`:8080`), booking-service (`:8082`), payment-service (`:8083`), and notification-service (`:8084`).  
User-service manages auth and JWT issuance with MongoDB. Booking and payment use PostgreSQL. Booking also uses Redis for distributed seat locking. Payment publishes success events to RabbitMQ, which are consumed by booking for confirmation and by notification for email delivery. This gives us clear service boundaries and asynchronous reliability for post-payment workflows.

## 2) Why did you choose microservices instead of a monolith?
**Best answer:**  
The domain naturally splits into identity, booking, payment, and notifications, each with different scaling and reliability requirements. Microservices let us deploy and scale these independently. For example, notification traffic can spike without affecting booking latency. It also allows different storage choices (MongoDB for users, PostgreSQL for transactional booking/payment).

## 3) What role does the API Gateway play?
**Best answer:**  
The API Gateway is the single entry point for clients. It centralizes routing, security checks, and service abstraction. Frontend never needs internal service URLs, only one base URL. It also supports compatibility rewrites (`/login` -> `/auth/login`) and protects internal topology from client coupling.

## 4) Explain your authentication flow.
**Best answer:**  
Users register/login via user-service endpoints through gateway. On successful login, user-service returns a JWT. The frontend sends this token in `Authorization: Bearer <token>` for protected APIs like seat locking and payments. Gateway and downstream services validate JWT for protected routes.

## 5) How do you prevent double booking of seats?
**Best answer:**  
We use a two-layer approach. First, booking-service checks seat state in PostgreSQL. Second, it creates Redis distributed locks for selected seats with a payment window. Booking rows are stored as `LOCKED` with expiry. On payment success, seats move to `BOOKED`; otherwise locks expire/release. This handles concurrent requests and race conditions.

## 6) Why use Redis if seat status already exists in DB?
**Best answer:**  
DB state alone is not enough under high concurrency because multiple transactions can read availability before updates commit. Redis locks provide fast, distributed mutual exclusion across instances, reducing race windows and improving lock acquisition performance.

## 7) Describe the booking lifecycle states.
**Best answer:**  
Typical flow is `AVAILABLE -> LOCKED -> BOOKED`.  
If payment fails or expires, `LOCKED` transitions back to `AVAILABLE` and booking records move to `EXPIRED`. This explicit state machine keeps business logic predictable and audit-friendly.

## 8) How is payment integrated?
**Best answer:**  
Payment-service has two key APIs: create order and verify payment. Create order stores a payment record and returns an order ID. Verify marks payment success and publishes a RabbitMQ event including booking IDs and user email. This decouples payment confirmation from downstream booking/notification side effects.

## 9) Why use asynchronous messaging after payment?
**Best answer:**  
Asynchronous fan-out improves resilience and response time. Payment API can respond quickly after verification while booking confirmation and email sending happen independently. If notification is slow, booking confirmation is not blocked. RabbitMQ also supports retry and better failure isolation.

## 10) What happens if notification service is down?
**Best answer:**  
Payment verification and booking confirmation can still succeed because they are decoupled via queue-based events. Notification events stay queued until the consumer recovers. This avoids user-facing checkout failure due to non-critical email delivery issues.

## 11) How do you handle idempotency?
**Best answer:**  
Booking APIs support `Idempotency-Key`. Frontend can safely retry lock/confirm operations without duplicating bookings when network errors occur. Server checks existing result for the same key and returns cached/consistent response.

## 12) What database choices did you make and why?
**Best answer:**  
MongoDB for user-service because user profiles/auth documents map naturally to flexible document structures. PostgreSQL for booking/payment because they are transactional domains requiring consistency, constraints, and relational querying.

## 13) What were major production-like bugs you solved?
**Best answer:**  
One major issue was gateway returning `404` for `/auth/login` due to incorrect gateway property namespace. Fixing route keys restored route registration.  
Another issue was Docker connectivity (`localhost` misuse) across services. We switched to container hostnames (like `rabbitmq_broker`) and environment-based config for stable container-to-container communication.

## 14) How do you secure service-to-service and client APIs?
**Best answer:**  
Protected endpoints enforce JWT validation, while auth and health endpoints remain public as needed. Sensitive config values are externalized through environment variables. Gateway acts as security choke point, and services still validate tokens for defense in depth.

## 15) How do you scale this system?
**Best answer:**  
Scale stateless services horizontally behind gateway. Redis and RabbitMQ handle shared coordination and async workloads. Booking-service can be scaled carefully with distributed lock strategy intact. Notification service can scale consumers independently for high email volume.

## 16) How do you ensure eventual consistency?
**Best answer:**  
Checkout uses asynchronous events after payment verification. We accept brief propagation delay between payment success and final seat state update. Frontend can poll or refresh seat status. Idempotent consumers and clear booking states reduce inconsistency impact.

## 17) What monitoring and health strategy do you use?
**Best answer:**  
Services expose health/actuator endpoints. We monitor gateway and critical dependencies (DB, RabbitMQ, Redis). Logs are structured around request path, payment events, and booking confirmations to trace cross-service flow quickly.

## 18) What is your error-handling strategy for frontend?
**Best answer:**  
We standardize status-code handling: `400` input issues, `401` auth failures, `409` seat conflicts, `5xx` server/transient failures. Frontend maps these to actionable UX: re-login, seat refresh prompt, and retry option.

## 19) How do you test this project end-to-end?
**Best answer:**  
Primary E2E flow via Postman through gateway: register/login, fetch seats, lock seats, create payment, verify payment, then re-check seat status. We also test negative paths like invalid token, lock conflict, and malformed payment payloads.

## 20) If you had more time, what improvements would you add?
**Best answer:**  
- Add OpenAPI specs per service and generated typed frontend clients.  
- Add distributed tracing (OpenTelemetry + Jaeger).  
- Add circuit breakers/retry policies for inter-service calls.  
- Add outbox pattern for guaranteed event publishing.  
- Harden secrets management with Vault/KMS and rotate credentials.

## 21) Explain a difficult trade-off in this architecture.
**Best answer:**  
Strong consistency vs performance during checkout is the key trade-off. We use immediate consistency for seat lock and payment verification boundaries, and eventual consistency for post-payment side effects. This preserves booking correctness while keeping APIs responsive.

## 22) Why did you include route compatibility like `/login` and `/auth/login`?
**Best answer:**  
Backward compatibility reduces frontend breakage during migration. Existing clients continue to work while the canonical route remains `/auth/login`. Gateway rewrite provides a safe transition path without changing downstream service contracts.

## 23) How does RabbitMQ routing work in your project?
**Best answer:**  
Payment-service publishes to a direct exchange (`payment_exchange`) with routing key (`payment_success`). Booking and notification queues bind to that key, so both consumers receive the success event and process their responsibilities independently.

## 24) How do you avoid coupling payment and booking too tightly?
**Best answer:**  
Payment does not call booking synchronously to confirm each booking. Instead, it emits domain events with booking IDs. Booking-service owns booking state transitions. This keeps boundaries clean and allows each service to evolve independently.

## 25) What are the most likely interview follow-ups and short responses?
**Best answers:**
- **Q: Is your system ACID across services?**  
  No global ACID transaction; we use local transactions plus event-driven eventual consistency.
- **Q: How do you prevent duplicate events?**  
  Design consumers to be idempotent and verify current booking state before update.
- **Q: Why not use Kafka?**  
  RabbitMQ fits current queueing/routing needs with lower operational complexity for project scale.
- **Q: What is a bottleneck risk?**  
  Seat lock hot-spots during high-demand shows; mitigated via Redis locks and optimized DB access.

## HR + Project Ownership Questions

## 26) What exactly was your contribution?
**Best answer:**  
I worked on integration-critical areas: gateway routing fixes, auth and protected route flow, booking/payment event contracts, Docker environment alignment, and frontend-facing architecture documentation. I also validated flows using Postman and compile checks.

## 27) What was the toughest bug and how did you debug it?
**Best answer:**  
A recurring `404` from gateway despite valid downstream endpoint. I compared service mappings with gateway route definitions, checked property namespace compatibility with the selected gateway dependency, corrected route keys, and validated by rebuilding and testing through gateway.

## 28) What did this project teach you?
**Best answer:**  
It strengthened my understanding of real-world distributed systems: consistency trade-offs, idempotency, async workflows, observability needs, and the importance of clean contracts between frontend and backend.

## 29) If deployed to production tomorrow, what final checklist would you run?
**Best answer:**  
- Security hardening (secret rotation, CORS/CSRF policy review, least-privileged access)  
- Load and concurrency tests on lock/payment paths  
- Alerting for queue lag and payment verification failures  
- Backup and recovery drills for DBs  
- Release runbook and rollback strategy

## 30) 30-second project pitch
**Best answer:**  
NPCine is a gateway-centric microservices cinema booking backend. It uses JWT auth, Redis seat locking, PostgreSQL transactional booking/payment domains, and RabbitMQ-driven async confirmation and notifications. The design prioritizes concurrency safety, service decoupling, and frontend-friendly API integration through one stable gateway endpoint.

## Quick Revision Sheet (One-liners)
- Gateway is the only frontend entry point.
- JWT secures booking and payment flows.
- Redis lock + DB state together prevent seat overbooking.
- Payment success is event-driven, not tightly coupled.
- Booking and notification consume the same success event.
- Docker networking uses service names, not localhost.
- Idempotency key protects retries from duplicate writes.
