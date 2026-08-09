package com.vault.api.model.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central exception handler for all REST controllers.
 *
 * IMPORTANT: Error messages are deliberately vague for auth failures to prevent
 * information leakage (e.g. "Invalid credentials" not "User not found").
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    record ErrorBody(int status, String error, LocalDateTime timestamp) {}

    @ExceptionHandler(VaultException.class)
    public ResponseEntity<ErrorBody> handleVault(VaultException ex) {
        log.warn("VaultException [{}]: {}", ex.getStatus(), ex.getMessage());
        return ResponseEntity
            .status(ex.getStatus())
            .body(new ErrorBody(ex.getStatus().value(), ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
                                      (a, b) -> a)); // keep first if duplicate
        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "status", 400,
                "error", "Validation failed",
                "fields", fieldErrors,
                "timestamp", LocalDateTime.now()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleGeneric(Exception ex) {
        // Log the real cause server-side; return a generic message to the client.
        log.error("Unhandled exception", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorBody(500, "An internal error occurred", LocalDateTime.now()));
    }
}
