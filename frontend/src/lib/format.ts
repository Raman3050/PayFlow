import type { Transaction } from '../api/types';

export function formatMoney(amount: number, currency = 'USD') {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(amount);
}

export function formatDateTime(iso: string) {
  return new Intl.DateTimeFormat('en-IN', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(iso));
}

export function formatFullDateTime(iso: string) {
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso));
}

/** How a transaction looks from the point of view of the given wallet. */
export function describe(tx: Transaction, myWalletId: number | undefined) {
  const incoming = tx.toWallet === myWalletId;
  if (tx.kind === 'TOP_UP') {
    return { label: 'Top-up', incoming: true, counterparty: null as number | null };
  }
  return incoming
    ? { label: `Received from wallet #${tx.fromWallet}`, incoming: true, counterparty: tx.fromWallet }
    : { label: `Sent to wallet #${tx.toWallet}`, incoming: false, counterparty: tx.toWallet };
}

export function signedAmount(tx: Transaction, incoming: boolean) {
  return `${incoming ? '+' : '−'} ${formatMoney(tx.amount, tx.currency)}`;
}

export function newIdempotencyKey() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID();
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
