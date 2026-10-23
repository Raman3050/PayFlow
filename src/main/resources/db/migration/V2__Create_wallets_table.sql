-- V2: Create wallets table
-- `version` column is the optimistic-locking version field (see concurrency safety)
CREATE TABLE IF NOT EXISTS wallets (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE,
    currency    VARCHAR(8) NOT NULL DEFAULT 'USD',
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_wallet_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'))
);

CREATE INDEX IF NOT EXISTS idx_wallets_user ON wallets (user_id);
CREATE INDEX IF NOT EXISTS idx_wallets_status ON wallets (status);