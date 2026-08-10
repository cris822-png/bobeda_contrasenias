package com.vault.api.service;

import com.vault.api.model.BannedIp;
import com.vault.api.repository.BannedIpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class BanService {

    private static final Logger log = LoggerFactory.getLogger(BanService.class);
    private static final String CACHE_PREFIX = "ban:ip:";
    private static final int FIRST_OFFENSE_MINUTES = 3;

    // Postgres supports years up to 294276, but year 9999 is standard and safe.
    private static final OffsetDateTime MAX_BANNED_UNTIL = OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);

    private final StringRedisTemplate redisTemplate;
    private final BannedIpRepository bannedIpRepository;

    public BanService(StringRedisTemplate redisTemplate, BannedIpRepository bannedIpRepository) {
        this.redisTemplate = redisTemplate;
        this.bannedIpRepository = bannedIpRepository;
    }

    public record BanStatus(boolean isBanned, long remainingSeconds) {}

    public BanStatus isBanned(String ip) {
        String key = CACHE_PREFIX + ip;
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            // In Spring Data Redis, getExpire returns -1 if key exists with no TTL, -2 if key does not exist.
            if (ttl != null && ttl > 0) {
                return new BanStatus(true, ttl);
            }
        } catch (Exception e) {
            log.warn("Failed to check Redis ban cache for IP {}: {}", ip, e.getMessage());
        }

        // Cache miss (or expired/Redis down) - fallback to PostgreSQL
        // We do NOT catch exceptions here. If Postgres is down, the exception propagates
        // to RateLimitFilter, which will fail closed (503) for auth routes.
        Optional<BannedIp> optionalBanned = bannedIpRepository.findById(ip);

        if (optionalBanned.isPresent()) {
            BannedIp bannedIp = optionalBanned.get();
            OffsetDateTime now = OffsetDateTime.now();
            if (bannedIp.getBannedUntil().isAfter(now)) {
                long remainingSeconds = Duration.between(now, bannedIp.getBannedUntil()).getSeconds();
                try {
                    // Repopulate cache
                    redisTemplate.opsForValue().set(key, "1", remainingSeconds, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("Failed to update Redis ban cache for IP {}: {}", ip, e.getMessage());
                }
                return new BanStatus(true, remainingSeconds);
            }
        }

        return new BanStatus(false, 0);
    }

    @Transactional
    public long registerOrEscalateBan(String ip) {
        OffsetDateTime now = OffsetDateTime.now();
        Optional<BannedIp> optionalBanned = bannedIpRepository.findByIpAddressForUpdate(ip);

        BannedIp bannedIp;
        long durationMinutes;

        if (optionalBanned.isEmpty()) {
            // First offense
            durationMinutes = FIRST_OFFENSE_MINUTES;
            bannedIp = new BannedIp(ip, 1, now.plusMinutes(durationMinutes), now);
        } else {
            // Repeat offense
            bannedIp = optionalBanned.get();
            int currentCount = bannedIp.getBanCount();
            int newBanCount = currentCount + 1;

            // Deterministic math: duration = 3 ^ (2 ^ (ban_count - 1))
            double exponent = Math.pow(2, newBanCount - 1);
            durationMinutes = (long) Math.pow(3, exponent);

            bannedIp.setBanCount(newBanCount);
            bannedIp.setLastBannedAt(now);

            // Safe addition with overflow cap
            OffsetDateTime newBannedUntil;
            try {
                newBannedUntil = now.plusMinutes(durationMinutes);
                if (newBannedUntil.isAfter(MAX_BANNED_UNTIL)) {
                    newBannedUntil = MAX_BANNED_UNTIL;
                }
            } catch (Exception e) {
                // E.g., ArithmeticException from overflow
                newBannedUntil = MAX_BANNED_UNTIL;
            }
            bannedIp.setBannedUntil(newBannedUntil);
        }

        bannedIpRepository.save(bannedIp);
        long remainingSeconds = Duration.between(now, bannedIp.getBannedUntil()).getSeconds();

        // Update Redis Cache
        try {
            redisTemplate.opsForValue().set(CACHE_PREFIX + ip, "1", remainingSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to set Redis ban cache for escalated IP {}: {}", ip, e.getMessage());
        }

        return remainingSeconds;
    }
}
