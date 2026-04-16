package com.rahul.cinebook.booking_service.config;

import com.rahul.cinebook.booking_service.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration

public class SecurityConfig {
    @Autowired
    private  JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. Actuator aur Health check ko hamesha pehle allow karo
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/bookings/health").permitAll()
                        .requestMatchers("/catalog/**").permitAll()

                        // 2. Baaki saare /bookings endpoints ke liye authentication zaroori hai
                        .requestMatchers("/admin/**", "/bookings/**").authenticated()
                        // 3. Baaki sab permitAll (safer side ke liye)
                        .anyRequest().permitAll()
                )
                // JWT filter ko UsernamePassword filter se pehle add karo
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new RuntimeException("Not used - JWT only");
        };
    }
}