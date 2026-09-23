// ============================================================================
// API ENDPOINTS — one typed function per backend route.
// ============================================================================
// Each function mirrors a controller method in stage-2 (comments point at the
// Java file). Pages NEVER call fetch/api directly — they call these, so the
// whole HTTP surface of the app is inventoried in one file. When Stage 5 moves
// the backend behind the API gateway, ONLY the proxy target changes; this file
// is untouched. That is what "the frontend talks to a contract, not to an
// implementation" means in practice.

import { api } from './client'
import type {
  Account,
  Customer,
  Loan,
  LoginResponse,
  Page,
  Transaction,
  TransferResponse,
} from './types'

/** POST /api/auth/login — AuthController.java. Username+password -> JWT. */
export function login(username: string, password: string): Promise<LoginResponse> {
  return api<LoginResponse>('/auth/login', {
    method: 'POST',
    body: { username, password },
  })
}

/** GET /api/customers/me — CustomerController.java (identity from the JWT). */
export function fetchMe(): Promise<Customer> {
  return api<Customer>('/customers/me')
}

/** GET /api/accounts — AccountController.myAccounts. */
export function fetchAccounts(): Promise<Account[]> {
  return api<Account[]>('/accounts')
}

/** POST /api/accounts — open a new account for the logged-in customer. */
export function openAccount(type: 'CHECKING' | 'SAVINGS'): Promise<Account> {
  return api<Account>('/accounts', { method: 'POST', body: { type } })
}

/** POST /api/transfers — TransferController; returns the correlation reference. */
export function transfer(fromAccountId: number, toAccountId: number, amount: number): Promise<TransferResponse> {
  return api<TransferResponse>('/transfers', {
    method: 'POST',
    body: { fromAccountId, toAccountId, amount },
  })
}

/** GET /api/accounts/{id}/transactions?page=&size= — paged statement. */
export function fetchStatement(accountId: number, page: number, size = 10): Promise<Page<Transaction>> {
  return api<Page<Transaction>>(`/accounts/${accountId}/transactions?page=${page}&size=${size}`)
}

/** GET /api/loans — customer: mine; staff: pending queue (LoanController). */
export function fetchLoans(): Promise<Loan[]> {
  return api<Loan[]>('/loans')
}

/** POST /api/loans — apply (CUSTOMER role only). Rate is bank-side. */
export function applyForLoan(principal: number, termMonths: number): Promise<Loan> {
  return api<Loan>('/loans', { method: 'POST', body: { principal, termMonths } })
}

/** POST /api/loans/{id}/approve — TELLER/ADMIN only (@PreAuthorize). */
export function approveLoan(id: number): Promise<Loan> {
  return api<Loan>(`/loans/${id}/approve`, { method: 'POST' })
}

/** POST /api/loans/{id}/reject — TELLER/ADMIN only. */
export function rejectLoan(id: number): Promise<Loan> {
  return api<Loan>(`/loans/${id}/reject`, { method: 'POST' })
}
