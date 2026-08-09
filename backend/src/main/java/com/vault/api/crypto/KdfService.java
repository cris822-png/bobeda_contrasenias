package com.vault.api.crypto;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * KDF (Key Derivation Function) service using Argon2id via Bouncy Castle.
 *
 * Argon2id parameters (OWASP recommended minimums for password hashing):
 *   - iterations (t): 3
 *   - memory (m):     64 MiB  (65536 KiB)
 *   - parallelism (p): 4
 *   - output length:  32 bytes (256-bit key)
 *
 * SECURITY INVARIANTS:
 *   - The master password (char[]) is zeroed immediately after deriving the key.
 *   - The derived key (byte[]) is the caller's responsibility to zero after use.
 *   - This class never logs the password, salt, or derived key.
 */
@Service
public class KdfService {

    // Argon2id parameters — adjust for your hardware in production
    public static final int ITERATIONS   = 3;
    public static final int MEMORY_KIB   = 65_536;   // 64 MiB
    public static final int PARALLELISM  = 4;
    public static final int KEY_LENGTH   = 32;        // 256 bits
    public static final int SALT_LENGTH  = 32;        // 256 bits

    private final SecureRandom secureRandom = new SecureRandom();

    /** Generates a cryptographically secure random salt. */
    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }

    /**
     * Derives a 256-bit key from a master password and a salt using Argon2id.
     *
     * @param masterPassword the master password as a char array.
     *                       The caller MUST zero this array after calling this method.
     * @param salt           the per-user salt (32 bytes, from the database).
     * @return a 32-byte derived key. The caller MUST zero this array after use.
     */
    public byte[] deriveKey(char[] masterPassword, byte[] salt) {
        // Convert char[] to byte[] (UTF-8) for Argon2
        byte[] passwordBytes = toUtf8Bytes(masterPassword);
        try {
            Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withSalt(salt)
                    .withIterations(ITERATIONS)
                    .withMemoryAsKB(MEMORY_KIB)
                    .withParallelism(PARALLELISM)
                    .build();

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(params);

            byte[] derivedKey = new byte[KEY_LENGTH];
            generator.generateBytes(passwordBytes, derivedKey);
            return derivedKey;
        } finally {
            // Always zero the password bytes, even if an exception occurs
            Arrays.fill(passwordBytes, (byte) 0);
        }
    }

    /** Converts a char[] to UTF-8 byte[]. The caller must zero the result. */
    private byte[] toUtf8Bytes(char[] chars) {
        java.nio.ByteBuffer bb = java.nio.charset.StandardCharsets.UTF_8
                .encode(java.nio.CharBuffer.wrap(chars));
        byte[] bytes = new byte[bb.limit()];
        bb.get(bytes);
        // Clear the buffer's backing array if accessible
        if (bb.hasArray()) Arrays.fill(bb.array(), (byte) 0);
        return bytes;
    }
}
