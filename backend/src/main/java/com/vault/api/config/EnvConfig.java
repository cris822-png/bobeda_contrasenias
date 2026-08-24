package com.vault.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that all required environment variables are present at startup.
 * The application will refuse to start (with a clear error) if any are missing.
 *
 * IMPORTANT: This class must NEVER log the values of sensitive variables
 * (DB_PASSWORD, JWT_SECRET, DATABASE_URL, REDIS_URL). Only the variable name is logged.
 */
@Component
public class EnvConfig {

    private static final Logger log = LoggerFactory.getLogger(EnvConfig.class);

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Value("${DB_HOST:#{null}}")
    private String dbHost;

    @Value("${DB_PORT:#{null}}")
    private String dbPort;

    @Value("${DB_NAME:#{null}}")
    private String dbName;

    @Value("${DB_USER:#{null}}")
    private String dbUser;

    @Value("${DB_PASSWORD:#{null}}")
    private String dbPassword;

    @Value("${REDIS_URL:#{null}}")
    private String redisUrl;

    @Value("${REDIS_HOST:#{null}}")
    private String redisHost;

    @Value("${JWT_SECRET:#{null}}")
    private String jwtSecret;

    @Value("${JWT_EXPIRY_MINUTES:#{null}}")
    private String jwtExpiryMinutes;

    @Value("${PORT:#{null}}")
    private String port;

    @Value("${API_PORT:#{null}}")
    private String apiPort;

    /**
     * Validates that no required variable has a placeholder/empty value.
     * Logs the names of missing variables but never their values.
     */
    @PostConstruct
    public void validateRequiredVars() {
        List<String> missing = new ArrayList<>();
        List<String> placeholders = List.of("CHANGE_ME", "your_", "TODO", "xxx");

        java.util.function.Predicate<String> isInvalid = (v) ->
                v == null || v.isBlank() || placeholders.stream().anyMatch(p -> v.toLowerCase().contains(p.toLowerCase()));

        // 1. Port
        if (isInvalid.test(port) && isInvalid.test(apiPort)) {
            missing.add("PORT or API_PORT");
        }

        // 2. Database
        if (isInvalid.test(databaseUrl)) {
            if (isInvalid.test(dbHost)) missing.add("DB_HOST (or DATABASE_URL)");
            if (isInvalid.test(dbPort)) missing.add("DB_PORT (or DATABASE_URL)");
            if (isInvalid.test(dbName)) missing.add("DB_NAME (or DATABASE_URL)");
            if (isInvalid.test(dbUser)) missing.add("DB_USER (or DATABASE_URL)");
            if (isInvalid.test(dbPassword)) missing.add("DB_PASSWORD (or DATABASE_URL)");
        }

        // 3. Redis
        if (isInvalid.test(redisUrl)) {
            if (isInvalid.test(redisHost)) missing.add("REDIS_HOST (or REDIS_URL)");
        }

        // 4. JWT
        if (isInvalid.test(jwtSecret)) missing.add("JWT_SECRET");
        if (isInvalid.test(jwtExpiryMinutes)) missing.add("JWT_EXPIRY_MINUTES");

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Application cannot start. The following required environment variables " +
                    "are missing or contain placeholder values. " +
                    "Copy backend/.env.example to backend/.env and set real values.\n" +
                    "Missing: " + String.join(", ", missing)
            );
        }

        log.info("✅  All required environment variables are present.");
        if (!isInvalid.test(databaseUrl)) {
            log.info("    DB  → Using DATABASE_URL connection string");
        } else {
            log.info("    DB  → {}:{}/{}", dbHost, dbPort, dbName);
        }

        if (!isInvalid.test(redisUrl)) {
            log.info("    Redis → Using REDIS_URL connection string");
        } else {
            log.info("    Redis → Host: {}", redisHost);
        }

        log.info("    API → port {}", !isInvalid.test(port) ? port : apiPort);
    }
}
