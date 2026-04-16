package com.rahul.cinebook.booking_service.consumer;
import com.rahul.cinebook.booking_service.filter.JwtAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rahul.cinebook.booking_service.service.BookingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service


public class PaymentSuccessConsumer {
    @Autowired
    private  BookingService bookingService;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    /**
     * Listens to the payment success queue.
     * Expects a payload containing a list of booking IDs and the payment ID.
     */
    @RabbitListener(queues = "payment_success_queue")
    public void handlePaymentSuccess(Map<String, Object> event) { // Changed to Object to accept the List
        log.info("Received Payment Success Message from RabbitMQ: {}", event);

        try {
            // Safely cast the incoming data to a List of Strings
            @SuppressWarnings("unchecked")
            List<String> bookingIds = (List<String>) event.get("bookingIds");

            // Extract standard string fields
            String paymentId = (String) event.get("paymentId");
            String status = (String) event.get("status");

            // Validate and process
            if ("SUCCESS".equals(status) && bookingIds != null && !bookingIds.isEmpty()) {
                bookingService.confirmBookingsBatch(bookingIds, paymentId);
                log.info("Batch confirmation initiated for {} bookings.", bookingIds.size());
            } else {
                log.warn("Ignored message. Status: {}, Booking IDs present: {}", status, (bookingIds != null));
            }

        } catch (Exception e) {
            log.error("Critical Error: Could not confirm bookings after successful payment. Payload: {}, Error: {}",
                    event, e.getMessage(), e);
        }
    }
}
