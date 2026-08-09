package com.vault.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Servlet filter that extracts the JWT from the Authorization header,
 * validates it, and sets the Spring Security authentication context.
 *
 * Expected header: Authorization: Bearer <token>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtTokenProvider tokenProvider;

    public JwtAuthFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (tokenProvider.isValid(token)) {
                try {
                    UUID userId = tokenProvider.getUserId(token);
                    // Store userId as the principal — no password needed in Spring Security context
                    var auth = new UsernamePasswordAuthenticationToken(
                            userId,       // principal
                            null,         // credentials
                            List.of()     // granted authorities (empty — authorization is ownership-based)
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception e) {
                    log.warn("Could not set authentication from valid token: {}", e.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
