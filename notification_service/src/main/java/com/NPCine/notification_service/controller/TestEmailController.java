package com.NPCine.notification_service.controller;

import com.NPCine.notification_service.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestEmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/test-email")
    public String sendTestEmail(@RequestParam String to) {


        emailService.sendTicketConfirmation(
                to,
                "TEST_DIRECT_PAY_999",
                List.of("TEST-SEAT-1", "TEST-SEAT-2")
        );

        return "Email request sent on: " + to + ". check you console";
    }
}