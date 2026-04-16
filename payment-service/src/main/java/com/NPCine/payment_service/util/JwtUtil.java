package com.NPCine.payment_service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // Helper method to properly generate the HMAC Key object
    private Key getSignInKey() {
        // Assume secret is a plain string. If it's Base64, you'd use Decoders.BASE64.decode(secret)
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims validateAndGetClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey()) // Using the robust Keys generator!
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return validateAndGetClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            validateAndGetClaims(token);
            return true;
        } catch (Exception e) {
            // Debugging ke liye actual exception console mein dikhana zaroori hai
            System.err.println("JWT Validation failed: " + e.getMessage());
            return false;
        }
    }
}