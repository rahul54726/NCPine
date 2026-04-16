package com.NPCine.notification_service.consumer;

import com.NPCine.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {
    private final EmailService emailService;
    @RabbitListener(queues = "notification_queue")
    public void receivePaymentSuccess(Map<String , Object> event){
        log.info("Notification Service received event : {}" , event);

        String status = (String) event.get("status");

        if ("SUCCESS".equals(status)){
            @SuppressWarnings("unchecked")
            List<String> bookingIds = (List<String>)event.get("bookingIds");
            String paymentId = (String) event.get("paymentId");
            String userEmail = (String) event.get("userEmail");
            if (userEmail == null || userEmail.isBlank()) {
                log.warn("Missing userEmail in payment event. Skipping email send for paymentId={}", paymentId);
                return;
            }
            emailService.sendTicketConfirmation(userEmail , paymentId , bookingIds);
        }
    }
}
