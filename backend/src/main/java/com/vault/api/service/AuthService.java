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
 *   - Users are identified by a human-chosen username; the internal UUID is
 *     never returned to or typed by the user.
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
     * the cryptographic parameters. Returns nothing — the username they chose
     * is all they need to log in.
     *
     * @throws VaultException 409 if the username is already taken.
     */
    @Transactional
    public void register(RegisterRequest request) {
        // Trim and validate username uniqueness before doing any crypto work
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw VaultException.conflict("Username already taken");
        }

        // Convert String → char[] immediately, then work with char[]
        char[] masterPassword = request.masterPassword().toCharArray();

        byte[] salt = kdfService.generateSalt();
        DekManager.WrapResult wrap = dekManager.wrapNewDek(masterPassword, salt);
        // masterPassword is zeroed inside wrapNewDek

        User user = new User();
        user.setUsername(username);
        user.setSalt(salt);
        user.setKdfAlgorithm("argon2id");
        user.setKdfIterations(KdfService.ITERATIONS);
        user.setKdfMemory(KdfService.MEMORY_KIB);
        user.setDekWrapped(wrap.wrappedDek());
        user.setDekIv(wrap.iv());

        userRepository.save(user);
        // UUID is intentionally not returned — user logs in by username only
    }

    /**
     * Verifies the master password by attempting to unwrap the DEK.
     * On success, caches the DEK and returns a JWT.
     *
     * @throws VaultException 401 if the username is not found or the password is wrong.
     */
    public UnlockResponse unlock(UnlockRequest request) {
        String username = request.username().trim();

        // Look up by username; same generic error for "not found" and "wrong password"
        // to prevent user enumeration.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> VaultException.unauthorized("Invalid credentials"));

        UUID userId = user.getId();
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
