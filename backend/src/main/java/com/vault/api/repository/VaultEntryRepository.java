package com.vault.api.repository;

import com.vault.api.model.VaultEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VaultEntryRepository extends JpaRepository<VaultEntry, UUID> {

    /** Returns all entries belonging to the given user, ordered by title. */
    List<VaultEntry> findByUserIdOrderByTitleAsc(UUID userId);

    /** Returns an entry only if it belongs to the given user (prevents IDOR). */
    Optional<VaultEntry> findByIdAndUserId(UUID id, UUID userId);
}
