-- Flyway migration V3
-- Create banned_ips table for escalating bans

CREATE TABLE IF NOT EXISTS vault.banned_ips (
    ip_address      INET        PRIMARY KEY,
    ban_count       INTEGER     NOT NULL DEFAULT 0,
    banned_until    TIMESTAMPTZ NOT NULL,
    last_banned_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index creation commented out because table is owned by postgres user and index already exists.
-- CREATE INDEX IF NOT EXISTS idx_banned_ips_until ON vault.banned_ips (banned_until);
