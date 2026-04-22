package com.rahul.cinebook.booking_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "showtimes")
@Getter
@Setter
@Data

public class Showtime {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String movieId;
    private String screenId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
