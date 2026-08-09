package com.vault.api.service;

import com.vault.api.crypto.DekManager;
import com.vault.api.crypto.KdfService;
import com.vault.api.model.User;
import com.vault.api.model.dto.RegisterRequest;
import com.vault.api.model.dto.UnlockRequest;
import com.vault.api.model.dto.UnlockResponse;
import com.vault.api.model.exception.VaultException;
import com.vault.api.repository.UserRepository;
import com.vault.api.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles user registration and unlock (authentication).
 *
 * SECURITY INVARIANTS:
 *   - The master password String is converted to char[] immediately and cleared after use.
 *   - A generic "Invalid credentials" message is returned for all auth failures
 *     to prevent user enumeration.
 *   - Registration intentionally has NO username — each server installation has
 *     one (or a few) users identified by UUID only.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final DekManager dekManager;
    private final KdfService kdfService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${vault.jwt.expiry-minutes:15}")
    private int jwtExpiryMinutes;

    public AuthService(UserRepository userRepository,
                       DekManager dekManager,
                       KdfService kdfService,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.dekManager = dekManager;
        this.kdfService = kdfService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Registers a new user. Generates a salt, wraps a new DEK, and persists
     * the cryptographic parameters. Returns the new user's UUID.
     */
    @Transactional
    public UUID register(RegisterRequest request) {
        // Convert String → char[] immediately, then null the reference
        char[] masterPassword = request.masterPassword().toCharArray();
        // request.masterPassword() reference will be GC'd; we can't zero the String
        // but we work with the char[] from here on.

        byte[] salt = kdfService.generateSalt();
        DekManager.WrapResult wrap = dekManager.wrapNewDek(masterPassword, salt);
        // masterPassword is zeroed inside wrapNewDek

        User user = new User();
        user.setSalt(salt);
        user.setKdfAlgorithm("argon2id");
        user.setKdfIterations(KdfService.ITERATIONS);
        user.setKdfMemory(KdfService.MEMORY_KIB);
        user.setDekWrapped(wrap.wrappedDek());
        user.setDekIv(wrap.iv());

        User saved = userRepository.save(user);
        return saved.getId();
    }

    /**
     * Verifies the master password by attempting to unwrap the DEK.
     * On success, caches the DEK and returns a JWT.
     *
     * @throws VaultException with 401 if the user is not found or password is wrong.
     */
    public UnlockResponse unlock(UnlockRequest request) {
        UUID userId;
        try {
            userId = UUID.fromString(request.userId());
        } catch (IllegalArgumentException e) {
            // Don't reveal whether the user exists
            throw VaultException.unauthorized("Invalid credentials");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> VaultException.unauthorized("Invalid credentials"));

        char[] masterPassword = request.masterPassword().toCharArray();
        try {
            dekManager.unlockAndCache(
                    userId,
                    masterPassword,            // zeroed inside unlockAndCache
                    user.getSalt(),
                    user.getDekWrapped(),
                    user.getDekIv(),
                    (long) jwtExpiryMinutes * 60
            );
        } catch (RuntimeException e) {
            // AEADBadTagException → wrong password
            throw VaultException.unauthorized("Invalid credentials");
        }

        String token = jwtTokenProvider.generateToken(userId);
        return new UnlockResponse(token);
    }

    /**
     * Explicitly locks (clears) the DEK cache for the given user.
     * Call this on logout.
     */
    public void lock(UUID userId) {
        dekManager.lock(userId);
    }
}
