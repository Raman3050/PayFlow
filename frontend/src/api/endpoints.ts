import { api } from './client';
import type { AuthResponse, FraudFlag, Page, Transaction, TransferResponse, Wallet } from './types';

export const authApi = {
  login: (email: string, password: string) =>
    api<AuthResponse>('/api/v1/auth/login', { method: 'POST', body: { email, password }, auth: false }),
  register: (email: string, password: string) =>
    api<AuthResponse>('/api/v1/auth/register', { method: 'POST', body: { email, password }, auth: false }),
};

export const walletApi = {
  me: () => api<Wallet>('/api/v1/wallets/me'),
  topUp: (amount: number, idempotencyKey: string) =>
    api<Wallet>('/api/v1/wallets/top-up', { method: 'POST', body: { amount, idempotencyKey } }),
};

export interface HistoryQuery {
  page?: number;
  size?: number;
  from?: string; // ISO instant
  to?: string; // ISO instant
}

export const transactionApi = {
  transfer: (body: { idempotencyKey: string; toWalletId: number; amount: number; currency: string }) =>
    api<TransferResponse>('/api/v1/transactions/transfer', { method: 'POST', body }),
  history: ({ page = 0, size = 10, from, to }: HistoryQuery = {}) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    return api<Page<Transaction>>(`/api/v1/transactions/history?${params}`);
  },
  get: (id: number) => api<Transaction>(`/api/v1/transactions/${id}`),
};

export const adminApi = {
  flags: () => api<FraudFlag[]>('/api/v1/admin/fraud/flags'),
  reviewFlag: (id: number) => api<FraudFlag>(`/api/v1/admin/fraud/flags/${id}/review?reviewed=true`, { method: 'POST' }),
  wallet: (walletId: number) => api<Wallet>(`/api/v1/admin/wallets/${walletId}`),
  freeze: (walletId: number) => api<Wallet>(`/api/v1/admin/wallets/${walletId}/freeze`, { method: 'POST' }),
  unfreeze: (walletId: number) => api<Wallet>(`/api/v1/admin/wallets/${walletId}/unfreeze`, { method: 'POST' }),
};
