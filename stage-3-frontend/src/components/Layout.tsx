// ============================================================================
// LAYOUT — the app shell: header, navigation, and the routed page.
// ============================================================================
// <Outlet/> (react-router) is the hole where the matched child route renders.
// Every page therefore shares one nav bar without nesting components by hand.
//
// NAV IS ROLE-AWARE: staff see the loan queue, customers see their loans —
// the same @PreAuthorize rules on the backend decide what actually WORKS;
// hiding the button is just courtesy (see RequireAuth's "UX not security").

import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

export function Layout() {
  const { user, logout, isStaff } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="shell">
      <header className="topbar">
        <Link to="/" className="brand">
          🏦 SecureBank
        </Link>
        <nav className="nav">
          {/* NavLink adds .active automatically — CSS highlights current page */}
          <NavLink to="/">Dashboard</NavLink>
          <NavLink to="/transfer">Transfer</NavLink>
          <NavLink to="/statements">Statements</NavLink>
          <NavLink to="/loans">{isStaff ? 'Loan Queue' : 'Loans'}</NavLink>
        </nav>
        <div className="session">
          {user && (
            <>
              <span className="who">
                {user.username} <em>({user.role.toLowerCase()})</em>
              </span>
              <button className="secondary" onClick={handleLogout}>
                Sign out
              </button>
            </>
          )}
        </div>
      </header>

      <main className="content">
        <Outlet />
      </main>

      <footer className="footer">
        EVAITCS Stage 3 — React 19 + TypeScript + Vite + React Query. API: stage-2
        monolith (or stage-5 gateway) on :8080.
      </footer>
    </div>
  )
}
