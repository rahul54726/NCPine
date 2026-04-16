package com.rahul.cinebook.user_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Production Fix: Use @Async to prevent registration/login from hanging
     * while waiting for the mail server response.
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        String htmlBody = "<h3>Hello " + userName + ",</h3>" +
                "<p>Welcome to <b>NPCine</b> – your secure theater booking application!</p>" +
                "<p>We're glad to have you onboard. Start watching amazing movies in your nearest cinema.</p>" +
                "<br><p>Cheers,<br>NPCine Team</p>";

        sendHtmlEmail(toEmail, "Welcome to NPCine", htmlBody);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String htmlBody = "<p>Click the link below to reset your password:</p>" +
                "<a href=\"" + resetLink + "\" style=\"background-color: #008CBA; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;\">Reset Password</a>" +
                "<p>If the button doesn't work, copy-paste this link: " + resetLink + "</p>";

        sendHtmlEmail(toEmail, "Reset Your Password - NPCine", htmlBody);
    }

    /**
     * Private helper to centralize error handling and logging
     */
    private void sendHtmlEmail(String to, String subject, String body) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            javaMailSender.send(message);
            log.info("Email sent successfully to: {}", to);

        } catch (MessagingException e) {
            // Production Fix: Log the error but don't crash the calling service
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}