package com.vault.api.model.exception;

import org.springframework.http.HttpStatus;

/**
 * Base application exception that carries an HTTP status code.
 */
public class VaultException extends RuntimeException {

    private final HttpStatus status;

    public VaultException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // ─── Convenience factory methods ────────────────────────────────────────

    public static VaultException notFound(String message) {
        return new VaultException(HttpStatus.NOT_FOUND, message);
    }

    public static VaultException unauthorized(String message) {
        return new VaultException(HttpStatus.UNAUTHORIZED, message);
    }

    public static VaultException conflict(String message) {
        return new VaultException(HttpStatus.CONFLICT, message);
    }

    public static VaultException badRequest(String message) {
        return new VaultException(HttpStatus.BAD_REQUEST, message);
    }

    public static VaultException internal(String message) {
        return new VaultException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public static VaultException tooManyRequests(String message) {
        return new VaultException(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
