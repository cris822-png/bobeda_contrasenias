package com.vault.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Service for checking rate limits using Redis.
 * Uses a Fixed Window algorithm via an atomic Lua script.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> redisScript;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        // Atomic Lua script: Increment counter, set TTL on first request, return current count
        String script =
            "local current = redis.call('incr', KEYS[1]) " +
            "if tonumber(current) == 1 then " +
            "   redis.call('expire', KEYS[1], ARGV[1]) " +
            "end " +
            "return current;";

        this.redisScript = new DefaultRedisScript<>(script, Long.class);
    }

    /**
     * Checks if the given key has exceeded the maximum allowed requests in the time window.
     *
     * @param key           The Redis key (e.g. "rate:auth:192.168.1.1")
     * @param maxRequests   The maximum number of requests allowed
     * @param windowSeconds The time window in seconds
     * @return true if the request is allowed, false if rate limited (HTTP 429)
     */
    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        try {
            Long count = redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                String.valueOf(windowSeconds)
            );
            return count != null && count <= maxRequests;
        } catch (Exception e) {
            // Fail-open: if Redis goes down, allow the request so the API doesn't completely break,
            // but log an error so we know rate limiting is disabled.
            log.error("Failed to check rate limit for key {}: {}", key, e.getMessage());
            return true;
        }
    }
}
