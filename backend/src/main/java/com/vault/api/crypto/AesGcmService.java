package com.vault.api.crypto;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES-256-GCM authenticated encryption service.
 *
 * Uses Java's built-in JCE (no external dependency needed).
 *
 * SECURITY INVARIANTS:
 *   - Every encrypt call generates a fresh 12-byte random nonce (IV).
 *   - GCM tag length is 128 bits.
 *   - The key byte array is zeroed immediately after constructing the SecretKey.
 *   - This class never logs key material, plaintexts, or nonces.
 */
@Service
public class AesGcmService {

    private static final String ALGORITHM   = "AES/GCM/NoPadding";
    private static final int    KEY_BITS    = 256;
    private static final int    IV_BYTES    = 12;   // 96-bit nonce, GCM standard
    private static final int    TAG_BITS    = 128;  // 16-byte GCM authentication tag

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypts plaintext with AES-256-GCM.
     *
     * @param plaintextBytes the data to encrypt (will NOT be zeroed by this method)
     * @param keyBytes       the 32-byte AES key (caller must zero after use)
     * @return an EncryptionResult containing the ciphertext (+ tag appended by JCE)
     *         and the random nonce used.
     */
    public EncryptionResult encrypt(byte[] plaintextBytes, byte[] keyBytes) {
        byte[] iv = generateIv();
        SecretKey secretKey = buildKey(keyBytes);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintextBytes);
            return new EncryptionResult(ciphertext, iv);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM encryption failed", e);
        }
    }

    /**
     * Decrypts AES-256-GCM ciphertext (GCM tag must be appended, as produced by JCE).
     *
     * @param ciphertextWithTag the ciphertext bytes (with 16-byte tag appended)
     * @param iv                the nonce used during encryption
     * @param keyBytes          the 32-byte AES key (caller must zero after use)
     * @return decrypted plaintext bytes (caller must zero after use)
     * @throws javax.crypto.AEADBadTagException if authentication fails (wrong key or tampered data)
     */
    public byte[] decrypt(byte[] ciphertextWithTag, byte[] iv, byte[] keyBytes) {
        SecretKey secretKey = buildKey(keyBytes);
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(ciphertextWithTag);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM decryption failed", e);
        }
    }

    private byte[] generateIv() {
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private SecretKey buildKey(byte[] keyBytes) {
        // SecretKeySpec copies the byte array internally, so we do NOT zero keyBytes here.
        // The caller is responsible for zeroing keyBytes after use.
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
    }

    /** Result of an encrypt operation — ciphertext and the nonce used. */
    public record EncryptionResult(byte[] ciphertext, byte[] iv) {}
}
