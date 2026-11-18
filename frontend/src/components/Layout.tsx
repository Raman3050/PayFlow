import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { onSlowRequests } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { CloseIcon, MenuIcon } from './Icons';

export function Layout() {
  const { user, isAdmin, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const [slow, setSlow] = useState(false);
  const location = useLocation();

  useEffect(() => onSlowRequests(setSlow), []);
  useEffect(() => setMenuOpen(false), [location.pathname]);

  return (
    <div className="shell">
      <aside className={`sidebar ${menuOpen ? 'open' : ''}`}>
        <div className="sidebar-top">
          <div className="brand">
            PayFlow
            {isAdmin && <span className="admin-badge">ADMIN</span>}
          </div>
          <button
            type="button"
            className="icon-btn menu-toggle"
            aria-label={menuOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((o) => !o)}
          >
            {menuOpen ? <CloseIcon /> : <MenuIcon />}
          </button>
        </div>
        <nav aria-label="Main" className="nav">
          <NavLink to="/" end>
            Dashboard
          </NavLink>
          <NavLink to="/send">Send money</NavLink>
          <NavLink to="/transactions">Transactions</NavLink>
          {isAdmin && <NavLink to="/admin">Fraud review</NavLink>}
        </nav>
        <div className="sidebar-footer">
          <span className="muted-light">Signed in as</span>
          <span className="sidebar-email">{user?.email}</span>
          <button type="button" className="btn btn-ghost-dark" onClick={logout}>
            Sign out
          </button>
        </div>
      </aside>

      <main className="content">
        {slow && (
          <div className="alert alert-info wake-banner" role="status">
            <span className="spinner" aria-hidden="true" />
            The server is waking up. This can take up to a minute on the free hosting tier.
          </div>
        )}
        <Outlet />
      </main>
    </div>
  );
}
