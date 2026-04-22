package com.rahul.cinebook.booking_service.dto;

import com.rahul.cinebook.booking_service.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SeatViewResponse {
    private String id;
    private String label;
    private Double price;
    private SeatStatus status;
}
