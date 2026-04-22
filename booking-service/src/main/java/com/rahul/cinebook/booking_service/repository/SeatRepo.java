package com.rahul.cinebook.booking_service.repository;

import com.rahul.cinebook.booking_service.entity.Seat;
import com.rahul.cinebook.booking_service.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface SeatRepo extends JpaRepository<Seat, String> {

    /**
     * Single seat lock (existing)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.showTimeId = :showTimeId AND s.seatNumber = :seatNumber")
    Optional<Seat> findAndLockSeat(@Param("showTimeId") String showTimeId, @Param("seatNumber") String seatNumber);

    /**
     * Production Upgrade: Batch Pessimistic Lock
     * Use this in SeatLockOrchestrator to lock ALL requested seats in one DB round-trip.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.showTimeId = :showTimeId AND s.seatNumber IN :seatNumbers")
    List<Seat> findAndLockAllSeats(@Param("showTimeId") String showTimeId, @Param("seatNumbers") List<String> seatNumbers);

    /**
     * Standard fetch for confirmation and release flows
     */
    List<Seat> findByShowTimeIdAndSeatNumberIn(String showTimeId, List<String> seatNumbers);

    /**
     * Single seat fetch for the Scheduler
     */
    Optional<Seat> findByShowTimeIdAndSeatNumber(String showTimeId, String seatNumber);

    boolean existsByShowTimeIdAndSeatNumberAndStatus(
            String showTimeId,
            String seatNumber,
            SeatStatus status
    );
    /**
     * Fetch all seats associated with a specific showtime.
     * Used by the Catalog Service to render the theater seat map.
     */
    List<Seat> findByShowTimeId(String showTimeId);
}