import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { CheckIcon } from '../components/Icons';
import { ErrorBanner } from '../components/ui';

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const isLogin = mode === 'login';
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!isLogin && password.length < 8) {
      setError('Password must be at least 8 characters long');
      return;
    }
    setSubmitting(true);
    try {
      if (isLogin) await login(email, password);
      else await register(email, password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth">
      <section className="auth-hero">
        <div className="brand brand-lg">PayFlow</div>
        <h1>Payments you can trust, down to the last cent.</h1>
        <ul className="auth-points">
          <li>
            <CheckIcon size={22} />
            Every transfer recorded in a double-entry ledger
          </li>
          <li>
            <CheckIcon size={22} />
            Idempotent payments — retries never charge twice
          </li>
          <li>
            <CheckIcon size={22} />
            Automatic fraud checks on every transfer
          </li>
        </ul>
      </section>

      <section className="auth-form-wrap">
        <form className="card auth-card" onSubmit={onSubmit} noValidate>
          <div>
            <h2>{isLogin ? 'Sign in' : 'Create your account'}</h2>
            <p className="muted">
              {isLogin ? 'Welcome back. Enter your details to continue.' : 'You get a USD wallet as soon as you sign up.'}
            </p>
          </div>

          {error && <ErrorBanner message={error} />}

          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              autoComplete={isLogin ? 'current-password' : 'new-password'}
              placeholder={isLogin ? '••••••••' : 'At least 8 characters'}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting || !email || !password}>
            {submitting ? 'Please wait…' : isLogin ? 'Sign in' : 'Create account'}
          </button>

          <p className="muted center">
            {isLogin ? (
              <>
                New to PayFlow? <Link to="/register">Create an account</Link>
              </>
            ) : (
              <>
                Already have an account? <Link to="/login">Sign in</Link>
              </>
            )}
          </p>
        </form>
      </section>
    </div>
  );
}
