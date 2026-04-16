package com.rahul.cinebook.booking_service.controller;

import com.rahul.cinebook.booking_service.dto.LockSeatsRequest;
import com.rahul.cinebook.booking_service.service.BookingService;
import com.rahul.cinebook.booking_service.service.IdempotencyService;
import com.rahul.cinebook.booking_service.service.SeatLockOrchestrator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bookings")

public class BookingController {
    @Autowired
    private  SeatLockOrchestrator seatLockOrchestrator;
    @Autowired
    private  BookingService bookingService;
    @Autowired
    private  IdempotencyService idempotencyService;
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        HashMap<String, String> map = new HashMap<>();
        map.put("status", "BOOKING SERVICE RUNNING");
        map.put("time", LocalDateTime.now().toString());
        return ResponseEntity.ok(map);
    }
    @PostMapping("/lock-seats")
    public ResponseEntity<?> lockSeats(@RequestHeader(value = "Idempotency-Key" , required = false) String idempotencyKey,
                                       @RequestBody LockSeatsRequest request ,
                                       Principal principal){
        if (principal == null){
            return new ResponseEntity<>("Unauthorized: Missing JWT token" , HttpStatus.UNAUTHORIZED);
        }
        String email = principal.getName();
        if (idempotencyKey != null){
            var cached = idempotencyService.handleIdempotency(idempotencyKey , email , request);
            if (cached.isPresent()) return cached.get();
        }

        // Receive List of Booking IDs
        List<String> generatedBookingIds = seatLockOrchestrator.lockMultipleSeats(
                request.getShowTimeId() ,
                request.getSeatNumbers() ,
                email
        );

        // If null or empty, it means locking failed
        if (generatedBookingIds == null || generatedBookingIds.isEmpty()){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "One or more seats are already Locked"));
        }

        // Return success with IDs in JSON
        return ResponseEntity.ok(Map.of(
                "message", "Seats Locked Successfully",
                "bookingIds", generatedBookingIds // Sending IDs back to Frontend!
        ));
    }
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmBooking(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey ,
            @Valid @RequestBody LockSeatsRequest request,
                                            Principal principal){
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized: Missing JWT token");
        }
        String email = principal.getName();
        if (idempotencyKey != null) {
            var cachedResponse = idempotencyService.handleIdempotency(idempotencyKey , email , request);
            if (cachedResponse.isPresent()) return cachedResponse.get();
        }
        try {
            var bookings = bookingService.confirmBooking(
                    request.getShowTimeId() ,
                    request.getSeatNumbers(),
                    email
            );
            return ResponseEntity.ok(bookings);
        }catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());
        }
    }
    @PostMapping("/release-seats")
    public ResponseEntity<?> releaseSeats(@Valid @RequestBody LockSeatsRequest request,
                                          Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized: Missing JWT token");
        }
        String userEmail = principal.getName();
        // Use bookingService instead of seatLockOrchestrator to avoid the cycle
        bookingService.releaseSeats(request.getShowTimeId(), request.getSeatNumbers(), userEmail);
        return new ResponseEntity<>("Seats released", HttpStatus.OK);
    }
}
