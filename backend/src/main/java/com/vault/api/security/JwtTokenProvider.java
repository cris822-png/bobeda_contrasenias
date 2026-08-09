package com.vault.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Generates and validates JWT tokens.
 *
 * SECURITY INVARIANTS:
 *   - Tokens are signed with HMAC-SHA256 (HS256).
 *   - The signing key is injected from JwtConfig and never stored as a String.
 *   - This class never logs the token value or signing key.
 *   - Expired tokens are rejected during validation.
 */
@Service
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey signingKey;
    private final int expiryMinutes;

    public JwtTokenProvider(SecretKey jwtSigningKey, int jwtExpiryMinutes) {
        this.signingKey = jwtSigningKey;
        this.expiryMinutes = jwtExpiryMinutes;
    }

    /**
     * Generates a JWT for the given user ID.
     *
     * @param userId the user's UUID — stored as the JWT subject claim
     * @return a signed JWT string
     */
    public String generateToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (long) expiryMinutes * 60 * 1000);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts the user ID from a token.
     *
     * @param token the JWT string (without "Bearer " prefix)
     * @return the user's UUID
     * @throws JwtException if the token is invalid, expired, or tampered
     */
    public UUID getUserId(String token) {
        String subject = parseToken(token).getPayload().getSubject();
        return UUID.fromString(subject);
    }

    /**
     * Validates a JWT token.
     *
     * @param token the JWT string
     * @return true if the token is valid and not expired
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired");
        } catch (SignatureException e) {
            log.warn("JWT signature verification failed");
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getClass().getSimpleName());
        }
        return false;
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }
}
