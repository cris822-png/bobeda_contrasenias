package com.vault.api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /auth/unlock.
 * Users now identify themselves by their chosen username, not by UUID.
 */
public record UnlockRequest(

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
    String username,

    @NotBlank(message = "Master password must not be blank")
    String masterPassword

) {}
