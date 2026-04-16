package com.rahul.cinebook.booking_service.scheduler;

import com.rahul.cinebook.booking_service.entity.Booking;
import com.rahul.cinebook.booking_service.entity.Seat;
import com.rahul.cinebook.booking_service.enums.BookingStatus;
import com.rahul.cinebook.booking_service.enums.SeatStatus;
import com.rahul.cinebook.booking_service.repository.BookingRepo;
import com.rahul.cinebook.booking_service.repository.SeatRepo;
import com.rahul.cinebook.booking_service.service.SeatLockService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {
    private final BookingRepo bookingRepo;
    private final SeatRepo seatRepo;
    private final SeatLockService seatLockService;
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void processExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepo.findByStatusAndExpiresAtBefore(BookingStatus.LOCKED, now);

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Found {} expired booking locks to clean up", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                // 1. Revert Seat status to AVAILABLE
                Seat seat = seatRepo.findByShowTimeIdAndSeatNumber(
                        booking.getShowtimeId(),
                        booking.getSeatNumber()
                ).orElse(null);

                if (seat != null && seat.getStatus() == SeatStatus.LOCKED) {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seatRepo.save(seat);
                }

                // 2. Remove distributed lock from Redis
                seatLockService.unlockSeat(booking.getShowtimeId(), booking.getSeatNumber());

                // 3. Mark Booking as EXPIRED
                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepo.save(booking);

            } catch (Exception e) {
                log.error("Failed to expire booking {}: {}", booking.getId(), e.getMessage());
            }
        }
    }
}
