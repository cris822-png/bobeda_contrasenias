package com.vault.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Validates that all required environment variables are present at startup.
 * The application will refuse to start (with a clear error) if any are missing.
 *
 * IMPORTANT: This class must NEVER log the values of sensitive variables
 * (DB_PASSWORD, JWT_SECRET). Only the variable name is logged.
 */
@Component
public class EnvConfig {

    private static final Logger log = LoggerFactory.getLogger(EnvConfig.class);

    // Required variables mapped from environment
    @Value("${DB_HOST}")
    private String dbHost;

    @Value("${DB_PORT}")
    private String dbPort;

    @Value("${DB_NAME}")
    private String dbName;

    @Value("${DB_USER}")
    private String dbUser;

    @Value("${DB_PASSWORD}")
    private String dbPassword;

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    @Value("${JWT_EXPIRY_MINUTES}")
    private String jwtExpiryMinutes;

    @Value("${API_PORT}")
    private String apiPort;

    /**
     * Validates that no required variable has a placeholder/empty value.
     * Logs the names of missing variables but never their values.
     */
    @PostConstruct
    public void validateRequiredVars() {
        // Map<varName, value> — checked for null/blank/placeholder
        Map<String, String> vars = Map.of(
                "DB_HOST", dbHost,
                "DB_PORT", dbPort,
                "DB_NAME", dbName,
                "DB_USER", dbUser,
                "DB_PASSWORD", dbPassword,
                "JWT_SECRET", jwtSecret,
                "JWT_EXPIRY_MINUTES", jwtExpiryMinutes,
                "API_PORT", apiPort
        );

        List<String> placeholders = List.of("CHANGE_ME", "your_", "TODO", "xxx");

        List<String> missing = vars.entrySet().stream()
                .filter(e -> {
                    String v = e.getValue();
                    if (v == null || v.isBlank()) return true;
                    return placeholders.stream().anyMatch(p -> v.toLowerCase().contains(p.toLowerCase()));
                })
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Application cannot start. The following required environment variables " +
                    "are missing or contain placeholder values. " +
                    "Copy backend/.env.example to backend/.env and set real values.\n" +
                    "Missing: " + String.join(", ", missing)
            );
        }

        log.info("✅  All required environment variables are present.");
        log.info("    DB  → {}:{}/{}", dbHost, dbPort, dbName);
        log.info("    API → port {}", apiPort);
        // JWT_SECRET and DB_PASSWORD are intentionally NOT logged.
    }
}
