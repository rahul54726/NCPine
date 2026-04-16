-- V1__Initial_Setup.sql
CREATE TABLE showtimes (
    id VARCHAR(255) PRIMARY KEY,
    movie_id VARCHAR(255) NOT NULL,
    screen_id VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL
);

CREATE TABLE seats (
    id VARCHAR(255) PRIMARY KEY,
    showtime_id VARCHAR(255) REFERENCES showtimes(id),
    seat_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'AVAILABLE'
);

CREATE TABLE bookings (
    id VARCHAR(255) PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    showtime_id VARCHAR(255) REFERENCES showtimes(id),
    seat_number VARCHAR(50),
    amount DOUBLE PRECISION DEFAULT 250.0,
    status VARCHAR(20),
    created_at TIMESTAMP,
    expires_at TIMESTAMP -- Required for the Expiry Scheduler
);

CREATE TABLE idempotency_records (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    user_email VARCHAR(255),
    response_hash TEXT,
    created_at TIMESTAMP
);

-- Performance Indexes for Docker/Production
CREATE INDEX idx_seats_lookup ON seats(showtime_id, seat_number);
CREATE INDEX idx_booking_expiry ON bookings(status, expires_at);