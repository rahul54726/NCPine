package com.rahul.cinebook.booking_service.service;

import com.rahul.cinebook.booking_service.entity.Seat;
import com.rahul.cinebook.booking_service.enums.SeatStatus;
import com.rahul.cinebook.booking_service.repository.SeatRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockOrchestrator {

    private final SeatLockService seatLockService;
    private final SeatRepo seatRepository;
    private final BookingService bookingService;

    @Transactional
    public List<String> lockMultipleSeats(String showTimeId, List<String> seatNumbers, String userEmail) {
        // Batch lock for performance
        List<Seat> seats = seatRepository.findAndLockAllSeats(showTimeId, seatNumbers);

        if (seats.size() != seatNumbers.size()) return null; // Use null for failure

        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) return null;
        }

        for (String seatNumber : seatNumbers) {
            boolean redisLocked = seatLockService.lockSeat(showTimeId, seatNumber, userEmail);
            if (!redisLocked) {
                rollbackRedisOnly(showTimeId, seatNumbers);
                return null;
            }
        }

        seats.forEach(seat -> seat.setStatus(SeatStatus.LOCKED));
        seatRepository.saveAll(seats);

        // Capture the bookings and return their IDs
        var lockedBookings = bookingService.createLockedBookings(showTimeId, seatNumbers, userEmail);

        return lockedBookings.stream()
                .map(booking -> booking.getId().toString()) // Assuming ID can be converted to String
                .toList();
    }

    private void rollbackRedisOnly(String showTimeId, List<String> seatNumbers) {
        seatNumbers.forEach(num -> seatLockService.unlockSeat(showTimeId, num));
    }
}