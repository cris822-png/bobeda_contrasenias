package com.vault.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing an encrypted vault entry.
 *
 * passwordCiphertext: AES-256-GCM ciphertext of the user's password.
 * iv: the GCM nonce used to encrypt this specific entry.
 * title and username are stored in plaintext for display/search purposes.
 */
@Entity
@Table(name = "vault_entries")
public class VaultEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column
    private String username;

    @Column(name = "password_ciphertext", nullable = false)
    private byte[] passwordCiphertext;

    @Column(nullable = false)
    private byte[] iv;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Getters / Setters ──────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public byte[] getPasswordCiphertext() { return passwordCiphertext; }
    public void setPasswordCiphertext(byte[] passwordCiphertext) { this.passwordCiphertext = passwordCiphertext; }

    public byte[] getIv() { return iv; }
    public void setIv(byte[] iv) { this.iv = iv; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
