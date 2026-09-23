// ============================================================================
// AUTH CONTEXT — the shared "session" type + context object ONLY.
// ============================================================================
// Three-file split (context / provider / hook) instead of one file exporting
// both a component and a hook: React Fast Refresh can only hot-reload files
// that export components, and mixing exports degrades the dev experience
// (oxlint's only-export-components rule enforces this). The split also reads
// well: types live with the context, logic with the provider, access with
// the hook.

import { createContext } from 'react'
import type { LoginResponse } from '../api/types'

export interface AuthState {
  /** The verified-by-server session profile, or null when logged out. */
  user: LoginResponse | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  isStaff: boolean
}

/** null default + the throwing useAuth() hook = compile-safe context access. */
export const AuthContext = createContext<AuthState | null>(null)
