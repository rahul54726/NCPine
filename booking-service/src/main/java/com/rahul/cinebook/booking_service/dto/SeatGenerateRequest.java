package com.rahul.cinebook.booking_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class SeatGenerateRequest {
    private List<String> rows;
    private int seatsPerRow;
}
