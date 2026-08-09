package com.vault.api.controller;

import com.vault.api.model.dto.EntryRequest;
import com.vault.api.model.dto.EntryResponse;
import com.vault.api.service.VaultService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for vault CRUD endpoints.
 *
 * All endpoints require a valid JWT (enforced by SecurityConfig).
 * The authenticated user UUID is injected via @AuthenticationPrincipal.
 *
 * GET    /vault/entries      — list all entries
 * POST   /vault/entries      — create a new entry
 * PUT    /vault/entries/{id} — update an entry
 * DELETE /vault/entries/{id} — delete an entry
 */
@RestController
@RequestMapping("/vault/entries")
public class VaultController {

    private final VaultService vaultService;

    public VaultController(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @GetMapping
    public List<EntryResponse> list(@AuthenticationPrincipal UUID userId) {
        return vaultService.list(userId);
    }

    @PostMapping
    public ResponseEntity<EntryResponse> create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody EntryRequest request) {
        EntryResponse created = vaultService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public EntryResponse update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody EntryRequest request) {
        return vaultService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        vaultService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
