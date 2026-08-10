package com.vault.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vault.api.service.BanService;
import com.vault.api.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global rate limiting filter.
 * Runs after JwtAuthFilter so that authenticated requests have the SecurityContext populated.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitService rateLimitService;
    private final BanService banService;
    private final ObjectMapper objectMapper;

    @Value("${vault.ratelimit.auth.max-requests:5}")
    private int authMaxRequests;

    @Value("${vault.ratelimit.auth.window-seconds:180}")
    private int authWindowSeconds;

    @Value("${vault.ratelimit.api.max-requests:100}")
    private int apiMaxRequests;

    @Value("${vault.ratelimit.api.window-seconds:60}")
    private int apiWindowSeconds;

    public RateLimitFilter(RateLimitService rateLimitService, BanService banService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.banService = banService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = getClientIp(request);
        boolean isAuthEndpoint = path.startsWith("/auth/");

        // --- PRE-CHECK: Is the IP currently banned? ---
        BanService.BanStatus banStatus;
        try {
            banStatus = banService.isBanned(ip);
        } catch (Exception e) {
            if (isAuthEndpoint) {
                log.warn("Postgres/Redis failure on ban check for auth route, failing closed (503): {}", e.getMessage());
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                response.setContentType("application/json");

                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("status", 503);
                errorBody.put("error", "Service temporarily unavailable, please try again shortly.");
                errorBody.put("timestamp", LocalDateTime.now().toString());

                response.getWriter().write(objectMapper.writeValueAsString(errorBody));
                return;
            } else {
                log.warn("Postgres/Redis failure on ban check for general API route, failing open: {}", e.getMessage());
                banStatus = new BanService.BanStatus(false, 0);
            }
        }

        if (banStatus.isBanned()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(banStatus.remainingSeconds()));

            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("status", 429);
            errorBody.put("error", "Your account or IP is temporarily banned due to excessive failed attempts.");
            errorBody.put("timestamp", LocalDateTime.now().toString());

            response.getWriter().write(objectMapper.writeValueAsString(errorBody));
            return; // Stop the filter chain
        }

        String cacheKey;
        int maxRequests;
        int windowSeconds;

        if (isAuthEndpoint) {
            cacheKey = "rate:auth:" + ip;
            maxRequests = authMaxRequests;
            windowSeconds = authWindowSeconds;
        } else {
            // For general API endpoints, try to include the userId if authenticated
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                cacheKey = "rate:api:" + ip + ":" + auth.getPrincipal().toString();
            } else {
                cacheKey = "rate:api:" + ip;
            }
            maxRequests = apiMaxRequests;
            windowSeconds = apiWindowSeconds;
        }

        boolean allowed;
        try {
            allowed = rateLimitService.isAllowed(cacheKey, maxRequests, windowSeconds);
        } catch (Exception e) {
            if (isAuthEndpoint) {
                log.warn("Redis failure on auth route, failing closed (503): {}", e.getMessage());
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                response.setContentType("application/json");

                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("status", 503);
                errorBody.put("error", "Service temporarily unavailable, please try again shortly.");
                errorBody.put("timestamp", LocalDateTime.now().toString());

                response.getWriter().write(objectMapper.writeValueAsString(errorBody));
                return;
            } else {
                log.warn("Redis failure on general API route, failing open: {}", e.getMessage());
                allowed = true;
            }
        }

        if (!allowed) {
            long retryAfterSeconds = windowSeconds;

            if (isAuthEndpoint) {
                try {
                    retryAfterSeconds = banService.registerOrEscalateBan(ip);
                } catch (Exception e) {
                    log.error("Failed to register or escalate ban for IP {}: {}", ip, e.getMessage());
                }
            }

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

            // Return a JSON error matching GlobalExceptionHandler.ErrorBody format
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("status", 429);
            if (isAuthEndpoint) {
                errorBody.put("error", "Your account or IP is temporarily banned due to excessive failed attempts.");
            } else {
                errorBody.put("error", "Rate limit exceeded. Please try again later.");
            }
            errorBody.put("timestamp", LocalDateTime.now().toString());

            response.getWriter().write(objectMapper.writeValueAsString(errorBody));
            return; // Stop the filter chain
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
