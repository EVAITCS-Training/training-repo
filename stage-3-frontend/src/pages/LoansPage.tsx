// ============================================================================
// LOANS PAGE — one screen, two audiences (role-based UI).
// ============================================================================
// CUSTOMER view: apply for a loan + see own applications.
// STAFF view:    the pending approval queue with approve/reject buttons.
//
// Same URL /loans for both — the backend (LoanController.list) already returns
// "mine" for customers and "pending" for staff based on the JWT role, so the
// page just renders what arrives and toggles the ACTION buttons by role.
// The buttons are hidden for customers purely as UX: if a customer crafted the
// POST /api/loans/1/approve call by hand, @PreAuthorize answers 403.
// Hiding UI is courtesy; server-side authorization is the security.

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { approveLoan, applyForLoan, fetchLoans, rejectLoan } from '../api/endpoints'
import type { Loan } from '../api/types'
import { formatMoney, formatWhen } from '../components/format'
import { useAuth } from '../auth/useAuth'

export function LoansPage() {
  const { isStaff } = useAuth()
  const queryClient = useQueryClient()
  const loans = useQuery({ queryKey: ['loans'], queryFn: fetchLoans })

  const [principal, setPrincipal] = useState('')
  const [termMonths, setTermMonths] = useState('36')
  const [error, setError] = useState<string | null>(null)
  const [applied, setApplied] = useState(false)

  // One invalidation strategy for ALL mutations on this page: after any
  // change, refetch ['loans'] so queue/status/badges update together.
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['loans'] })

  const apply = useMutation({
    mutationFn: () => applyForLoan(Number(principal), Number(termMonths)),
    onSuccess: () => { setApplied(true); setError(null); invalidate() },
    onError: (e) => setError(e instanceof ApiError ? `${e.title} — ${e.detail ?? ''}` : 'Application failed'),
  })

  const approve = useMutation({ mutationFn: approveLoan, onSuccess: invalidate,
    onError: (e) => setError(e instanceof ApiError ? e.title : 'Approve failed') })
  const reject = useMutation({ mutationFn: rejectLoan, onSuccess: invalidate,
    onError: (e) => setError(e instanceof ApiError ? e.title : 'Reject failed') })

  function handleApply(event: FormEvent) {
    event.preventDefault()
    setApplied(false)
    apply.mutate()
  }

  return (
    <div className="stack">
      <h1>{isStaff ? 'Loan approval queue' : 'Loans'}</h1>
      {error && <div className="alert">{error}</div>}

      {/* ---------- CUSTOMER: application form ---------- */}
      {!isStaff && (
        <form onSubmit={handleApply} className="card stack">
          <h2>Apply for a loan</h2>
          <p className="muted">
            Standard rate 7.50% APR (bank-side pricing — the server ignores any
            rate a client might send; see LoanApplicationRequest.java).
          </p>
          <div className="row">
            <label>
              Principal (USD)
              <input type="number" min="500" step="0.01" value={principal}
                     onChange={(e) => setPrincipal(e.target.value)} placeholder="10000" required />
            </label>
            <label>
              Term (months)
              <input type="number" min="6" max="360" value={termMonths}
                     onChange={(e) => setTermMonths(e.target.value)} required />
            </label>
          </div>
          <button type="submit" disabled={apply.isPending}>
            {apply.isPending ? 'Applying…' : 'Submit application'}
          </button>
          {applied && <div className="success">Application submitted — a teller will review it.</div>}
        </form>
      )}

      {/* ---------- list / queue ---------- */}
      {loans.isLoading && <p>Loading…</p>}
      {loans.data && loans.data.length === 0 && (
        <p className="muted">{isStaff ? 'Queue is empty.' : 'No loan applications yet.'}</p>
      )}
      {loans.data && loans.data.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>#</th><th>Principal</th><th>Rate</th><th>Term</th>
              <th>Status</th><th className="right">Monthly</th><th>Applied</th>
              {isStaff && <th>Decision</th>}
            </tr>
          </thead>
          <tbody>
            {loans.data.map((loan: Loan) => (
              <tr key={loan.id}>
                <td>{loan.id}</td>
                <td>{formatMoney(loan.principal)}</td>
                <td>{loan.annualRate.toFixed(2)}%</td>
                <td>{loan.termMonths} mo</td>
                <td><span className={`badge ${loan.status.toLowerCase()}`}>{loan.status}</span></td>
                <td className="right">
                  {loan.monthlyPayment !== null ? formatMoney(loan.monthlyPayment) : '—'}
                </td>
                <td>{formatWhen(loan.appliedAt)}</td>
                {isStaff && (
                  <td>
                    {loan.status === 'APPLIED' ? (
                      <span className="row">
                        <button className="small" onClick={() => approve.mutate(loan.id)}
                                disabled={approve.isPending}>Approve</button>
                        <button className="small secondary" onClick={() => reject.mutate(loan.id)}
                                disabled={reject.isPending}>Reject</button>
                      </span>
                    ) : (
                      <span className="muted">decided</span>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
