-- V7: Ledger fixes and system funding wallet

-- 1. Fix the double-entry trigger.
-- It runs BEFORE INSERT, so the counts only include rows that already exist.
-- The old check (count > 1) let a transaction get a second DEBIT or CREDIT row.
CREATE OR REPLACE FUNCTION enforce_double_entry()
RETURNS TRIGGER AS $$
DECLARE
    existing_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO existing_count
    FROM ledger_entries
    WHERE transaction_id = NEW.transaction_id AND type = NEW.type;

    IF existing_count >= 1 THEN
        RAISE EXCEPTION 'Transaction % already has a % entry', NEW.transaction_id, NEW.type;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. System funding account.
-- Top-ups are recorded as a normal double-entry transfer from this wallet
-- (DEBIT system, CREDIT user), so every balance is still derived purely from
-- ledger entries. The system wallet's balance going negative represents money
-- that entered the platform from outside.
-- The password hash is not a valid bcrypt hash, so nobody can log in as this user.
INSERT INTO users (email, password_hash, role)
VALUES ('system@payflow.internal', '!login-disabled', 'USER')
ON CONFLICT (email) DO NOTHING;

INSERT INTO wallets (user_id, currency, status)
SELECT id, 'USD', 'ACTIVE' FROM users WHERE email = 'system@payflow.internal'
ON CONFLICT (user_id) DO NOTHING;

-- 3. Indexes for the queries the app now runs.
CREATE INDEX IF NOT EXISTS idx_ledger_wallet_only ON ledger_entries (wallet_id);
CREATE INDEX IF NOT EXISTS idx_fraud_txn_reviewed ON fraud_flags (transaction_id, reviewed);
