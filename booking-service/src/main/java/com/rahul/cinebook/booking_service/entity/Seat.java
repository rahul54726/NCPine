package com.rahul.cinebook.booking_service.entity;

import com.rahul.cinebook.booking_service.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "seats")
@Data
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String showTimeId;
    private String seatNumber;
    @Enumerated(EnumType.STRING)
    private SeatStatus status;
}
