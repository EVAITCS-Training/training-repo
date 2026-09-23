// ============================================================================
// DASHBOARD — the screen the study guide's "Project Kick-In" builds:
// total balance, account list, recent activity.
// ============================================================================
// REACT QUERY (Week 8: State Management) does the server-state heavy lifting:
//   useQuery(['accounts']) -> GET /api/accounts, cached by key, with
//   loading/error states, background refetch, and deduplication.
// Compare with raw useEffect+fetch (shown in the comment at the bottom):
// ~20 lines of race-prone boilerplate disappear.
//
// CACHE KEY DISCIPLINE: ['accounts'] is the identity of this data. When a
// transfer mutation succeeds we call invalidateQueries(['accounts']) and every
// component using that key re-renders with fresh data — that's "the cache as
// the single source of truth" pattern.

import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { fetchAccounts, fetchMe, fetchStatement } from '../api/endpoints'
import type { Transaction } from '../api/types'
import { formatMoney, formatWhen } from '../components/format'
import { useAuth } from '../auth/useAuth'

export function DashboardPage() {
  const { user } = useAuth()
  const isStaff = user?.role === 'TELLER' || user?.role === 'ADMIN'

  // Three parallel queries; React Query fires them together, not serially.
  const me = useQuery({ queryKey: ['me'], queryFn: fetchMe, enabled: !isStaff })
  const accounts = useQuery({ queryKey: ['accounts'], queryFn: fetchAccounts, enabled: !isStaff })

  // "Recent activity" = page 0 of the first account's statement (a customer-
  // scoped endpoint). Staff skip it — they have no accounts of their own.
  const firstAccountId = accounts.data?.[0]?.id
  const recent = useQuery({
    queryKey: ['statement', firstAccountId, 0],
    queryFn: () => fetchStatement(firstAccountId!, 0, 5),
    enabled: firstAccountId !== undefined,
  })

  if (isStaff) {
    return (
      <div className="stack">
        <h1>Staff dashboard</h1>
        <p className="muted">
          Signed in as {user?.role.toLowerCase()}. You have no customer accounts —
          head to the <Link to="/loans">loan queue</Link> to process applications.
        </p>
      </div>
    )
  }

  if (accounts.isLoading || me.isLoading) return <p>Loading…</p>
  if (accounts.isError) return <div className="alert">Failed to load accounts: {accounts.error.message}</div>

  // THE ONE MONEY COMPUTATION ON THE FRONTEND: summing displayed balances.
  // Double addition of 2-decimal values is exact enough for a 2-account demo,
  // but production would fetch a server-computed total (SQL SUM over DECIMAL).
  // Knowing WHEN float math is acceptable is the lesson.
  const total = (accounts.data ?? []).reduce((sum, a) => sum + a.balance, 0)

  return (
    <div className="stack">
      <section className="hero">
        <div>
          <h1>Welcome, {me.data?.name ?? user?.username}</h1>
          <p className="total">Total balance {formatMoney(total)}</p>
        </div>
        <Link className="cta" to="/transfer">Make a transfer</Link>
      </section>

      <section>
        <h2>Accounts</h2>
        <div className="cards">
          {(accounts.data ?? []).map((a) => (
            // key={a.id}: React reconciles lists by identity — see Week 2
            // "rendering lists with keys" (index-as-key is the classic bug).
            <div className="card" key={a.id}>
              <div className="card-title">
                {a.type === 'CHECKING' ? 'Checking' : 'Savings'} <span className="muted">#{a.id}</span>
              </div>
              <div className="card-value">{formatMoney(a.balance)}</div>
              <Link to={`/statements?account=${a.id}`}>Statement →</Link>
            </div>
          ))}
        </div>
      </section>

      <section>
        <h2>Recent activity</h2>
        {recent.isLoading && <p>Loading…</p>}
        {recent.data && (
          <table>
            <thead>
              <tr><th>When</th><th>Description</th><th>Type</th><th className="right">Amount</th></tr>
            </thead>
            <tbody>
              {recent.data.content.map((t: Transaction) => (
                <tr key={t.id}>
                  <td>{formatWhen(t.occurredAt)}</td>
                  <td>{t.description ?? '—'}</td>
                  <td>{t.type}</td>
                  <td className={`right ${t.type === 'DEBIT' ? 'neg' : 'pos'}`}>
                    {t.type === 'DEBIT' ? '−' : '+'}{formatMoney(t.amount)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  )
}

// RAW-FETCH COMPARISON (what React Query replaces) — keep for the lecture:
//
//   const [accounts, setAccounts] = useState<Account[] | null>(null)
//   const [loading, setLoading] = useState(true)
//   const [error, setError] = useState<string | null>(null)
//   useEffect(() => {
//     let cancelled = false                       // <- the race guard everyone forgets
//     fetchAccounts()
//       .then((data) => { if (!cancelled) setAccounts(data) })
//       .catch((e) => { if (!cancelled) setError(e.message) })
//       .finally(() => { if (!cancelled) setLoading(false) })
//     return () => { cancelled = true }
//   }, [])
//   ...plus manual refetch after every mutation, in every component. No.
