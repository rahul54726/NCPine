package com.NPCine.payment_service.service;

import com.NPCine.payment_service.config.RabbitMQConfig;
import com.NPCine.payment_service.entity.PaymentRecord;
import com.NPCine.payment_service.enums.PaymentStatus;
import com.NPCine.payment_service.repository.PaymentRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepo paymentRepo;
    private final RabbitTemplate rabbitTemplate;

//    @Value("${razorpay.key.secret}")
//    private String keySecret;

    public String createOrder(List<String> bookingIds, Double amount, String userEmail) {
        String mockOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        // Simple approach: Convert List to comma-separated string for DB storage
        String joinedBookingIds = String.join(",", bookingIds);

        PaymentRecord record = new PaymentRecord();
        record.setBookingId(joinedBookingIds); // Storing multiple IDs as a single string
        record.setUserEmail(userEmail);
        record.setAmount(amount);
        record.setRazorpayOrderId(mockOrderId);
        record.setStatus(PaymentStatus.CREATED);

        paymentRepo.save(record);
        log.info("Mock order {} created for bookings {}", mockOrderId, joinedBookingIds);
        return mockOrderId;
    }

    public boolean verifyPayment(String orderId, String paymentId, List<String> bookingIds) {
        PaymentRecord record = paymentRepo.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("order not found"));

        record.setStatus(PaymentStatus.SUCCESS);
        record.setRazorpayPaymentId(paymentId != null ? paymentId : "pay_" + UUID.randomUUID().toString().substring(0, 10));
        paymentRepo.save(record);

        // RabbitMQ message must pass the exact List object
        Map<String, Object> event = Map.of(
                "bookingIds", bookingIds,
                "paymentId", record.getRazorpayPaymentId(),
                "userEmail", record.getUserEmail(),
                "status", "SUCCESS"
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY, event);
        log.info("Payment success event sent to RabbitMQ for Bookings {}", bookingIds);

        return true;
    }
}