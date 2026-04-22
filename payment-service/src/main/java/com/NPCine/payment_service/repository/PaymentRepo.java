package com.NPCine.payment_service.repository;

import com.NPCine.payment_service.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.util.Optional;

public interface PaymentRepo extends JpaRepository<PaymentRecord , Long> {
    Optional<PaymentRecord> findByRazorpayOrderId(String razorpayOrderId);
}
