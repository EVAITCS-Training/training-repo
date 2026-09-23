// ============================================================================
// useAuth — the hook pages use to read/drive the session.
// ============================================================================
// Throws when used outside <AuthProvider>: failing fast with a clear message
// beats silently rendering "null user" screens and debugging for an hour.

import { useContext } from 'react'
import { AuthContext, type AuthState } from './AuthContext'

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
