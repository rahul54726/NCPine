package com.NPCine.payment_service.controller;

import com.NPCine.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/health")
    public ResponseEntity<?> health(){
        return new ResponseEntity<>(Map.of("Status" , "This is running fine",
                "time" , LocalDateTime.now().toString()) , HttpStatus.OK);
    }

    // Isko bhi update kar diya taaki list receive kar sake, though createOrder normally cart ya session se uthata hai
    // Par consistency ke liye abhi simple String list le lete hain
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        List<String> bookingIds = readBookingIds(request.get("bookingIds"));
        Object amountRaw = request.get("amount");
        if (bookingIds == null || bookingIds.isEmpty() || amountRaw == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "bookingIds and amount are required"));
        }
        Double amount = Double.valueOf(amountRaw.toString());
        String orderId = paymentService.createOrder(bookingIds, amount, principal.getName());
        return ResponseEntity.ok(Map.of("razorpayOrderId", orderId));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> request) {
        Object orderIdRaw = request.get("razorpayOrderId");
        if (orderIdRaw == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "razorpayOrderId is required"));
        }
        String orderId = orderIdRaw.toString();
        String paymentId = request.get("razorpayPaymentId") != null ? request.get("razorpayPaymentId").toString() : null;

        // Pura List of IDs nikal rahe hain
        List<String> bookingIds = readBookingIds(request.get("bookingIds"));
        if (bookingIds == null || bookingIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "bookingIds are required"));
        }

        // Update your payment service to accept this list
        boolean isVerified = paymentService.verifyPayment(orderId, paymentId, bookingIds);

        if (isVerified) {
            return ResponseEntity.ok(Map.of("status", "Payment Successful", "confirmedBookings", bookingIds));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Payment Failed"));
    }

    private List<String> readBookingIds(Object bookingIdsRaw) {
        if (!(bookingIdsRaw instanceof List<?> rawList)) {
            return null;
        }
        List<String> bookingIds = new ArrayList<>();
        for (Object item : rawList) {
            if (item == null) {
                continue;
            }
            bookingIds.add(item.toString());
        }
        return bookingIds;
    }
}