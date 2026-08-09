package com.vault.api.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /auth/unlock.
 */
public record UnlockRequest(

    @NotBlank(message = "User ID must not be blank")
    String userId,

    @NotBlank(message = "Master password must not be blank")
    String masterPassword

) {}
