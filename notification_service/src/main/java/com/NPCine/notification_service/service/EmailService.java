package com.NPCine.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
//    @Value("${system.email}")
//    String email;
    private final JavaMailSender mailSender;

    public void sendTicketConfirmation(String toEmail , String paymentId
                                       , List<String> bookingIds){
        try {
            SimpleMailMessage message  = new SimpleMailMessage();
            message.setFrom("vermarahul11034@gmail.com");
            message.setTo(toEmail);
            message.setSubject("🎬 Your NPCine Tickets are Confirmed!");

            String body = "Hello Movie Buff!\n\n" +
                    "Your payment was successful. Here are your details:\n\n" +
                    "Payment ID: " + paymentId + "\n" +
                    "Booking References: " + String.join(", ", bookingIds) + "\n\n" +
                    "Grab your popcorn and enjoy the show!\n\n" +
                    "- The NPCine Team";

            message.setText(body);
            mailSender.send(message);

            log.info("Ticket email sent successfully to {}" , toEmail);
        }
        catch (Exception e) {
            log.error("Failed to sent email to {} : {}" , toEmail , e.getMessage());
        }
    }
}
