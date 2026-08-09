package com.vault.api.model.dto;

import com.vault.api.model.VaultEntry;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response body for vault entry read operations.
 *
 * The password field contains the decrypted plaintext password,
 * sent over HTTPS only. The client should hold it in memory and
 * clear it after display.
 */
public record EntryResponse(
    UUID id,
    String title,
    String username,
    String password,   // decrypted for the authenticated user
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static EntryResponse from(VaultEntry entry, String decryptedPassword) {
        return new EntryResponse(
            entry.getId(),
            entry.getTitle(),
            entry.getUsername(),
            decryptedPassword,
            entry.getCreatedAt(),
            entry.getUpdatedAt()
        );
    }
}
