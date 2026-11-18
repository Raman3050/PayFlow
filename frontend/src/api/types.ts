// Shapes returned by the PayFlow API (see README "API Reference").
// BigDecimal amounts arrive as JSON numbers.

export type Role = 'USER' | 'ADMIN';
export type WalletStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED';
export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  email: string;
  role: Role;
}

export interface Wallet {
  id: number;
  userId: number;
  currency: string;
  status: WalletStatus;
  balance: number;
  version: number;
  createdAt: string;
}

export interface LedgerEntry {
  walletId: number;
  type: 'DEBIT' | 'CREDIT';
  amount: number;
  currency: string;
}

export interface Transaction {
  id: number;
  idempotencyKey: string;
  kind: 'TRANSFER' | 'TOP_UP';
  fromWallet: number;
  toWallet: number;
  amount: number;
  currency: string;
  status: TransactionStatus;
  failureReason: string | null;
  underReview: boolean;
  createdAt: string;
  ledgerEntries?: LedgerEntry[];
}

export interface TransferResponse {
  transactionId: number;
  idempotencyKey: string;
  status: TransactionStatus;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface FraudFlag {
  id: number;
  rule: 'AMOUNT_THRESHOLD_EXCEEDED' | 'VELOCITY_CHECK_FAILED' | string;
  description: string;
  reviewed: boolean;
  createdAt: string;
  transactionId: number;
  fromWalletId: number;
  toWalletId: number;
  amount: number;
  currency: string;
  fromWalletStatus: WalletStatus | null;
  fromUserEmail: string | null;
}

export interface ApiErrorBody {
  status?: number;
  error?: string;
  message?: string;
}
