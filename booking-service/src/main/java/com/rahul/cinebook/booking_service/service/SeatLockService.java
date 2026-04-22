package com.rahul.cinebook.booking_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "seat:lock:";
    private static final long LOCK_TTL_MINUTES = 5; // industry standard (payment window)

    private String getKey(String showtimeId, String seatNumber) {
        return LOCK_PREFIX + showtimeId + ":" + seatNumber;
    }

    /**
     * Try to lock seat (atomic)
     */
    public boolean lockSeat(String showtimeId, String seatNumber, String userEmail) {
        String key = getKey(showtimeId, seatNumber);

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, userEmail, Duration.ofMinutes(LOCK_TTL_MINUTES));

        return Boolean.TRUE.equals(success);
    }

    /**
     * Check if seat is locked
     */
    public boolean isSeatLocked(String showtimeId, String seatNumber) {
        String key = getKey(showtimeId, seatNumber);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Unlock seat (after payment success/failure)
     */
    public void unlockSeat(String showtimeId, String seatNumber) {
        String key = getKey(showtimeId, seatNumber);
        redisTemplate.delete(key);
    }

    /**
     * Validate ownership (important for security)
     */
    public boolean isSeatLockedByUser(String showTimeId, String seatNumber, String userEmail) {
        String key = LOCK_PREFIX + showTimeId + ":" + seatNumber;
        String lockedBy = redisTemplate.opsForValue().get(key);
        return userEmail.equals(lockedBy);
    }
}