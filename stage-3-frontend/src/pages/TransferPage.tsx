// ============================================================================
// TRANSFER PAGE — the write-path counterpart of the dashboard.
// ============================================================================
// useMutation = React Query's write operation: mutationFn fires the POST,
// onSuccess invalidates cached queries so every screen shows fresh balances.
//
// THE INVALIDATION CONTRACT (the most important habit in this file):
// after a successful transfer we invalidate ['accounts'] AND ['statement'].
// Forget this and the UI lies — the transfer happened but the dashboard
// still shows stale numbers. Server-cache consistency is the developer's job.
//
// ERROR UX: backend business failures (insufficient funds = 400 problem+json)
// render title+detail inline — the user sees "Insufficient funds — Account 1
// has 300.00 available but 9999.00 was requested", straight from
// InsufficientFundsException via GlobalExceptionHandler.

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import { fetchAccounts, transfer } from '../api/endpoints'
import { formatMoney } from '../components/format'

export function TransferPage() {
  const queryClient = useQueryClient()
  const accounts = useQuery({ queryKey: ['accounts'], queryFn: fetchAccounts })

  const [fromId, setFromId] = useState<string>('')
  const [toId, setToId] = useState<string>('')
  const [amount, setAmount] = useState<string>('')
  const [receipt, setReceipt] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () => transfer(Number(fromId), Number(toId), Number(amount)),
    onSuccess: (result) => {
      setReceipt(result.reference)
      setError(null)
      // Balances changed on the server — make every cached view re-fetch.
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      queryClient.invalidateQueries({ queryKey: ['statement'] })
    },
    onError: (e) => {
      setReceipt(null)
      setError(e instanceof ApiError ? `${e.title}${e.detail ? ` — ${e.detail}` : ''}` : 'Transfer failed')
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    mutation.mutate()
  }

  const list = accounts.data ?? []
  const from = list.find((a) => a.id === Number(fromId))

  return (
    <div className="stack narrow">
      <h1>Transfer money</h1>

      {accounts.isLoading && <p>Loading accounts…</p>}
      {list.length < 2 && !accounts.isLoading && (
        <p className="muted">
          You need two accounts to transfer between them (the demo seeds a
          Checking and a Savings account — refresh the backend if missing).
        </p>
      )}

      <form onSubmit={handleSubmit} className="stack card">
        <label>
          From
          <select value={fromId} onChange={(e) => setFromId(e.target.value)} required>
            <option value="" disabled>Select source account…</option>
            {list.map((a) => (
              <option key={a.id} value={a.id}>
                {a.type} #{a.id} — {formatMoney(a.balance)}
              </option>
            ))}
          </select>
        </label>

        <label>
          To
          <select value={toId} onChange={(e) => setToId(e.target.value)} required>
            <option value="" disabled>Select target account…</option>
            {/* Any account id is allowed as target (that's the product), but
                the SOURCE must be yours — enforced server-side, not here. */}
            {list.map((a) => (
              <option key={a.id} value={a.id}>
                {a.type} #{a.id} — {formatMoney(a.balance)}
              </option>
            ))}
          </select>
        </label>

        <label>
          Amount (USD)
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder="0.00"
            required
          />
        </label>

        {from && amount && Number(amount) > from.balance && (
          <div className="warn">
            Heads-up: {formatMoney(Number(amount))} exceeds the available{' '}
            {formatMoney(from.balance)} — the server will refuse this (and it
            would refuse it even without this warning; the check is courtesy).
          </div>
        )}

        {error && <div className="alert">{error}</div>}
        {receipt && (
          <div className="success">
            Transfer complete — reference <code>{receipt}</code>. Give this id to
            support to find both ledger halves.
          </div>
        )}

        <button type="submit" disabled={mutation.isPending || list.length < 2}>
          {mutation.isPending ? 'Transferring…' : 'Send transfer'}
        </button>
      </form>
    </div>
  )
}
