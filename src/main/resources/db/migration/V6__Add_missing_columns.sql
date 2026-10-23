-- V6: Add missing columns to ledger_entries and fraud_flags
ALTER TABLE ledger_entries ADD COLUMN IF NOT EXISTS currency VARCHAR(8) NOT NULL DEFAULT 'USD';
ALTER TABLE fraud_flags ADD COLUMN IF NOT EXISTS description VARCHAR(255) NOT NULL DEFAULT '';
