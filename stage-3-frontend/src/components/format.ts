// ============================================================================
// MONEY FORMATTING — one place turns numbers into currency strings.
// ============================================================================
// Intl.NumberFormat is built into every browser: locale-aware, correct
// rounding, no library needed. Centralizing it means the day the bank adds
// EUR or changes decimal rules, ONE file changes.
//
// WHY NOT do arithmetic here? Display only. Remember the contract: the
// backend owns exact BigDecimal math; JS numbers are IEEE doubles and must
// never be used to COMPUTE money (0.1 + 0.2 !== 0.3). We only render totals
// the backend calculated... with one deliberate exception: the dashboard's
// cross-account sum (see DashboardPage for the trade-off discussion).

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

export function formatMoney(amount: number): string {
  return usd.format(amount)
}

/** Locale-aware timestamp rendering for ISO-8601 strings from the API. */
export function formatWhen(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
