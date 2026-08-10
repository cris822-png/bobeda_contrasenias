package com.vault.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Value("${vault.ratelimit.auth.max-requests:5}")
    private int authMaxRequests;

    @Value("${vault.ratelimit.auth.window-seconds:180}")
    private int authWindowSeconds;

    @Value("${vault.ratelimit.api.max-requests:100}")
    private int apiMaxRequests;

    @Value("${vault.ratelimit.api.window-seconds:60}")
    private int apiWindowSeconds;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = getClientIp(request);
        boolean isAuthEndpoint = path.startsWith("/auth/");

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

        if (!rateLimitService.isAllowed(cacheKey, maxRequests, windowSeconds)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(windowSeconds));

            // Return a JSON error matching GlobalExceptionHandler.ErrorBody format
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("status", 429);
            errorBody.put("error", "Rate limit exceeded. Please try again later.");
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
