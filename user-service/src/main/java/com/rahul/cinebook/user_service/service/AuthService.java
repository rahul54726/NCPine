package com.rahul.cinebook.user_service.service;

import com.rahul.cinebook.user_service.dto.AuthResponse;
import com.rahul.cinebook.user_service.dto.LoginRequest;
import com.rahul.cinebook.user_service.dto.RegisterRequest;
import com.rahul.cinebook.user_service.entity.User;
import com.rahul.cinebook.user_service.repository.UserRepo;
import com.rahul.cinebook.user_service.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;

    /**
     * Fix: Manual constructor with @Lazy is the ONLY way to stop the StackOverflowError.
     * It prevents the Manager from trying to initialize the SecurityConfig recursively.
     */
    public AuthService(UserRepo userRepo,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       EmailService emailService,
                       @Lazy AuthenticationManager authenticationManager) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);

        userRepo.save(user);

        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getName());
        } catch (Exception e) {
            log.error("Email failed: {}", e.getMessage());
        }

        return new AuthResponse(jwtUtil.generateToken(user.getEmail()));
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return new AuthResponse(jwtUtil.generateToken(request.getEmail()));
    }
}