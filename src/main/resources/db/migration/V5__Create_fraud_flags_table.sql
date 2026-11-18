-- V5: Create fraud_flags table
CREATE TABLE IF NOT EXISTS fraud_flags (
    id             BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    rule           VARCHAR(100) NOT NULL,
    reviewed       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_fraud_txn FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_fraud_unreviewed ON fraud_flags (reviewed) WHERE reviewed = FALSE;
CREATE INDEX IF NOT EXISTS idx_fraud_txn ON fraud_flags (transaction_id);