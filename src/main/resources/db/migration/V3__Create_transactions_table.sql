-- V3: Create transactions table
-- idempotency_key enforces the "no double charge" guarantee
CREATE TABLE IF NOT EXISTS transactions (
    id               BIGSERIAL PRIMARY KEY,
    idempotency_key  VARCHAR(128) NOT NULL UNIQUE,
    from_wallet      BIGINT NOT NULL,
    to_wallet        BIGINT NOT NULL,
    amount           NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency         VARCHAR(8) NOT NULL DEFAULT 'USD',
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason   TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_txn_from_wallet FOREIGN KEY (from_wallet) REFERENCES wallets (id) ON DELETE RESTRICT,
    CONSTRAINT fk_txn_to_wallet   FOREIGN KEY (to_wallet)   REFERENCES wallets (id) ON DELETE RESTRICT,
    CONSTRAINT chk_txn_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED'))
);

CREATE INDEX IF NOT EXISTS idx_txn_idempotency ON transactions (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_txn_from ON transactions (from_wallet, created_at);
CREATE INDEX IF NOT EXISTS idx_txn_to ON transactions (to_wallet, created_at);
CREATE INDEX IF NOT EXISTS idx_txn_status ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_txn_created ON transactions (created_at DESC);