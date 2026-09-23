// ============================================================================
// ROUTE GUARD — the frontend's authorization gate.
// ============================================================================
// Wrap protected routes: no session -> redirect to /login (remembering where
// the user wanted to go). This is UX, NOT security: anyone can call the API
// directly, so the REAL gate is the backend's SecurityConfig + JWT
// validation. Frontend guards keep honest users from seeing broken pages;
// backend rules keep everyone else out. (Week 9 OWASP: never trust the client.)

import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from './useAuth'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    // state.from lets LoginPage navigate BACK to the attempted page after
    // a successful login instead of always dumping users on the dashboard.
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  return <>{children}</>
}
