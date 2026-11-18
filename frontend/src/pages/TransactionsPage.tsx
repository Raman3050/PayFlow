import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { transactionApi, walletApi } from '../api/endpoints';
import type { Transaction } from '../api/types';
import { EmptyState, ErrorBanner, Loading, Notice, TxStatusChip } from '../components/ui';
import { describe, formatDateTime, formatFullDateTime, formatMoney, signedAmount } from '../lib/format';
import { useApi } from '../lib/useApi';

const PAGE_SIZE = 10;

function startOfDayIso(date: string) {
  return date ? new Date(`${date}T00:00:00`).toISOString() : undefined;
}
function endOfDayIso(date: string) {
  return date ? new Date(`${date}T23:59:59.999`).toISOString() : undefined;
}

export function TransactionsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');

  const selectedId = Number(searchParams.get('selected')) || null;
  const select = (id: number) => setSearchParams({ selected: String(id) }, { replace: true });

  const wallet = useApi(() => walletApi.me());
  const history = useApi(
    () => transactionApi.history({ page, size: PAGE_SIZE, from: startOfDayIso(fromDate), to: endOfDayIso(toDate) }),
    [page, fromDate, toDate],
  );

  // Select the first row when nothing is selected yet
  useEffect(() => {
    const first = history.data?.content[0];
    if (!selectedId && first) select(first.id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [history.data]);

  const myWalletId = wallet.data?.id;
  const rows = history.data?.content ?? [];
  const dateRangeInvalid = !!fromDate && !!toDate && fromDate > toDate;

  return (
    <div className="page">
      <header className="page-header">
        <h1>Transactions</h1>
        <div className="filters">
          <div className="field field-inline">
            <label htmlFor="from">From</label>
            <input
              id="from"
              type="date"
              value={fromDate}
              max={toDate || undefined}
              onChange={(e) => {
                setFromDate(e.target.value);
                setPage(0);
              }}
            />
          </div>
          <div className="field field-inline">
            <label htmlFor="to">To</label>
            <input
              id="to"
              type="date"
              value={toDate}
              min={fromDate || undefined}
              onChange={(e) => {
                setToDate(e.target.value);
                setPage(0);
              }}
            />
          </div>
          {(fromDate || toDate) && (
            <button
              type="button"
              className="btn btn-link"
              onClick={() => {
                setFromDate('');
                setToDate('');
                setPage(0);
              }}
            >
              Clear
            </button>
          )}
        </div>
      </header>

      {dateRangeInvalid && <Notice>The "From" date is after the "To" date.</Notice>}
      {history.error && <ErrorBanner message={history.error} onRetry={history.reload} />}

      <div className="split">
        <section className="card table-card" aria-label="Transaction list">
          {history.loading && !history.data ? (
            <Loading />
          ) : rows.length === 0 ? (
            <EmptyState>No transactions {fromDate || toDate ? 'in this date range' : 'yet'}.</EmptyState>
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Transaction</th>
                    <th className="hide-sm">Date</th>
                    <th>Status</th>
                    <th className="num">Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((tx) => {
                    const d = describe(tx, myWalletId);
                    const isSelected = tx.id === selectedId;
                    return (
                      <tr key={tx.id} className={isSelected ? 'selected' : ''}>
                        <td className="cell-button">
                          <button type="button" onClick={() => select(tx.id)} aria-pressed={isSelected}>
                            <span className="strong">{d.label}</span>
                            <span className="mono small muted">ID {tx.id}</span>
                          </button>
                        </td>
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

          {history.data && history.data.totalPages > 1 && (
            <div className="pager">
              <span className="muted">
                Page {history.data.number + 1} of {history.data.totalPages}
              </span>
              <div className="actions">
                <button type="button" className="btn btn-secondary btn-sm" disabled={history.data.first} onClick={() => setPage((p) => p - 1)}>
                  Previous
                </button>
                <button type="button" className="btn btn-secondary btn-sm" disabled={history.data.last} onClick={() => setPage((p) => p + 1)}>
                  Next
                </button>
              </div>
            </div>
          )}
        </section>

        <aside className="side-col">{selectedId && <TransactionDetail id={selectedId} myWalletId={myWalletId} />}</aside>
      </div>
    </div>
  );
}

function TransactionDetail({ id, myWalletId }: { id: number; myWalletId: number | undefined }) {
  const { data: tx, error, loading, reload } = useApi<Transaction>(() => transactionApi.get(id), [id]);

  if (loading && !tx) {
    return (
      <div className="card">
        <Loading label="Loading details…" />
      </div>
    );
  }
  if (error) return <ErrorBanner message={error} onRetry={reload} />;
  if (!tx) return null;

  const d = describe(tx, myWalletId);
  return (
    <div className="card detail" aria-label="Transaction details">
      <div>
        <span className="label">Transaction details</span>
        <div className="display">{signedAmount(tx, d.incoming)}</div>
        <span className="muted">
          {d.label} · {formatFullDateTime(tx.createdAt)}
        </span>
      </div>

      <div className="stack-sm">
        <h2 className="h-sm">Ledger entries</h2>
        <div className="ledger">
          {tx.ledgerEntries?.map((e) => (
            <div key={`${e.walletId}-${e.type}`} className="ledger-row">
              <span>
                <strong className={e.type === 'DEBIT' ? 'debit' : 'credit'}>{e.type}</strong> ·{' '}
                {tx.kind === 'TOP_UP' && e.type === 'DEBIT' ? 'PayFlow funding' : `Wallet #${e.walletId}`}
                {e.walletId === myWalletId && ' (you)'}
              </span>
              <span className="strong">{formatMoney(e.amount, e.currency)}</span>
            </div>
          ))}
        </div>
        <span className="muted small">Debits and credits always balance, enforced in the database.</span>
      </div>

      <div className="mono-box">
        <span>Transaction ID: {tx.id}</span>
        <span>Idempotency key: {tx.idempotencyKey}</span>
        <span>Status: {tx.status}</span>
      </div>

      {tx.underReview && <Notice>This transfer was flagged by our fraud checks and is being reviewed by our team.</Notice>}
    </div>
  );
}
