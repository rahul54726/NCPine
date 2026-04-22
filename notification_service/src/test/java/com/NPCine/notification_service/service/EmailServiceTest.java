package com.NPCine.notification_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // Ye Mockito ko activate karta hai
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender; // JavaMailSender ka ek nakli (mock) object banaya

    @InjectMocks
    private EmailService emailService; // Isme mock kiye hue JavaMailSender ko daal diya

    @Test
    void sendTicketConfirmation_ShouldSendEmail() {
        // 1. Arrange: Test ke liye dummy data taiyaar karo
        String toEmail = "rahul23256rajpoot@gmail.com";
        String paymentId = "pay_mock_12345";
        List<String> bookingIds = List.of("ticket-A1", "ticket-A2");

        // 2. Act: Asli method ko call karo
        emailService.sendTicketConfirmation(toEmail, paymentId, bookingIds);

        // 3. Assert: Check karo ki kya hua
        // ArgumentCaptor se hum pakdenge ki mailSender ke paas konsa message gaya
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Verify karo ki mailSender.send() sirf 1 baar call hua hai
        verify(mailSender, times(1)).send(messageCaptor.capture());

        // Jo message pakda hai, uske andar ka data check karo
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertEquals(toEmail, sentMessage.getTo()[0], "Email address match nahi hua!");
        assertEquals("🎬 Your NPCine Tickets are Confirmed!", sentMessage.getSubject(), "Subject match nahi hua!");
        assertTrue(sentMessage.getText().contains(paymentId), "Body mein Payment ID missing hai!");
        assertTrue(sentMessage.getText().contains("ticket-A1"), "Body mein Booking ID missing hai!");
    }
}