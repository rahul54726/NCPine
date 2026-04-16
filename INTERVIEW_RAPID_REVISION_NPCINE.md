# NPCine Viva Rapid Revision (2-Page Style)

## 30-Second Project Intro
NPCine is a microservices cinema booking backend with an API Gateway as single entry point.  
It uses JWT auth, Redis-based seat locking to prevent overbooking, PostgreSQL for transactional booking/payment data, MongoDB for user/auth data, and RabbitMQ for asynchronous payment-success fan-out to booking and notification services.

## 90-Second Architecture Explanation
- Frontend calls only gateway (`:8081`)
- Gateway routes to:
  - user-service (`:8080`) for auth
  - booking-service (`:8082`) for seat/booking lifecycle
  - payment-service (`:8083`) for create/verify payment
  - notification-service (`:8084`) for email notifications
- Datastores:
  - MongoDB: user/auth domain
  - PostgreSQL: booking + payment domain
  - Redis: distributed seat lock coordination
- Messaging:
  - payment-service publishes payment success event to RabbitMQ
  - booking-service confirms bookings from event
  - notification-service sends email from event

## Core Flow (Interview Must-Know)
1. Login -> JWT issued  
2. Catalog seats fetched  
3. Seats locked (`LOCKED`, expiry window, Redis lock)  
4. Payment order created  
5. Payment verified  
6. Event published  
7. Booking confirmed (`BOOKED`) + email sent

## One-Liners for Key Concepts
- **Why gateway?** Single URL, centralized routing/security, hides internal topology.
- **Why Redis lock + DB status?** DB ensures durable state; Redis prevents race conditions under concurrency.
- **Why async after payment?** Better latency and fault isolation; email failures don’t break checkout.
- **Why idempotency key?** Safe retries, prevents duplicate lock/confirm writes.
- **Consistency model?** Strong local consistency per service + eventual consistency across services via events.

## Top Technical Questions (Short Answers)

## Q1: How do you prevent double booking?
Use seat status validation in DB + distributed Redis locks + lock expiry window + final state transition checks.

## Q2: How is JWT used?
User-service issues JWT; frontend sends bearer token; gateway and protected services validate token.

## Q3: What if notification service is down?
Checkout still succeeds; events remain queued; notification catches up when consumer is healthy.

## Q4: Why separate payment and booking services?
Clear domain boundaries; independent scaling/deployment; event-based decoupling reduces tight coupling.

## Q5: What issue did you fix recently?
Gateway `404` due to wrong route key namespace; corrected to `spring.cloud.gateway.server.webmvc.routes`.

## Q6: What are your important HTTP status mappings?
`400` bad input, `401` unauthorized, `404` route/resource not found, `409` seat conflict, `5xx` server error.

## Q7: How do you scale for high-demand show releases?
Scale stateless services horizontally, optimize lock path, tune Redis, partition hot data, and increase consumer concurrency.

## Q8: How do you test end-to-end?
Postman flow: register/login -> lock seats -> create payment -> verify -> validate seat `BOOKED`.

## Common Follow-Ups
- **Global ACID?** No; distributed system uses event-driven eventual consistency.
- **Duplicate events?** Consumer logic should be idempotent and state-aware.
- **Why RabbitMQ?** Good routing/queue semantics and lower operational overhead for current scale.

## Project Ownership Answers
- I handled critical integration fixes: gateway routing, Docker host alignment, booking/payment/notification event contract improvements, and frontend handoff docs.
- I debugged route mismatch and flow breakages with config/service mapping verification and end-to-end retesting.

## Production Lens (Quick Points)
- Add tracing, metrics, alerts
- Add DLQ/retry policy
- Add rate limiting and WAF
- Add secret management and key rotation
- Add CI/CD quality gates and performance tests

## Last-Minute Memory Hooks
- Gateway only, never service ports from frontend
- JWT for protected calls
- `AVAILABLE -> LOCKED -> BOOKED`
- Payment verify publishes event
- Booking + notification consume same success event
- Redis lock prevents race
- Idempotency protects retries
