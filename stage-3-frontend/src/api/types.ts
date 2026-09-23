// ============================================================================
// API TYPES — the TypeScript mirror of the backend's DTO records.
// ============================================================================
// THE CONTRACT: stage-2's web/dto/*.java files define the JSON shapes; this
// file re-declares them as TS types. TypeScript erases at runtime (types are
// compile-time only), so this is a MANUAL mirror — if a DTO changes, update
// both sides. (Week 7 discusses how OpenAPI codegen can automate exactly this;
// the springdoc endpoint /v3/api-docs is the machine-readable contract.)
//
// WHY IT PAYS OFF: `account.balanse` is a compile error here, and your editor
// autocompletes every field. That's the "TypeScript = types + JavaScript"
// lesson from Week 2 doing real work.

export type AccountType = 'CHECKING' | 'SAVINGS'
export type Role = 'CUSTOMER' | 'TELLER' | 'ADMIN'
export type LoanStatus = 'APPLIED' | 'APPROVED' | 'REJECTED' | 'PAID_OFF'
export type TransactionType = 'DEBIT' | 'CREDIT'

/** Mirrors web/dto/LoginResponse.java */
export interface LoginResponse {
  token: string
  tokenType: string
  expiresAt: string
  username: string
  role: Role
  customerId: number | null // null for staff (teller/admin)
}

/** Mirrors web/dto/AccountResponse.java — BigDecimal arrives as a JSON number.
 *  (Safe for display; the BACKEND keeps exact BigDecimal arithmetic — never do
 *  money math in float-land here beyond formatting. Sum totals come from SQL.) */
export interface Account {
  id: number
  type: AccountType
  balance: number
  customerId: number
}

/** Mirrors web/dto/CustomerResponse.java */
export interface Customer {
  id: number
  name: string
  email: string
}

/** Mirrors web/dto/TransactionResponse.java */
export interface Transaction {
  id: number
  type: TransactionType
  amount: number
  description: string | null
  reference: string | null
  occurredAt: string
}

/** Mirrors web/dto/TransferResponse.java */
export interface TransferResponse {
  reference: string
  fromAccountId: number
  toAccountId: number
  amount: number
  occurredAt: string
}

/** Mirrors web/dto/LoanResponse.java */
export interface Loan {
  id: number
  principal: number
  annualRate: number
  termMonths: number
  status: LoanStatus
  monthlyPayment: number | null // null until APPROVED
  appliedAt: string
}

/** Mirrors web/dto/PageDto.java — our stable pagination envelope. */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** Mirrors RFC 7807 problem+json produced by GlobalExceptionHandler.java.
 *  One error SHAPE for every failure = uniform error UI (see ApiError). */
export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  [extension: string]: unknown // ProblemDetail "extension members" (accountId, available...)
}
