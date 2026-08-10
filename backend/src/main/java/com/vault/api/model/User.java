package com.vault.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a vault user.
 *
 * Security invariants:
 *  - The master password is NEVER stored here.
 *  - salt, dekWrapped, dekIv are raw bytes (BYTEA in PostgreSQL).
 *  - kdfAlgorithm / kdfIterations / kdfMemory are the parameters needed
 *    to re-derive the KEK from the master password on unlock.
 */
@Entity
@Table(name = "users", schema = "vault")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Human-chosen login name. Stored as CITEXT (case-insensitive) in PostgreSQL. */
    @Column(nullable = false, unique = true, updatable = false)
    private String username;

    @Column(nullable = false)
    private byte[] salt;

    @Column(name = "kdf_algorithm", nullable = false)
    private String kdfAlgorithm = "argon2id";

    @Column(name = "kdf_iterations", nullable = false)
    private int kdfIterations;

    @Column(name = "kdf_memory", nullable = false)
    private int kdfMemory; // KiB

    @Column(name = "dek_wrapped", nullable = false)
    private byte[] dekWrapped;

    @Column(name = "dek_iv", nullable = false)
    private byte[] dekIv;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ─── Getters / Setters ──────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public byte[] getSalt() { return salt; }
    public void setSalt(byte[] salt) { this.salt = salt; }

    public String getKdfAlgorithm() { return kdfAlgorithm; }
    public void setKdfAlgorithm(String kdfAlgorithm) { this.kdfAlgorithm = kdfAlgorithm; }

    public int getKdfIterations() { return kdfIterations; }
    public void setKdfIterations(int kdfIterations) { this.kdfIterations = kdfIterations; }

    public int getKdfMemory() { return kdfMemory; }
    public void setKdfMemory(int kdfMemory) { this.kdfMemory = kdfMemory; }

    public byte[] getDekWrapped() { return dekWrapped; }
    public void setDekWrapped(byte[] dekWrapped) { this.dekWrapped = dekWrapped; }

    public byte[] getDekIv() { return dekIv; }
    public void setDekIv(byte[] dekIv) { this.dekIv = dekIv; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
