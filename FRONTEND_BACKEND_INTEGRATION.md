# NPCine Frontend-Backend Integration

This document explains how the `npcine-frontend` app is integrated with your microservices through the API Gateway.

## Integration Architecture

- Frontend never calls individual services directly.
- All requests go to the API Gateway at `http://localhost:8081`.
- JWT token returned by auth APIs is stored in `localStorage` and automatically attached to protected requests.

## Core Integration Files

- Frontend API client:
  - `npcine-frontend/src/lib/api/client.ts`
  - Contains Axios `baseURL` and request interceptor for `Authorization: Bearer <token>`.
- Frontend API services:
  - `npcine-frontend/src/lib/api/services.ts`
  - Centralized service methods for auth, catalog, booking, payment.
- Frontend state stores:
  - `npcine-frontend/src/store/auth-store.ts` for JWT/session
  - `npcine-frontend/src/store/booking-store.ts` for selected movie/seats/booking IDs

## Endpoint Contracts Used by Frontend

- Auth:
  - `POST /auth/register` with `{ name, email, password }`
  - `POST /auth/login` with `{ email, password }`
  - Token key consumed from backend response: `token` (also tolerant to `jwt` / `accessToken`)
- Catalog:
  - `GET /catalog/movies`
  - Frontend expects movie objects with:
    - `id`, `title`, `genre`, `duration`, `rating`, `price`, `posterUrl`, `showtimeId`
- Seats and booking:
  - `GET /bookings/showtimes/{showtimeId}/seats`
  - `POST /bookings/lock-seats` with `{ showTimeId, seatNumbers }`
  - `GET /bookings/my-tickets`
- Payment:
  - `POST /payments/create` with `{ bookingIds, amount }`
  - `POST /payments/verify` with `{ razorpayOrderId, bookingIds }`

## Backend Changes Done For Compatibility

- Booking service:
  - Added `GET /catalog/movies` in `booking-service/.../controller/CatalogController.java`.
  - Added `GET /bookings/showtimes/{showTimeId}/seats` in `booking-service/.../controller/BookingController.java`.
  - Added `GET /bookings/my-tickets` in `booking-service/.../controller/BookingController.java`.
  - Added DTOs:
    - `CatalogMovieResponse`
    - `SeatViewResponse`
    - `MyTicketResponse`
  - Added seat pricing support in entity:
    - `booking-service/.../entity/Seat.java` now includes `price`.
  - Updated seat generation to set default price:
    - `booking-service/.../controller/AdminController.java`.

## No Hardcoded Frontend Data

- Dummy movie fallback has been removed from `npcine-frontend/src/lib/api/services.ts`.
- Home page now shows a backend-unavailable message if `/catalog/movies` fails instead of injecting local mock data.

## How to Verify End-to-End

- Start all services (gateway, user-service, booking-service, payment-service, notification-service if needed).
- Start frontend:
  - `cd npcine-frontend`
  - `npm run dev`
- Test flow:
  - Register (without phone number)
  - Login
  - Open home catalog (fetched from `/catalog/movies`)
  - Select seats and lock
  - Checkout create + verify payment
  - Confirm tickets appear in dashboard

## Important Operational Notes

- If catalog is empty, seed showtime and seats first (admin/DB setup) so `/catalog/movies` and seat map have data.
- Keep gateway route predicates active for:
  - `/auth/**`, `/catalog/**`, `/bookings/**`, `/payments/**`
- Ensure all services share compatible JWT secret/verification config for authenticated flows.
