package com.rahul.cinebook.booking_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MyTicketResponse {
    private String bookingId;
    private String movieTitle;
    private String showTime;
    private List<String> seats;
    private Double totalAmount;
    private String status;
}
