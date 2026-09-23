// ============================================================================
// LOGIN PAGE — where a session begins.
// ============================================================================
// CONTROLLED FORM (Week 2 React core): inputs are React state; every
// keystroke flows state -> value, every change flows event -> setState.
// The form is a MIRROR of the backend's LoginRequest DTO (username/password)
// and its validation errors arrive as RFC 7807 problem+json which we render
// via ApiError.title/detail.
//
// Demo credentials (seeded): alice/password123 (customer) · teller/password123 (staff)

import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/useAuth'

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  // RequireAuth stashed the attempted URL here — honor it after login.
  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/'

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={from} replace /> // already logged in

  async function handleSubmit(event: FormEvent) {
    event.preventDefault() // stop the browser's native form POST + reload
    setBusy(true)
    setError(null)
    try {
      await login(username, password)
      navigate(from, { replace: true })
    } catch (e) {
      // Uniform error UX: every failure path shows title/detail from the API.
      // Note the backend answers BOTH unknown-user and wrong-password with
      // the same 401 "Invalid credentials" (no enumeration oracle).
      setError(e instanceof ApiError ? `${e.title}${e.detail ? ` — ${e.detail}` : ''}` : 'Login failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="center-card">
      <h1>Sign in</h1>
      <p className="muted">SecureBank demo — try alice/password123 or teller/password123</p>
      <form onSubmit={handleSubmit} className="stack">
        <label>
          Username
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            required
            minLength={3}
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
            minLength={8}
          />
        </label>
        {error && <div className="alert">{error}</div>}
        <button type="submit" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </div>
  )
}
