import { useState } from 'react';
import { Link } from 'react-router-dom';
import { transactionApi, walletApi } from '../api/endpoints';
import { CopyIcon } from '../components/Icons';
import { TopUpDialog } from '../components/TopUpDialog';
import { EmptyState, ErrorBanner, Loading, TxStatusChip, WalletStatusChip } from '../components/ui';
import { describe, formatDateTime, formatMoney, signedAmount } from '../lib/format';
import { useApi } from '../lib/useApi';

export function DashboardPage() {
  const wallet = useApi(() => walletApi.me());
  const recent = useApi(() => transactionApi.history({ page: 0, size: 5 }));
  const [topUpOpen, setTopUpOpen] = useState(false);
  const [copied, setCopied] = useState(false);

  const w = wallet.data;

  async function copyWalletId() {
    if (!w) return;
    try {
      await navigator.clipboard.writeText(String(w.id));
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // clipboard blocked; the ID is visible on screen anyway
    }
  }

  return (
    <div className="page">
      <header className="page-header">
        <h1>Wallet</h1>
        <div className="actions">
          <button type="button" className="btn btn-secondary" onClick={() => setTopUpOpen(true)} disabled={!w || w.status !== 'ACTIVE'}>
            Top up
          </button>
          <Link to="/send" className="btn btn-primary">
            Send money
          </Link>
        </div>
      </header>

      {wallet.error && <ErrorBanner message={wallet.error} onRetry={wallet.reload} />}

      <section className="balance-grid" aria-label="Balance">
        <div className="card balance-card">
          {wallet.loading && !w ? (
            <Loading label="Loading wallet…" />
          ) : w ? (
            <>
              <div className="row-between">
                <span className="label">Available balance</span>
                <WalletStatusChip status={w.status} />
              </div>
              <div className="balance">{formatMoney(w.balance, w.currency)}</div>
              <div className="meta-row">
                <span>
                  Wallet ID <strong>#{w.id}</strong>
                  <button type="button" className="icon-btn icon-btn-sm" onClick={copyWalletId} aria-label="Copy wallet ID">
                    <CopyIcon size={16} />
                  </button>
                  {copied && <span className="copied">Copied</span>}
                </span>
                <span>
                  Currency <strong>{w.currency}</strong>
                </span>
              </div>
              {w.status === 'FROZEN' && (
                <p className="frozen-note">Your wallet is frozen by an administrator. You can't send or receive money right now.</p>
              )}
            </>
          ) : null}
        </div>
        <div className="card info-card">
          <span className="info-title">Ledger-backed balance</span>
          <p>Your balance is calculated from double-entry ledger records. Every transfer posts one debit and one credit.</p>
          <p className="muted small">Share your wallet ID with people who want to send you money.</p>
          <Link to="/transactions" className="link-strong">
            View ledger entries →
          </Link>
        </div>
      </section>

      <section className="card" aria-labelledby="recent-heading">
        <div className="row-between">
          <h2 id="recent-heading">Recent transactions</h2>
          <Link to="/transactions" className="link-strong">
            See all
          </Link>
        </div>
        {recent.error && <ErrorBanner message={recent.error} onRetry={recent.reload} />}
        {recent.loading && !recent.data ? (
          <Loading />
        ) : recent.data && recent.data.content.length === 0 ? (
          <EmptyState>
            No transactions yet. <button type="button" className="btn btn-link" onClick={() => setTopUpOpen(true)}>Top up your wallet</button> to get started.
          </EmptyState>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Description</th>
                  <th className="hide-sm">Date</th>
                  <th>Status</th>
                  <th className="num">Amount</th>
                </tr>
              </thead>
              <tbody>
                {recent.data?.content.map((tx) => {
                  const d = describe(tx, w?.id);
                  return (
                    <tr key={tx.id}>
                      <td className="strong">{d.label}</td>
                      <td className="muted hide-sm">{formatDateTime(tx.createdAt)}</td>
                      <td>
                        <TxStatusChip status={tx.status} underReview={tx.underReview} />
                      </td>
                      <td className={`num strong ${d.incoming ? 'positive' : ''}`}>{signedAmount(tx, d.incoming)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <TopUpDialog
        open={topUpOpen}
        onClose={() => setTopUpOpen(false)}
        onDone={(updated) => {
          wallet.setData(updated);
          void recent.reload();
        }}
      />
    </div>
  );
}
