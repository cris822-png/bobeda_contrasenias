package com.vault.api.repository;

import com.vault.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Look up a user by their chosen username (CITEXT — case-insensitive in DB). */
    Optional<User> findByUsername(String username);

    /** Check uniqueness before registration to return a clear 409 instead of a DB error. */
    boolean existsByUsername(String username);
}
