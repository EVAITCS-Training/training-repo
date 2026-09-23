// ============================================================================
// STATEMENTS PAGE — server-side pagination against the ledger.
// ============================================================================
// The page number lives in the QUERY KEY (['statement', accountId, page]),
// so React Query treats each page as its own cached resource: back/forward
// through pages is instant (cached) and correct (per-page fetch). This is the
// canonical React Query pagination pattern.
//
// DERIVED STATE, NOT EFFECTS (Week 8 React quality lesson):
// Which account is selected is COMPUTED during render:
//   user's explicit choice  >  ?account= URL param  >  first account loaded
// The only state we store is that explicit choice (plus its page, stored
// TOGETHER so switching accounts can't strand you on a stale page number).
// A first draft of this page used useEffect(setAccountId) + useEffect(setPage 0)
// — oxlint flagged set-state-in-effect, and rightly so: effects that mirror
// state cause cascading renders and flicker. "Derive during render" is the
// modern React rule of thumb; useEffect is for EXTERNAL systems only
// (subscriptions, the 401-handler wiring in AuthProvider, ...).

import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { fetchAccounts, fetchStatement } from '../api/endpoints'
import { formatMoney, formatWhen } from '../components/format'

const PAGE_SIZE = 10

/** The user's explicit selection; null = "follow the defaults". */
interface Selection {
  accountId: string
  page: number
}

export function StatementsPage() {
  const [searchParams] = useSearchParams()
  const accounts = useQuery({ queryKey: ['accounts'], queryFn: fetchAccounts })
  const [selection, setSelection] = useState<Selection | null>(null)

  // --- derived during render: no effects, no stale intermediate frames -----
  const accountId =
    selection?.accountId ??
    searchParams.get('account') ??                       // dashboard links here with ?account=
    (accounts.data?.length ? String(accounts.data[0].id) : '')
  // Page belongs to its account: if the effective account differs from the
  // stored selection (e.g. defaults kicked in), we are on page 0 by definition.
  const page = selection?.accountId === accountId ? selection.page : 0

  const statement = useQuery({
    queryKey: ['statement', accountId, page],
    queryFn: () => fetchStatement(Number(accountId), page, PAGE_SIZE),
    enabled: accountId !== '',
  })

  return (
    <div className="stack">
      <h1>Statements</h1>

      <label className="inline">
        Account
        <select
          value={accountId}
          onChange={(e) => setSelection({ accountId: e.target.value, page: 0 })}
        >
          <option value="" disabled>Select…</option>
          {(accounts.data ?? []).map((a) => (
            <option key={a.id} value={a.id}>
              {a.type} #{a.id}
            </option>
          ))}
        </select>
      </label>

      {statement.isLoading && <p>Loading…</p>}
      {statement.isError && <div className="alert">{statement.error.message}</div>}

      {statement.data && (
        <>
          <table>
            <thead>
              <tr>
                <th>When</th><th>Description</th><th>Type</th>
                <th>Reference</th><th className="right">Amount</th>
              </tr>
            </thead>
            <tbody>
              {statement.data.content.map((t) => (
                <tr key={t.id}>
                  <td>{formatWhen(t.occurredAt)}</td>
                  <td>{t.description ?? '—'}</td>
                  <td>{t.type}</td>
                  {/* reference links the two halves of a transfer — show it:
                      it's the reconciliation key from stage-1's self-join demo */}
                  <td>{t.reference ? <code title={t.reference}>{t.reference.slice(0, 8)}…</code> : '—'}</td>
                  <td className={`right ${t.type === 'DEBIT' ? 'neg' : 'pos'}`}>
                    {t.type === 'DEBIT' ? '−' : '+'}{formatMoney(t.amount)}
                  </td>
                </tr>
              ))}
              {statement.data.content.length === 0 && (
                <tr><td colSpan={5} className="muted">No transactions yet.</td></tr>
              )}
            </tbody>
          </table>

          <div className="pager">
            <button
              className="secondary"
              disabled={page === 0}
              onClick={() => setSelection({ accountId, page: page - 1 })}
            >
              ← Newer
            </button>
            <span className="muted">
              Page {statement.data.page + 1} of {Math.max(statement.data.totalPages, 1)} ·{' '}
              {statement.data.totalElements} total
            </span>
            <button
              className="secondary"
              disabled={statement.data.page + 1 >= statement.data.totalPages}
              onClick={() => setSelection({ accountId, page: page + 1 })}
            >
              Older →
            </button>
          </div>
        </>
      )}
    </div>
  )
}
