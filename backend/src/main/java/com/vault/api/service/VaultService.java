package com.vault.api.service;

import com.vault.api.crypto.AesGcmService;
import com.vault.api.crypto.DekManager;
import com.vault.api.model.VaultEntry;
import com.vault.api.model.User;
import com.vault.api.model.dto.EntryRequest;
import com.vault.api.model.dto.EntryResponse;
import com.vault.api.model.exception.VaultException;
import com.vault.api.repository.UserRepository;
import com.vault.api.repository.VaultEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Vault CRUD service — encrypts/decrypts vault entries using the user's cached DEK.
 *
 * SECURITY INVARIANTS:
 *   - The DEK is retrieved as a copy from DekManager and zeroed after each operation.
 *   - Decrypted password bytes are zeroed after conversion to String.
 *   - The service always verifies ownership (userId) before any operation.
 */
@Service
public class VaultService {

    private final VaultEntryRepository entryRepository;
    private final UserRepository userRepository;
    private final DekManager dekManager;
    private final AesGcmService aesGcmService;

    public VaultService(VaultEntryRepository entryRepository,
                        UserRepository userRepository,
                        DekManager dekManager,
                        AesGcmService aesGcmService) {
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
        this.dekManager = dekManager;
        this.aesGcmService = aesGcmService;
    }

    /** Lists all entries for the authenticated user, decrypting passwords. */
    @Transactional(readOnly = true)
    public List<EntryResponse> list(UUID userId) {
        byte[] dek = dekManager.getDek(userId);
        try {
            return entryRepository.findByUserIdOrderByTitleAsc(userId).stream()
                    .map(e -> decryptEntry(e, dek))
                    .toList();
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }

    /** Creates a new encrypted vault entry. */
    @Transactional
    public EntryResponse create(UUID userId, EntryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> VaultException.notFound("User not found"));

        byte[] dek = dekManager.getDek(userId);
        try {
            byte[] plaintext = request.password().getBytes(StandardCharsets.UTF_8);
            AesGcmService.EncryptionResult enc = aesGcmService.encrypt(plaintext, dek);
            Arrays.fill(plaintext, (byte) 0);

            VaultEntry entry = new VaultEntry();
            entry.setUser(user);
            entry.setTitle(request.title());
            entry.setUsername(request.username());
            entry.setPasswordCiphertext(enc.ciphertext());
            entry.setIv(enc.iv());

            VaultEntry saved = entryRepository.save(entry);
            return decryptEntry(saved, dek);
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }

    /** Updates an existing entry, re-encrypting the password. */
    @Transactional
    public EntryResponse update(UUID userId, UUID entryId, EntryRequest request) {
        VaultEntry entry = entryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> VaultException.notFound("Entry not found"));

        byte[] dek = dekManager.getDek(userId);
        try {
            byte[] plaintext = request.password().getBytes(StandardCharsets.UTF_8);
            AesGcmService.EncryptionResult enc = aesGcmService.encrypt(plaintext, dek);
            Arrays.fill(plaintext, (byte) 0);

            entry.setTitle(request.title());
            entry.setUsername(request.username());
            entry.setPasswordCiphertext(enc.ciphertext());
            entry.setIv(enc.iv());

            VaultEntry saved = entryRepository.save(entry);
            return decryptEntry(saved, dek);
        } finally {
            Arrays.fill(dek, (byte) 0);
        }
    }

    /** Deletes an entry, ensuring it belongs to the authenticated user. */
    @Transactional
    public void delete(UUID userId, UUID entryId) {
        VaultEntry entry = entryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> VaultException.notFound("Entry not found"));
        entryRepository.delete(entry);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private EntryResponse decryptEntry(VaultEntry entry, byte[] dek) {
        byte[] plaintextBytes = null;
        try {
            plaintextBytes = aesGcmService.decrypt(
                    entry.getPasswordCiphertext(), entry.getIv(), dek);
            String password = new String(plaintextBytes, StandardCharsets.UTF_8);
            return EntryResponse.from(entry, password);
        } finally {
            if (plaintextBytes != null) Arrays.fill(plaintextBytes, (byte) 0);
        }
    }
}
