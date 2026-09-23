// ============================================================================
// API CLIENT — every HTTP call in the app funnels through this file.
// ============================================================================
// WHY A WRAPPER around fetch() instead of calling fetch everywhere?
//   1. ONE place attaches "Authorization: Bearer <token>" (Week 1: HTTP is
//      stateless — the server remembers nothing, so EVERY request must carry
//      its identity).
//   2. ONE place parses errors. The backend answers failures with RFC 7807
//      problem+json (GlobalExceptionHandler); we convert every non-2xx into a
//      typed ApiError so pages just catch and display `title`/`detail`.
//   3. ONE place handles 401: token expired/revoked -> force logout, which
//      sends the user back to the login page instead of showing broken data.
//
// The relative baseUrl ("/api") works because vite.config.ts proxies /api to
// the backend in dev, and nginx does the same in Stage 4 containers.

import type { ProblemDetail } from './types'

/** A parsed backend error — pages render title/detail directly.
 *  (Written without TS "parameter properties" — tsconfig sets
 *  erasableSyntaxOnly, which forbids syntax that needs runtime codegen.
 *  Modern TS style: declare fields, assign in the body.) */
export class ApiError extends Error {
  readonly status: number
  readonly title: string
  readonly detail?: string
  readonly problem?: ProblemDetail

  constructor(status: number, title: string, detail?: string, problem?: ProblemDetail) {
    super(detail ?? title)
    this.status = status
    this.title = title
    this.detail = detail
    this.problem = problem
  }
}

/** Where the auth token lives between reloads.
 *  localStorage vs cookies trade-off (Week 6 security lesson):
 *   - localStorage: JS-readable => XSS could steal it, but immune to CSRF.
 *   - httpOnly cookie: JS can't read it (XSS-safe) but needs CSRF tokens.
 *  We use localStorage + short-lived tokens (1h) + Bearer headers (no
 *  auto-send), the common SPA choice — and the reason the backend disables
 *  CSRF (see SecurityConfig comments). */
const TOKEN_KEY = 'securebank.token'
const SESSION_KEY = 'securebank.session'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setSession(token: string, session: unknown): void {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function getStoredSession<T>(): T | null {
  const raw = localStorage.getItem(SESSION_KEY)
  return raw ? (JSON.parse(raw) as T) : null
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(SESSION_KEY)
}

/** Called by the auth layer when a 401 means "session over" (expired token). */
let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(handler: () => void): void {
  onUnauthorized = handler
}

type Json = Record<string, unknown> | unknown[] | null

/** The one true fetch: JSON in, typed JSON out, ApiError on failure. */
export async function api<T>(
  path: string,
  options: { method?: string; body?: Json } = {},
): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}` // stateless identity, every request

  const response = await fetch(`/api${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })

  if (!response.ok) {
    let problem: ProblemDetail | undefined
    try {
      problem = (await response.json()) as ProblemDetail
    } catch {
      /* body wasn't JSON (e.g. proxy error page) — fall through with defaults */
    }
    if (response.status === 401 && getToken()) {
      onUnauthorized() // expired/invalid token mid-session -> back to login
    }
    throw new ApiError(
      response.status,
      problem?.title ?? `Request failed (${response.status})`,
      problem?.detail,
      problem,
    )
  }

  // 204 No Content -> nothing to parse
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}
