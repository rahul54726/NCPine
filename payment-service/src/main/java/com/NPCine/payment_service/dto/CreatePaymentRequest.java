package com.NPCine.payment_service.dto;

import lombok.Data;

@Data
public class CreatePaymentRequest {
    private String bookingId;
    private Double amount;
}
