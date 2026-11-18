import { useEffect, useRef, useState, type FormEvent } from 'react';
import { walletApi } from '../api/endpoints';
import type { Wallet } from '../api/types';
import { newIdempotencyKey } from '../lib/format';
import { ErrorBanner } from './ui';

const PRESETS = [100, 500, 1000];

export function TopUpDialog({ open, onClose, onDone }: { open: boolean; onClose: () => void; onDone: (w: Wallet) => void }) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [amount, setAmount] = useState('500');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  // One key per attempt: retrying after a network error reuses it, so the top-up can't be applied twice
  const keyRef = useRef(newIdempotencyKey());

  useEffect(() => {
    const d = dialogRef.current;
    if (!d) return;
    if (open && !d.open) {
      setError(null);
      keyRef.current = newIdempotencyKey();
      d.showModal();
    }
    if (!open && d.open) d.close();
  }, [open]);

  const value = Number(amount);
  const valid = Number.isFinite(value) && value >= 0.01 && value <= 10000;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setSubmitting(true);
    setError(null);
    try {
      const wallet = await walletApi.topUp(Math.round(value * 100) / 100, keyRef.current);
      keyRef.current = newIdempotencyKey();
      onDone(wallet);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Top-up failed');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <dialog ref={dialogRef} className="dialog" onClose={onClose} aria-labelledby="topup-title">
      <form onSubmit={onSubmit} className="dialog-body">
        <h2 id="topup-title">Top up wallet</h2>
        <p className="muted">Demo funds are added through the ledger like any other transfer. Max $10,000 per top-up.</p>
        {error && <ErrorBanner message={error} />}
        <div className="field">
          <label htmlFor="topup-amount">Amount (USD)</label>
          <input
            id="topup-amount"
            className="input-amount"
            type="number"
            inputMode="decimal"
            min="0.01"
            max="10000"
            step="0.01"
            value={amount}
            onChange={(e) => {
              setAmount(e.target.value);
              keyRef.current = newIdempotencyKey();
            }}
          />
        </div>
        <div className="preset-row">
          {PRESETS.map((p) => (
            <button
              key={p}
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => {
                setAmount(String(p));
                keyRef.current = newIdempotencyKey();
              }}
            >
              ${p}
            </button>
          ))}
        </div>
        <div className="dialog-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={!valid || submitting}>
            {submitting ? 'Adding…' : 'Add funds'}
          </button>
        </div>
      </form>
    </dialog>
  );
}
