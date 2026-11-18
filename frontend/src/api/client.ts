import type { ApiErrorBody, AuthResponse } from './types';

export const API_URL = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? 'http://localhost:8080';

const SESSION_KEY = 'payflow.session';

/** An error from the API, carrying the HTTP status and the server's message. */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly correlationId: string | null = null,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

// ---------------------------------------------------------------------------
// Session storage
// ---------------------------------------------------------------------------

let session: AuthResponse | null = loadSession();
const listeners = new Set<(s: AuthResponse | null) => void>();

function loadSession(): AuthResponse | null {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  } catch {
    return null;
  }
}

export function getSession() {
  return session;
}

export function setSession(next: AuthResponse | null) {
  session = next;
  try {
    if (next) localStorage.setItem(SESSION_KEY, JSON.stringify(next));
    else localStorage.removeItem(SESSION_KEY);
  } catch {
    // storage unavailable (private mode); keep the session in memory only
  }
  listeners.forEach((l) => l(next));
}

export function onSessionChange(listener: (s: AuthResponse | null) => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

// ---------------------------------------------------------------------------
// "Server is waking up" notice (free hosting sleeps when idle)
// ---------------------------------------------------------------------------

let pending = 0;
let slowTimer: ReturnType<typeof setTimeout> | null = null;
const slowListeners = new Set<(slow: boolean) => void>();

export function onSlowRequests(listener: (slow: boolean) => void) {
  slowListeners.add(listener);
  return () => {
    slowListeners.delete(listener);
  };
}

function trackStart() {
  pending += 1;
  if (!slowTimer) {
    slowTimer = setTimeout(() => slowListeners.forEach((l) => l(true)), 4000);
  }
}

function trackEnd() {
  pending = Math.max(0, pending - 1);
  if (pending === 0) {
    if (slowTimer) clearTimeout(slowTimer);
    slowTimer = null;
    slowListeners.forEach((l) => l(false));
  }
}

// ---------------------------------------------------------------------------
// Requests
// ---------------------------------------------------------------------------

let refreshing: Promise<boolean> | null = null;

/** Swap the refresh token for a new pair. Concurrent 401s share one refresh call. */
async function refreshTokens(): Promise<boolean> {
  const current = session;
  if (!current?.refreshToken) return false;
  if (!refreshing) {
    refreshing = (async () => {
      try {
        const res = await fetch(`${API_URL}/api/v1/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken: current.refreshToken }),
        });
        if (!res.ok) return false;
        setSession((await res.json()) as AuthResponse);
        return true;
      } catch {
        return false;
      } finally {
        refreshing = null;
      }
    })();
  }
  return refreshing;
}

async function toApiError(res: Response): Promise<ApiError> {
  const correlationId = res.headers.get('X-Correlation-ID');
  let message = `Request failed (${res.status})`;
  try {
    const body = (await res.json()) as ApiErrorBody;
    if (body?.message) message = body.message;
  } catch {
    // body wasn't JSON
  }
  if (res.status === 401 && message.startsWith('Request failed')) {
    message = 'Your session has expired. Please sign in again.';
  }
  return new ApiError(res.status, message, correlationId);
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
  auth?: boolean;
}

export async function api<T>(path: string, { method = 'GET', body, auth = true }: RequestOptions = {}): Promise<T> {
  const send = () => {
    const headers: Record<string, string> = {};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (auth && session) headers.Authorization = `Bearer ${session.accessToken}`;
    return fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  };

  trackStart();
  try {
    let res: Response;
    try {
      res = await send();
    } catch {
      throw new ApiError(0, 'Could not reach the PayFlow server. Check your connection and try again.');
    }

    // Access tokens live 15 minutes: refresh once and retry
    if (res.status === 401 && auth && session) {
      if (await refreshTokens()) {
        res = await send();
      } else {
        setSession(null);
      }
    }

    if (!res.ok) throw await toApiError(res);
    if (res.status === 204) return undefined as T;
    return (await res.json()) as T;
  } finally {
    trackEnd();
  }
}
