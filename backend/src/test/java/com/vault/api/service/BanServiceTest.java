package com.vault.api.service;

import com.vault.api.model.BannedIp;
import com.vault.api.repository.BannedIpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BanServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private BannedIpRepository bannedIpRepository;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private BanService banService;

    @BeforeEach
    void setUp() {
        banService = new BanService(redisTemplate, bannedIpRepository);
        // Lenient mock in case it's not called (e.g. exceptions are thrown in Redis set)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testEscalationMath() {
        String ip = "127.0.0.1";

        // 1st offense (ban_count = 0 -> 1)
        when(bannedIpRepository.findByIpAddressForUpdate(ip)).thenReturn(Optional.empty());
        long remaining1 = banService.registerOrEscalateBan(ip);
        assertApproximateMinutes(3, remaining1);

        // 2nd offense (ban_count = 1 -> 2)
        BannedIp firstOffense = new BannedIp(ip, 1, OffsetDateTime.now().plusMinutes(3), OffsetDateTime.now());
        when(bannedIpRepository.findByIpAddressForUpdate(ip)).thenReturn(Optional.of(firstOffense));
        long remaining2 = banService.registerOrEscalateBan(ip);
        assertApproximateMinutes(9, remaining2);

        // 3rd offense (ban_count = 2 -> 3)
        BannedIp secondOffense = new BannedIp(ip, 2, OffsetDateTime.now().plusMinutes(9), OffsetDateTime.now());
        when(bannedIpRepository.findByIpAddressForUpdate(ip)).thenReturn(Optional.of(secondOffense));
        long remaining3 = banService.registerOrEscalateBan(ip);
        assertApproximateMinutes(81, remaining3);

        // 4th offense (ban_count = 3 -> 4)
        BannedIp thirdOffense = new BannedIp(ip, 3, OffsetDateTime.now().plusMinutes(81), OffsetDateTime.now());
        when(bannedIpRepository.findByIpAddressForUpdate(ip)).thenReturn(Optional.of(thirdOffense));
        long remaining4 = banService.registerOrEscalateBan(ip);
        assertApproximateMinutes(6561, remaining4);
    }

    private void assertApproximateMinutes(long expectedMinutes, long actualSeconds) {
        long actualMinutes = actualSeconds / 60;
        // Allow 1 minute variation due to execution time during testing
        assertEquals(expectedMinutes, actualMinutes, 1, "Expected roughly " + expectedMinutes + " minutes, but got " + actualMinutes);
    }
}
