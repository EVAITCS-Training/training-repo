// ============================================================================
// AUTH PROVIDER — session lifecycle logic.
// ============================================================================
// THE SESSION LIFECYCLE, end to end:
//   login()  -> POST /api/auth/login -> JWT + profile claims
//            -> token & session saved to localStorage (survives F5)
//   requests -> api/client.ts attaches "Authorization: Bearer <token>"
//   401      -> client.ts fires the unauthorized handler -> logout() here
//   logout() -> clear storage, router guard bounces to /login
//
// NOTE what we DON'T have: no refresh loop, no session polling. The token
// lives 1 hour (backend app.jwt.ttl); when it expires the next API call 401s
// and we re-login. Real apps add refresh tokens — a Week 6 discussion topic.

import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { clearSession, getStoredSession, setSession, setUnauthorizedHandler } from '../api/client'
import { login as loginCall } from '../api/endpoints'
import type { LoginResponse } from '../api/types'
import { AuthContext, type AuthState } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  // Initialize from localStorage so a page refresh keeps the session.
  // (Lazy initializer function — runs ONCE, not on every render.)
  const [user, setUser] = useState<LoginResponse | null>(() => getStoredSession<LoginResponse>())

  const logout = useCallback(() => {
    clearSession()
    setUser(null)
  }, [])

  // Wire the API client's 401 escape hatch to our logout (module-level
  // callback registered once — the client can't import React state directly).
  useEffect(() => {
    setUnauthorizedHandler(logout)
  }, [logout])

  const login = useCallback(async (username: string, password: string) => {
    const result = await loginCall(username, password)
    setSession(result.token, result)
    setUser(result)
  }, [])

  // useMemo keeps the context value referentially stable — without it, every
  // provider render creates a new object and EVERY consumer re-renders.
  const value = useMemo<AuthState>(
    () => ({
      user,
      login,
      logout,
      isStaff: user?.role === 'TELLER' || user?.role === 'ADMIN',
    }),
    [user, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
