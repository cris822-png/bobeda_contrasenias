package com.vault.api.controller;

import com.vault.api.model.dto.RegisterRequest;
import com.vault.api.model.dto.UnlockRequest;
import com.vault.api.model.dto.UnlockResponse;
import com.vault.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for authentication endpoints.
 *
 * POST /auth/register  — create user
 * POST /auth/unlock    — verify master password → JWT
 * POST /auth/lock      — explicit logout (clears DEK from memory)
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
     * Returns 201 No Content — the user logs in with the username they just chose.
     * The internal UUID is not exposed.
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(201).build();
    }

    /**
     * Unlock the vault with the master password.
     * Returns 200 with a short-lived JWT on success, 401 on failure.
     */
    @PostMapping("/unlock")
    public ResponseEntity<UnlockResponse> unlock(
            @Valid @RequestBody UnlockRequest request) {
        UnlockResponse response = authService.unlock(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Explicit lock — clears the DEK from server memory.
     * Call this on client logout/lock, in addition to discarding the JWT.
     */
    @PostMapping("/lock")
    public ResponseEntity<Void> lock(
            @AuthenticationPrincipal UUID userId) {
        if (userId != null) {
            authService.lock(userId);
        }
        return ResponseEntity.noContent().build();
    }
}
