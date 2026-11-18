import { useRef, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../api/client';
import { transactionApi, walletApi } from '../api/endpoints';
import type { TransferResponse } from '../api/types';
import { CheckIcon } from '../components/Icons';
import { ErrorBanner, Loading, Notice } from '../components/ui';
import { formatMoney, newIdempotencyKey } from '../lib/format';
import { useApi } from '../lib/useApi';

const FRAUD_THRESHOLD = 10000;

interface Receipt extends TransferResponse {
  amount: number;
  toWalletId: number;
}

export function SendMoneyPage() {
  const wallet = useApi(() => walletApi.me());
  const [to, setTo] = useState('');
  const [amount, setAmount] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [receipt, setReceipt] = useState<Receipt | null>(null);

  // The idempotency key belongs to one transfer attempt. It is kept if the request
  // fails on the network (so a retry can't double-charge) and replaced when the
  // form changes or after a transfer succeeds.
  const keyRef = useRef(newIdempotencyKey());
  const resetKey = () => {
    keyRef.current = newIdempotencyKey();
  };

  const w = wallet.data;
  const value = Number(amount);
  const toId = Number(to);
  const amountValid = amount !== '' && Number.isFinite(value) && value >= 0.01 && /^\d+(\.\d{1,2})?$/.test(amount);
  const toValid = /^\d+$/.test(to) && toId > 0 && toId !== w?.id;
  const overBalance = !!w && amountValid && value > w.balance;
  const canSubmit = !!w && w.status === 'ACTIVE' && amountValid && toValid && !overBalance && !submitting;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!canSubmit || !w) return;
    setSubmitting(true);
    setError(null);
    try {
      const res = await transactionApi.transfer({
        idempotencyKey: keyRef.current,
        toWalletId: toId,
        amount: value,
        currency: w.currency,
      });
      setReceipt({ ...res, amount: value, toWalletId: toId });
      resetKey();
      void wallet.reload();
    } catch (err) {
      if (err instanceof ApiError && err.status === 409 && err.message.startsWith('Duplicate')) {
        // Our earlier attempt with this key already went through
        setError('This transfer was already processed. Check your transactions before sending again.');
        resetKey();
      } else if (err instanceof ApiError && err.status === 409) {
        setError(err.message);
        resetKey();
      } else {
        setError(err instanceof Error ? err.message : 'Transfer failed');
        // Only network failures keep the key, so pressing Send again is a safe retry
        if (!(err instanceof ApiError && err.status === 0)) resetKey();
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (receipt && w) {
    return (
      <div className="page">
        <h1>Send money</h1>
        <div className="card receipt">
          <div className="receipt-icon">
            <CheckIcon size={26} />
          </div>
          <h2 className="display">{formatMoney(receipt.amount, w.currency)} sent</h2>
          <p className="muted">
            To wallet #{receipt.toWalletId} · Status <strong className="positive">{receipt.status}</strong>
          </p>
          {receipt.amount > FRAUD_THRESHOLD && (
            <Notice>This transfer is above {formatMoney(FRAUD_THRESHOLD)} and has been flagged for a routine review.</Notice>
          )}
          <div className="mono-box">
            <span>Transaction ID: {receipt.transactionId}</span>
            <span>Idempotency key: {receipt.idempotencyKey}</span>
          </div>
          <div className="actions">
            <Link to={`/transactions?selected=${receipt.transactionId}`} className="btn btn-primary">
              View transaction
            </Link>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => {
                setReceipt(null);
                setAmount('');
              }}
            >
              Send another
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>Send money</h1>

      {wallet.error && <ErrorBanner message={wallet.error} onRetry={wallet.reload} />}
      {wallet.loading && !w && <Loading label="Loading wallet…" />}

      {w && (
        <div className="split">
          <form className="card form-card" onSubmit={onSubmit} noValidate>
            {w.status !== 'ACTIVE' && <Notice>Your wallet is {w.status.toLowerCase()}, so you can't send money right now.</Notice>}
            {error && <ErrorBanner message={error} />}

            <div className="field">
              <span className="field-label">From</span>
              <div className="readonly">
                Wallet #{w.id} · {w.currency} · Balance {formatMoney(w.balance, w.currency)}
              </div>
            </div>

            <div className="field">
              <label htmlFor="to">Recipient wallet ID</label>
              <input
                id="to"
                type="text"
                inputMode="numeric"
                placeholder="e.g. 2087"
                value={to}
                onChange={(e) => {
                  setTo(e.target.value.trim());
                  resetKey();
                }}
                aria-invalid={to !== '' && !toValid}
                aria-describedby="to-help"
              />
              <span id="to-help" className={`help ${to !== '' && !toValid ? 'help-error' : ''}`}>
                {to !== '' && toId === w.id
                  ? "That's your own wallet."
                  : to !== '' && !toValid
                    ? 'Enter a numeric wallet ID.'
                    : 'Ask the recipient for the wallet ID shown on their dashboard.'}
              </span>
            </div>

            <div className="field">
              <label htmlFor="amount">Amount ({w.currency})</label>
              <input
                id="amount"
                className="input-amount"
                type="text"
                inputMode="decimal"
                placeholder="0.00"
                value={amount}
                onChange={(e) => {
                  setAmount(e.target.value.trim());
                  resetKey();
                }}
                aria-invalid={amount !== '' && (!amountValid || overBalance)}
                aria-describedby="amount-help"
              />
              <span id="amount-help" className={`help ${amount !== '' && (!amountValid || overBalance) ? 'help-error' : ''}`}>
                {amount !== '' && !amountValid
                  ? 'Enter an amount like 250 or 250.50.'
                  : overBalance
                    ? `That's more than your balance of ${formatMoney(w.balance, w.currency)}.`
                    : ' '}
              </span>
            </div>

            {amountValid && value > FRAUD_THRESHOLD && (
              <Notice>Transfers above {formatMoney(FRAUD_THRESHOLD)} are flagged for a manual review by our team.</Notice>
            )}

            <button type="submit" className="btn btn-primary btn-lg" disabled={!canSubmit}>
              {submitting ? 'Sending…' : `Send ${amountValid ? formatMoney(value, w.currency) : ''}`.trim()}
            </button>
          </form>

          <aside className="side-col">
            <div className="card">
              <h2 className="h-sm">Summary</h2>
              <dl className="summary">
                <div>
                  <dt>To</dt>
                  <dd>{toValid ? `Wallet #${toId}` : '—'}</dd>
                </div>
                <div>
                  <dt>Amount</dt>
                  <dd>{amountValid ? formatMoney(value, w.currency) : '—'}</dd>
                </div>
                <div className="summary-total">
                  <dt>Balance after</dt>
                  <dd>{amountValid && !overBalance ? formatMoney(w.balance - value, w.currency) : '—'}</dd>
                </div>
              </dl>
            </div>
            <div className="card info-card">
              <p>
                <strong>Safe to retry.</strong> Each transfer carries a unique idempotency key, so pressing Send twice or retrying after a
                network error never charges you twice.
              </p>
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}
