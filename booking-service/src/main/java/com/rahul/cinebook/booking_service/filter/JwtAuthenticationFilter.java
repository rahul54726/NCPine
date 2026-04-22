package com.rahul.cinebook.booking_service.filter;

import com.rahul.cinebook.booking_service.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String path = request.getRequestURI();

        // Debug Logs: Sabse pehle print honge
        System.out.println("== Processing Path: " + path + " ==");

        // 1. Check if header is valid
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("No Bearer token found for path: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 2. Validate Token (isTokenValid should handle expiry check)
            if (jwtUtil.isTokenValid(token)) {
                String email = jwtUtil.extractEmail(token);
                System.out.println("Valid Token! Authenticating User: " + email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                Collections.emptyList()
                        );
                // Set context taaki Principal.getName() mein email mil jaye
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                System.out.println("Token is INVALID or EXPIRED for path: " + path);
            }

        } catch (Exception ex) {
            System.out.println("JWT Filter Error: " + ex.getMessage());
            // Hum return nahi karenge, bas chain aage badhayenge
        }

        // 3. Hamesha chain aage badhao, SecurityConfig decide karega access
        filterChain.doFilter(request, response);
    }
}