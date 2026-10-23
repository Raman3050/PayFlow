-- V4: Create ledger_entries table (DOUBLE-ENTRY LEDGER)
-- Every transfer writes exactly one DEBIT row and one CREDIT row.
-- The wallet balance is derived from these rows, never stored redundantly.
CREATE TABLE IF NOT EXISTS ledger_entries (
    id            BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    wallet_id     BIGINT NOT NULL,
    type          VARCHAR(10) NOT NULL,
    amount        NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ledger_txn FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE,
    CONSTRAINT fk_ledger_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id) ON DELETE CASCADE,
    CONSTRAINT chk_ledger_type CHECK (type IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX IF NOT EXISTS idx_ledger_txn ON ledger_entries (transaction_id);
CREATE INDEX IF NOT EXISTS idx_ledger_wallet ON ledger_entries (wallet_id, type, created_at);

-- Enforce double-entry invariant: each transaction must have exactly one DEBIT and one CREDIT
CREATE OR REPLACE FUNCTION enforce_double_entry()
RETURNS TRIGGER AS $$
DECLARE
    debit_count  INTEGER;
    credit_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO debit_count
    FROM ledger_entries
    WHERE transaction_id = NEW.transaction_id AND type = 'DEBIT';
    SELECT COUNT(*) INTO credit_count
    FROM ledger_entries
    WHERE transaction_id = NEW.transaction_id AND type = 'CREDIT';

    IF NEW.type = 'DEBIT' AND debit_count > 1 THEN
        RAISE EXCEPTION 'Transaction % already has a DEBIT entry', NEW.transaction_id;
    END IF;
    IF NEW.type = 'CREDIT' AND credit_count > 1 THEN
        RAISE EXCEPTION 'Transaction % already has a CREDIT entry', NEW.transaction_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_double_entry
BEFORE INSERT ON ledger_entries
FOR EACH ROW EXECUTE FUNCTION enforce_double_entry();