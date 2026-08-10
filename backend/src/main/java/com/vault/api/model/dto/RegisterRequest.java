package com.vault.api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /auth/register.
 *
 * The master password arrives as a String over HTTPS.
 * The service layer immediately converts it to char[] and clears it after use.
 * The username is trimmed server-side before use.
 */
public record RegisterRequest(

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 32, message = "Username must be between 3 and 32 characters")
    String username,

    @NotBlank(message = "Master password must not be blank")
    @Size(min = 12, message = "Master password must be at least 12 characters")
    String masterPassword

) {}
