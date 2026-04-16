## NPCine User Service – Architecture

### 1. Role in NPCine System
The User Service is responsible for:
- **User lifecycle**: registration, authentication, and basic account state (`enabled`).
- **Identity tokens**: issuing JWT access tokens that other NPCine services (gateway, booking, etc.) can trust.
- **Notifications**: sending welcome and password-related emails via SMTP.

It is designed to be a **stateless**, **HTTP/JSON** microservice with MongoDB as its persistence layer.

### 2. High-Level Component Diagram
- **API Layer**
  - `AuthController`
  - `healthcheck`
- **Business Layer**
  - `AuthService`
  - `EmailService`
- **Persistence Layer**
  - `UserRepo` (Spring Data MongoDB)
  - `User` document
- **Infrastructure / Cross-Cutting**
  - `SecurityConfig` (Spring Security)
  - `JwtUtil` (JWT creation & parsing)
  - Spring Boot Actuator (health/info)

### 3. Request Flows

#### 3.1 Registration (`POST /auth/register`)
1. `AuthController.register` receives a validated `RegisterRequest`.
2. `AuthService.register`:
   - Verifies email uniqueness via `UserRepo.existsByEmail`.
   - Hashes the password with `BCryptPasswordEncoder`.
   - Creates and saves a `User` document in MongoDB with `enabled = true`.
   - Triggers `EmailService.sendWelcomeEmail` to send an HTML welcome email.
   - Uses `JwtUtil.generateToken` to generate a JWT for the user’s email.
3. The controller returns an `AuthResponse` containing the JWT.

#### 3.2 Login (`POST /auth/login`)
1. `AuthController.login` receives a `LoginRequest`.
2. `AuthService.login`:
   - Loads the `User` by email via `UserRepo.findByEmail`.
   - Verifies the plaintext password against the stored BCrypt hash.
   - Generates a new JWT using `JwtUtil.generateToken`.
3. The controller returns an `AuthResponse` containing the JWT.

#### 3.3 Health Check (`GET /health`)
1. `healthcheck.getHealth` returns a simple JSON map with current timestamp and a static “running fine” status.
2. Spring Boot Actuator also exposes `/actuator/health` and `/actuator/info` for platform-level checks.

### 4. Security Design
- **Stateless**: `SecurityConfig` configures `SessionCreationPolicy.STATELESS` so the server does not store HTTP sessions.
- **Entry Points**:
  - Public: `/health/**`, `/actuator/health`, `/actuator/info`, `/auth/**`.
  - Protected: all other endpoints require authentication (intended to be via JWT).
- **JWT Handling**:
  - `JwtUtil` issues HS256-signed tokens using a shared secret and expiration from configuration.
  - Token subject is the user’s email.
  - (Planned enhancement) A JWT authentication filter should be added to validate tokens on incoming requests and build the `SecurityContext`.

### 5. Data Model
`User` document:
- `id`: MongoDB identifier.
- `name`: user’s display name.
- `email`: unique login identifier.
- `password`: BCrypt-hashed password.
- `enabled`: flag indicating whether the account is active.

Potential future fields:
- `roles` / `authorities` (for RBAC).
- `createdAt`, `updatedAt`, `lastLoginAt`.
- Account status enum (e.g., `PENDING_VERIFICATION`, `ACTIVE`, `SUSPENDED`).

### 6. External Integrations
- **MongoDB**:
  - Configured via `spring.data.mongodb.uri`.
  - Used by `UserRepo` for CRUD and query methods.
- **SMTP (Gmail)**:
  - Configured via `spring.mail.*` properties.
  - Used by `EmailService` to send HTML emails.
- **JWT**:
  - Issued by `JwtUtil` with `jjwt` library.
  - Secret and expiration configured via `jwt.secret` and `jwt.expiration`.

### 7. Production-Grade Enhancements (Planned)
- Externalize secrets (JWT secret, mail password) to environment variables or a secrets manager.
- Introduce a JWT authentication filter and integrate it into `SecurityFilterChain`.
- Add structured logging, correlation IDs, and central error handling via `@ControllerAdvice`.
- Enhance the user model with roles, auditing fields, and explicit account states.
- Implement email-based verification and password reset flows backed by persistent tokens.
