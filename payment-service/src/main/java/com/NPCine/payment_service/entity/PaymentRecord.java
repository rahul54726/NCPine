package com.NPCine.payment_service.entity;

import com.NPCine.payment_service.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_records")
@Data
public class PaymentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String bookingId;
    @Column(nullable = false)
    private String userEmail;
    @Column(nullable = false)
    private Double amount;
    @Column(unique = true , nullable = false)
    private String razorpayOrderId;
    private String razorpayPaymentId;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
//    @PrePersist
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }


}
