package com.vault.api.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the in-memory DEK (Data Encryption Key) cache.
 *
 * After a successful unlock, the unwrapped DEK is cached here for the duration
 * of the session (JWT TTL). This lets vault operations encrypt/decrypt without
 * requiring the master password on every request.
 *
 * SECURITY INVARIANTS:
 *   - DEKs are stored as byte[] and NEVER serialized or logged.
 *   - Entries are evicted on explicit lock/logout AND by TTL sweeper.
 *   - The byte[] is zeroed on eviction so the JVM can reclaim the memory securely.
 *   - This class is NOT a substitute for a proper HSM in high-security deployments.
 *   - JVM heap dumps will contain these keys; protect heap dump access accordingly
 *     (restrict jmap access, use -XX:+DisableAttachMechanism in production).
 */
@Service
public class DekManager {

    private static final Logger log = LoggerFactory.getLogger(DekManager.class);

    private final KdfService kdfService;
    private final AesGcmService aesGcmService;

    /**
     * In-memory store: userId → CachedDek.
     * ConcurrentHashMap for thread-safety; entries are zeroed on removal.
     */
    private final ConcurrentHashMap<UUID, CachedDek> cache = new ConcurrentHashMap<>();

    public DekManager(KdfService kdfService, AesGcmService aesGcmService) {
        this.kdfService = kdfService;
        this.aesGcmService = aesGcmService;
    }

    // ─── Wrap / Unwrap ───────────────────────────────────────────────────────

    /**
     * Generates a new random DEK and wraps it with a key derived from the master password.
     *
     * @param masterPassword the master password as a char array. Will be zeroed here.
     * @param salt           the user's Argon2id salt
     * @return WrapResult containing the wrapped DEK and the GCM nonce used.
     */
    public WrapResult wrapNewDek(char[] masterPassword, byte[] salt) {
        byte[] kek = null;
        byte[] dek = null;
        try {
            kek = kdfService.deriveKey(masterPassword, salt);
            dek = generateDek();
            AesGcmService.EncryptionResult r = aesGcmService.encrypt(dek, kek);
            return new WrapResult(r.ciphertext(), r.iv());
        } finally {
            Arrays.fill(masterPassword, '\0');
            if (kek != null) Arrays.fill(kek, (byte) 0);
            if (dek != null) Arrays.fill(dek, (byte) 0);
        }
    }

    /**
     * Unwraps the DEK using the master password and caches it for the given user.
     * Call this after a successful unlock.
     *
     * @param userId         the user's UUID
     * @param masterPassword the master password as a char array. Will be zeroed here.
     * @param salt           the user's Argon2id salt
     * @param dekWrapped     the wrapped DEK from the database
     * @param dekIv          the GCM nonce used when wrapping
     * @param ttlSeconds     how long to cache the DEK (should match JWT expiry)
     * @throws RuntimeException if authentication tag verification fails (wrong password)
     */
    public void unlockAndCache(UUID userId, char[] masterPassword, byte[] salt,
                                byte[] dekWrapped, byte[] dekIv, long ttlSeconds) {
        byte[] kek = null;
        byte[] dek = null;
        try {
            kek = kdfService.deriveKey(masterPassword, salt);
            // AEADBadTagException is thrown here if the password is wrong
            dek = aesGcmService.decrypt(dekWrapped, dekIv, kek);
            // Cache a copy; zero the local variable
            byte[] dekCopy = Arrays.copyOf(dek, dek.length);
            Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
            CachedDek existing = cache.put(userId, new CachedDek(dekCopy, expiresAt));
            if (existing != null) existing.zero(); // zero any old entry
        } finally {
            Arrays.fill(masterPassword, '\0');
            if (kek != null) Arrays.fill(kek, (byte) 0);
            if (dek != null) Arrays.fill(dek, (byte) 0);
        }
    }

    /**
     * Returns a COPY of the cached DEK for the given user.
     * The caller MUST zero the returned byte[] after use.
     *
     * @throws IllegalStateException if the DEK is not cached (session expired or not unlocked)
     */
    public byte[] getDek(UUID userId) {
        CachedDek entry = cache.get(userId);
        if (entry == null || entry.isExpired()) {
            if (entry != null) evict(userId); // evict expired entry
            throw new IllegalStateException("Session expired. Please unlock again.");
        }
        return Arrays.copyOf(entry.dekBytes(), entry.dekBytes().length);
    }

    /**
     * Explicitly evicts and zeroes the cached DEK for a user.
     * Call this on logout or explicit lock.
     */
    public void lock(UUID userId) {
        evict(userId);
        log.debug("DEK cache cleared for user {}", userId);
    }

    /** Periodic sweeper: removes expired entries every minute. */
    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        Iterator<Map.Entry<UUID, CachedDek>> it = cache.entrySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            Map.Entry<UUID, CachedDek> entry = it.next();
            if (entry.getValue().isExpired()) {
                entry.getValue().zero();
                it.remove();
                count++;
            }
        }
        if (count > 0) log.debug("Evicted {} expired DEK cache entries", count);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void evict(UUID userId) {
        CachedDek removed = cache.remove(userId);
        if (removed != null) removed.zero();
    }

    private byte[] generateDek() {
        byte[] dek = new byte[32]; // 256-bit DEK
        new java.security.SecureRandom().nextBytes(dek);
        return dek;
    }

    // ─── Inner types ─────────────────────────────────────────────────────────

    public record WrapResult(byte[] wrappedDek, byte[] iv) {}

    /**
     * Holds a cached DEK with its expiry time.
     * NOT a Java record so we can implement zero().
     */
    private static final class CachedDek {
        private final byte[] dekBytes;
        private final Instant expiresAt;

        CachedDek(byte[] dekBytes, Instant expiresAt) {
            this.dekBytes = dekBytes;
            this.expiresAt = expiresAt;
        }

        byte[] dekBytes() { return dekBytes; }

        boolean isExpired() { return Instant.now().isAfter(expiresAt); }

        /** Zeros the DEK bytes so the GC can reclaim the memory. */
        void zero() { Arrays.fill(dekBytes, (byte) 0); }
    }
}
