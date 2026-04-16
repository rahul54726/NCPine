package com.rahul.cinebook.booking_service.service;

import com.rahul.cinebook.booking_service.entity.Booking;
import com.rahul.cinebook.booking_service.entity.Seat;
import com.rahul.cinebook.booking_service.enums.BookingStatus;
import com.rahul.cinebook.booking_service.enums.SeatStatus;
import com.rahul.cinebook.booking_service.repository.BookingRepo;
import com.rahul.cinebook.booking_service.repository.SeatRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepo bookingRepo;
    private final SeatRepo seatRepo;
    private final SeatLockService seatLockService; // Direct dependency to handle Redis locks

    /**
     * Creates new booking records with a LOCKED status and an expiration time.
     */
    @Transactional
    public List<Booking> createLockedBookings(String showTimeId, List<String> seatNumbers, String userEmail) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new RuntimeException("Seat list cannot be empty");
        }

        // Ensure none of the selected seats are already confirmed
        boolean alreadyBooked = bookingRepo.existsByShowtimeIdAndSeatNumberInAndStatus(
                showTimeId, seatNumbers, BookingStatus.CONFIRMED);

        if (alreadyBooked) {
            throw new RuntimeException("One or more selected seats are already confirmed.");
        }

        // Generate a new booking record for each seat
        List<Booking> bookings = seatNumbers.stream().map(seat -> {
            Booking booking = new Booking();
            booking.setUserEmail(userEmail);
            booking.setShowtimeId(showTimeId);
            booking.setSeatNumber(seat);
            booking.setAmount(250.0); // Assume base price, can be dynamic later
            booking.setStatus(BookingStatus.LOCKED);
            booking.setCreatedAt(LocalDateTime.now());
            // Lock the seat for 5 minutes (payment window)
            booking.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            return booking;
        }).toList();

        return bookingRepo.saveAll(bookings);
    }

    /**
     * Manual confirmation method (Bypass flow). Confirms seats if they belong to the user.
     */
    @Transactional
    public List<Booking> confirmBooking(String showTimeId, List<String> seatNumbers, String userEmail) {
        // 1. Verify ownership and state integrity
        List<Booking> existingBookings = bookingRepo
                .findByUserEmailAndShowtimeIdAndSeatNumberInAndStatus(
                        userEmail, showTimeId, seatNumbers, BookingStatus.LOCKED);

        if (existingBookings.size() != seatNumbers.size()) {
            throw new RuntimeException("Unauthorized or Invalid: Some seats were not locked by you.");
        }

        // 2. Update physical seat statuses in the database
        List<Seat> seats = seatRepo.findByShowTimeIdAndSeatNumberIn(showTimeId, seatNumbers);
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.LOCKED) {
                throw new RuntimeException("Seat " + seat.getSeatNumber() + " is no longer locked.");
            }
            seat.setStatus(SeatStatus.BOOKED);
        }
        seatRepo.saveAll(seats);

        // 3. Mark the booking records as CONFIRMED
        existingBookings.forEach(booking -> {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setExpiresAt(null); // Remove expiration upon confirmation
        });

        // 4. Release Redis locks to prevent memory leaks
        for (String seatNumber : seatNumbers) {
            seatLockService.unlockSeat(showTimeId, seatNumber);
        }

        return bookingRepo.saveAll(existingBookings);
    }

    /**
     * Releases locked seats back to the AVAILABLE pool. Used for cancellations or timeouts.
     */
    @Transactional
    public void releaseSeats(String showTimeId, List<String> seatNumbers, String userEmail) {
        List<Seat> seats = seatRepo.findByShowTimeIdAndSeatNumberIn(showTimeId, seatNumbers);

        for (Seat seat : seats) {
            if (seat.getStatus() == SeatStatus.LOCKED) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seatLockService.unlockSeat(showTimeId, seat.getSeatNumber());
            }
        }
        seatRepo.saveAll(seats);

        // Mark associated booking records as EXPIRED
        List<Booking> lockedBookings = bookingRepo
                .findByUserEmailAndShowtimeIdAndSeatNumberInAndStatus(
                        userEmail, showTimeId, seatNumbers, BookingStatus.LOCKED);

        lockedBookings.forEach(b -> b.setStatus(BookingStatus.EXPIRED));
        bookingRepo.saveAll(lockedBookings);
    }

    /**
     * Automated confirmation method triggered by RabbitMQ events (Payment Success).
     * Processes a batch of booking IDs concurrently.
     */
    @Transactional
    public void confirmBookingsBatch(List<String> bookingIds, String paymentId) {
        for (String bookingId : bookingIds) {
            Booking booking = bookingRepo.findById(bookingId).orElse(null);

            if (booking != null && booking.getStatus() == BookingStatus.LOCKED) {
                // 1. Transition seat status from LOCKED to BOOKED
                Seat seat = seatRepo.findByShowTimeIdAndSeatNumber(booking.getShowtimeId(), booking.getSeatNumber())
                        .orElseThrow(() -> new RuntimeException("Seat record not found for booking ID: " + bookingId));

                seat.setStatus(SeatStatus.BOOKED);
                seatRepo.save(seat);

                // 2. Finalize the booking record
                booking.setStatus(BookingStatus.CONFIRMED);
                booking.setExpiresAt(null); // Clear the expiration timer

                // TODO: If you add a paymentId column to the Booking entity, set it here
                // booking.setPaymentId(paymentId);

                bookingRepo.save(booking);

                // 3. Purge the distributed lock from Redis
                seatLockService.unlockSeat(booking.getShowtimeId(), booking.getSeatNumber());

                log.info("Successfully confirmed booking for ID: {} and Seat: {}", bookingId, booking.getSeatNumber());
            } else {
                log.warn("Skipping confirmation for booking ID: {}. Record not found or not in LOCKED state.", bookingId);
            }
        }
    }
}