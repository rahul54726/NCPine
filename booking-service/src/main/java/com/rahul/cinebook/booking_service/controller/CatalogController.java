package com.rahul.cinebook.booking_service.controller;

import com.rahul.cinebook.booking_service.entity.Seat;
import com.rahul.cinebook.booking_service.repository.SeatRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/catalog")

public class CatalogController {
    @Autowired
    private  SeatRepo seatRepo;

    @GetMapping("/shows/{showId}/seats")
    public ResponseEntity<?> getShowSeats(@PathVariable String showId){
        List<Seat> seats = seatRepo.findByShowTimeId(showId);

        if (seats.isEmpty()) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(seats);
    }
}
