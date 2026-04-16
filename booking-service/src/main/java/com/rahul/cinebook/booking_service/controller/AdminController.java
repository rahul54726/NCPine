package com.rahul.cinebook.booking_service.controller;

import com.rahul.cinebook.booking_service.dto.SeatGenerateRequest;
import com.rahul.cinebook.booking_service.entity.Seat;
import com.rahul.cinebook.booking_service.enums.SeatStatus;
import com.rahul.cinebook.booking_service.repository.SeatRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    private final SeatRepo seatRepo;

    @PostMapping("/shows/{showId}/seats")
    public ResponseEntity<?> generateSeatsForShow(
            @PathVariable String showId,
            @RequestBody SeatGenerateRequest request
            ){
        List<Seat> newSeats = new ArrayList<>();
        for (String row : request.getRows()){
            for (int i = 1;i <= request.getSeatsPerRow();i++){
                String seatNumber = row + i;

                Seat seat = new Seat();
//                seat.setId("SEAT-" + UUID.randomUUID().toString().substring(0, 8));
                seat.setShowTimeId(showId);
                seat.setSeatNumber(seatNumber);
                seat.setStatus(SeatStatus.AVAILABLE);

                newSeats.add(seat);
            }
        }
        seatRepo.saveAll(newSeats);

        log.info("Successfully generated {} seats for showtime: {}", newSeats.size(), showId);
        return ResponseEntity.ok(Map.of(
                "message", "Seats generated successfully",
                "totalSeatsCreated", newSeats.size()
        ));
    }
}
