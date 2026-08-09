package com.vault.api.model.dto;

/**
 * Response body for POST /auth/unlock.
 *
 * The token is a short-lived JWT (configurable, default 15 minutes).
 * It must be stored in memory on the client only — never on disk.
 */
public record UnlockResponse(String token) {}
