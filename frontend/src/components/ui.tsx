import type { ReactNode } from 'react';
import type { TransactionStatus, WalletStatus } from '../api/types';
import { AlertIcon } from './Icons';

export function Chip({ tone, children }: { tone: 'success' | 'warning' | 'info' | 'neutral' | 'danger'; children: ReactNode }) {
  return <span className={`chip chip-${tone}`}>{children}</span>;
}

export function TxStatusChip({ status, underReview }: { status: TransactionStatus; underReview: boolean }) {
  if (underReview) return <Chip tone="warning">Under review</Chip>;
  if (status === 'COMPLETED') return <Chip tone="success">Completed</Chip>;
  if (status === 'FAILED') return <Chip tone="danger">Failed</Chip>;
  if (status === 'REVERSED') return <Chip tone="neutral">Reversed</Chip>;
  return <Chip tone="info">Pending</Chip>;
}

export function WalletStatusChip({ status }: { status: WalletStatus | null | undefined }) {
  if (status === 'ACTIVE') return <Chip tone="success">ACTIVE</Chip>;
  if (status === 'FROZEN') return <Chip tone="info">FROZEN</Chip>;
  return <Chip tone="neutral">{status ?? 'UNKNOWN'}</Chip>;
}

export function ErrorBanner({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="alert alert-error" role="alert">
      <AlertIcon />
      <span>{message}</span>
      {onRetry && (
        <button type="button" className="btn btn-link" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function Notice({ tone = 'warning', children }: { tone?: 'warning' | 'info'; children: ReactNode }) {
  return (
    <div className={`alert alert-${tone}`} role="status">
      <AlertIcon />
      <span>{children}</span>
    </div>
  );
}

export function Loading({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="loading" role="status">
      <span className="spinner" aria-hidden="true" />
      {label}
    </div>
  );
}

export function EmptyState({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>;
}
