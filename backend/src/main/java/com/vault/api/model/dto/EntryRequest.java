package com.vault.api.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /vault/entries and PUT /vault/entries/{id}.
 *
 * The password is sent in plaintext over HTTPS; the server encrypts it
 * before persisting.
 */
public record EntryRequest(

    @NotBlank(message = "Title must not be blank")
    String title,

    String username,

    @NotBlank(message = "Password must not be blank")
    String password

) {}
