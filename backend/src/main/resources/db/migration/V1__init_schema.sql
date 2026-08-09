-- Bóveda de Contraseñas — Database Schema
-- Flyway migration V1
-- Based on: schema_para_agente.md

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ─── Users ───────────────────────────────────────────────────────────────────
-- The master password is NEVER stored. Only the cryptographic parameters needed
-- to derive the Key Encryption Key (KEK) and the wrapped Data Encryption Key
-- (DEK) are persisted here.
CREATE TABLE users (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    salt            BYTEA       NOT NULL,
    kdf_algorithm   TEXT        NOT NULL DEFAULT 'argon2id',
    kdf_iterations  INTEGER     NOT NULL,
    kdf_memory      INTEGER     NOT NULL,  -- in KiB (for Argon2id)
    dek_wrapped     BYTEA       NOT NULL,  -- AES-256-GCM ciphertext of the DEK
    dek_iv          BYTEA       NOT NULL,  -- GCM nonce used to wrap the DEK
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ─── Vault Entries ───────────────────────────────────────────────────────────
-- password_ciphertext is encrypted with the user's DEK (AES-256-GCM).
-- title and username are stored in plaintext for search convenience;
-- if you need them encrypted too, apply the same pattern as password_ciphertext.
CREATE TABLE vault_entries (
    id                  UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title               TEXT        NOT NULL,
    username            TEXT,
    password_ciphertext BYTEA       NOT NULL,
    iv                  BYTEA       NOT NULL,  -- GCM nonce for this entry
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vault_entries_user_id ON vault_entries (user_id);
