import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { getSession, onSessionChange, setSession } from '../api/client';
import { authApi } from '../api/endpoints';
import type { AuthResponse } from '../api/types';

interface AuthContextValue {
  user: AuthResponse | null;
  isAdmin: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(getSession());

  // Stay in sync when the API client refreshes tokens or clears an expired session
  useEffect(() => onSessionChange(setUser), []);

  const login = useCallback(async (email: string, password: string) => {
    setSession(await authApi.login(email, password));
  }, []);

  const register = useCallback(async (email: string, password: string) => {
    setSession(await authApi.register(email, password));
  }, []);

  const logout = useCallback(() => setSession(null), []);

  const value = useMemo(
    () => ({ user, isAdmin: user?.role === 'ADMIN', login, register, logout }),
    [user, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
