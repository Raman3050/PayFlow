import { useState, type FormEvent } from 'react';
import { adminApi } from '../api/endpoints';
import type { FraudFlag, Wallet } from '../api/types';
import { EmptyState, ErrorBanner, Loading, WalletStatusChip } from '../components/ui';
import { formatDateTime, formatMoney } from '../lib/format';
import { useApi } from '../lib/useApi';

export function AdminPage() {
  const flags = useApi(() => adminApi.flags());
  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  async function run(key: string, action: () => Promise<unknown>) {
    setBusy(key);
    setActionError(null);
    try {
      await action();
      await flags.reload();
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Action failed');
    } finally {
      setBusy(null);
    }
  }

  const list = flags.data ?? [];

  return (
    <div className="page">
      <header>
        <h1>Fraud review</h1>
        <p className="muted">Transfers flagged by the fraud engine. Review each one and freeze the wallet if needed.</p>
      </header>

      <section className="stat-grid" aria-label="Overview">
        <div className="card stat">
          <span className="muted">Unreviewed flags</span>
          <span className="display">{flags.data ? list.length : '—'}</span>
        </div>
        <div className="card stat">
          <span className="muted">Amount threshold</span>
          <span className="display">$10,000</span>
        </div>
        <div className="card stat">
          <span className="muted">Velocity limit</span>
          <span className="display">5 / 5 min</span>
        </div>
      </section>

      {flags.error && <ErrorBanner message={flags.error} onRetry={flags.reload} />}
      {actionError && <ErrorBanner message={actionError} />}

      <section aria-label="Flagged transfers" className="stack">
        {flags.loading && !flags.data ? (
          <Loading label="Loading flags…" />
        ) : list.length === 0 ? (
          <EmptyState>All caught up — no flags waiting for review.</EmptyState>
        ) : (
          list.map((f) => <FlagCard key={f.id} flag={f} busy={busy} run={run} />)
        )}
      </section>

      <WalletLookup />
    </div>
  );
}

function FlagCard({
  flag: f,
  busy,
  run,
}: {
  flag: FraudFlag;
  busy: string | null;
  run: (key: string, action: () => Promise<unknown>) => Promise<void>;
}) {
  const frozen = f.fromWalletStatus === 'FROZEN';
  return (
    <article className="card flag">
      <div className="flag-main">
        <div className="row-wrap">
          <span className="chip chip-warning mono">{f.rule}</span>
          <span className="muted small">{formatDateTime(f.createdAt)}</span>
        </div>
        <span className="flag-title">
          {formatMoney(f.amount, f.currency)} · wallet #{f.fromWalletId} → #{f.toWalletId}
        </span>
        <span className="muted">{f.description}</span>
        <span className="mono small muted">
          Transaction {f.transactionId}
          {f.fromUserEmail && ` · ${f.fromUserEmail}`}
        </span>
      </div>
      <div className="flag-actions">
        <WalletStatusChip status={f.fromWalletStatus} />
        <button
          type="button"
          className="btn btn-secondary"
          disabled={busy !== null}
          onClick={() =>
            run(`wallet-${f.id}`, () => (frozen ? adminApi.unfreeze(f.fromWalletId) : adminApi.freeze(f.fromWalletId)))
          }
        >
          {busy === `wallet-${f.id}` ? 'Saving…' : frozen ? 'Unfreeze wallet' : 'Freeze wallet'}
        </button>
        <button type="button" className="btn btn-primary" disabled={busy !== null} onClick={() => run(`review-${f.id}`, () => adminApi.reviewFlag(f.id))}>
          {busy === `review-${f.id}` ? 'Saving…' : 'Mark reviewed'}
        </button>
      </div>
    </article>
  );
}

function WalletLookup() {
  const [query, setQuery] = useState('');
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function lookup(e: FormEvent) {
    e.preventDefault();
    const id = Number(query);
    if (!Number.isInteger(id) || id <= 0) {
      setError('Enter a numeric wallet ID.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      setWallet(await adminApi.wallet(id));
    } catch (err) {
      setWallet(null);
      setError(err instanceof Error ? err.message : 'Lookup failed');
    } finally {
      setBusy(false);
    }
  }

  async function toggle() {
    if (!wallet) return;
    setBusy(true);
    setError(null);
    try {
      setWallet(wallet.status === 'FROZEN' ? await adminApi.unfreeze(wallet.id) : await adminApi.freeze(wallet.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Action failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card stack-sm" aria-labelledby="lookup-heading">
      <h2 id="lookup-heading">Wallet lookup</h2>
      <form className="row-wrap" onSubmit={lookup}>
        <label htmlFor="wallet-id" className="sr-only">
          Wallet ID
        </label>
        <input
          id="wallet-id"
          type="text"
          inputMode="numeric"
          placeholder="Wallet ID"
          value={query}
          onChange={(e) => setQuery(e.target.value.trim())}
          className="input-narrow"
        />
        <button type="submit" className="btn btn-secondary" disabled={busy || !query}>
          Look up
        </button>
      </form>
      {error && <ErrorBanner message={error} />}
      {wallet && (
        <div className="row-between lookup-result">
          <span>
            Wallet <strong>#{wallet.id}</strong> · user {wallet.userId} · {formatMoney(wallet.balance, wallet.currency)}
          </span>
          <span className="row-wrap">
            <WalletStatusChip status={wallet.status} />
            <button type="button" className="btn btn-secondary btn-sm" onClick={toggle} disabled={busy}>
              {wallet.status === 'FROZEN' ? 'Unfreeze' : 'Freeze'}
            </button>
          </span>
        </div>
      )}
    </section>
  );
}
