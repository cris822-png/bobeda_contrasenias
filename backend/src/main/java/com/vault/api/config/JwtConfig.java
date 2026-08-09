package com.vault.api.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Provides the JWT signing key as a Spring bean.
 *
 * The raw JWT_SECRET string from the environment is converted to an HMAC-SHA256
 * SecretKey. The string itself is not stored as a field after key creation.
 *
 * IMPORTANT: This bean must never be serialized, exposed via an actuator endpoint,
 * or included in any heap dump.
 */
@Configuration
public class JwtConfig {

    @Value("${vault.jwt.secret}")
    private String rawSecret;

    @Value("${vault.jwt.expiry-minutes}")
    private int expiryMinutes;

    /** Converts the raw secret string into a proper signing key. */
    @Bean
    public SecretKey jwtSigningKey() {
        byte[] keyBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        // Clear the raw bytes from memory
        java.util.Arrays.fill(keyBytes, (byte) 0);
        return key;
    }

    @Bean
    public int jwtExpiryMinutes() {
        return expiryMinutes;
    }
}
