package com.rahul.cinebook.booking_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
@Data
public class IdempotencyRecord {
    @Id
    private String idempotencyKey;
    private String userEmail;
    private String responseHash;
    private LocalDateTime createdAt;
}
