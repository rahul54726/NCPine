package com.rahul.cinebook.user_service.controller;

import com.rahul.cinebook.user_service.dto.AuthResponse;
import com.rahul.cinebook.user_service.dto.LoginRequest;
import com.rahul.cinebook.user_service.dto.RegisterRequest;
import com.rahul.cinebook.user_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // Fix: The service now returns AuthResponse directly
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // Fix: No need to wrap it twice
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
