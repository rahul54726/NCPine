package com.rahul.cinebook.booking_service.repository;

import com.rahul.cinebook.booking_service.entity.Booking;
import com.rahul.cinebook.booking_service.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepo extends JpaRepository<Booking, String> {

    // Used by BookingService to check for existing confirmed seats
    boolean existsByShowtimeIdAndSeatNumberInAndStatus(
            String showtimeId,
            List<String> seatNumbers,
            BookingStatus status
    );

    // Used by BookingService to find the specific rows to transition from LOCKED to CONFIRMED
    List<Booking> findByUserEmailAndShowtimeIdAndSeatNumberInAndStatus(
            String userEmail,
            String showtimeId,
            List<String> seatNumbers,
            BookingStatus status
    );

    // Used by the Scheduler to find abandoned locks
    List<Booking> findByStatusAndExpiresAtBefore(
            BookingStatus status,
            LocalDateTime time
    );
}