## NPCine User Service – Summary

### Overview
The NPCine User Service is a Spring Boot–based microservice responsible for user registration, authentication, and basic account management. It issues JWT access tokens, persists users in MongoDB, and integrates with an SMTP provider to send welcome and password-related emails.

### Features Implemented
- **User registration**: `POST /auth/register` accepts a `RegisterRequest`, validates the payload, checks for duplicate email, hashes the password with BCrypt, persists a `User` document, sends a welcome email, and returns a JWT token in an `AuthResponse`.
- **User login**: `POST /auth/login` accepts a `LoginRequest`, verifies credentials against the stored BCrypt hash, and returns a JWT token in an `AuthResponse`.
- **JWT token generation**: `JwtUtil` generates HS256-signed JWT tokens using a shared secret and configurable expiration; tokens carry the user email as the subject.
- **MongoDB persistence**: `UserRepo` manages `User` documents in the `users` collection, keyed by ID, with helper methods `findByEmail` and `existsByEmail`.
- **Email notifications**: `EmailService` sends HTML emails via `JavaMailSender`, including a welcome email and helper methods intended for generic and password reset emails.
- **Health endpoint**: `GET /health` returns a simple JSON payload with current time and status string for basic liveness checks.
- **Security configuration**: `SecurityConfig` enables stateless security, disables form login and HTTP basic, and whitelists `/health/**`, `/actuator/health`, `/actuator/info`, and `/auth/**` while requiring authentication for other endpoints.

### Technology Stack
- **Language**: Java 21
- **Framework**: Spring Boot 4.x
- **Data Store**: MongoDB (Spring Data MongoDB)
- **Security**: Spring Security, JWT (jjwt)
- **Messaging**: SMTP via `spring-boot-starter-mail` (Gmail in development)
- **Observability**: Spring Boot Actuator (health, info)

### Current Limitations / Gaps (High Level)
- Secrets (JWT secret, mail password) are stored directly in `application.yml` instead of being externalized to environment-specific secret management.
- JWT validation filter is not yet wired into the security filter chain for protecting downstream endpoints.
- Error handling uses generic `RuntimeException` without a consistent error response format or global exception mapping.
- Domain model lacks auditing fields (e.g., created/updated time), roles/authorities, and explicit account states beyond a simple `enabled` flag.
