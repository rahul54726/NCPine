package com.rahul.cinebook.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Dushman number 1: CSRF ko disable karo
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Gateway ko bolo ki routes public hain
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/api/users/**", "/actuator/**").permitAll()
                        // Abhi testing ke liye gateway se har cheez nikalne do
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}