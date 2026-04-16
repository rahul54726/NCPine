package com.rahul.cinebook.booking_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class LockSeatsRequest {
    private  String showTimeId;
    private List<String> seatNumbers;
}
