package com.evaitcs.securebank.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * LOAN SERVICE — the Lending bounded context.
 * ============================================================================
 *
 * WHY LOANS SPLIT SO EASILY: even in stage-2, LoanService touched only loan
 * data (+ one customer lookup). A bounded context that already behaves like
 * this extracts almost verbatim — which is the DDD argument in practice:
 * decompose along BUSINESS capabilities and the code follows; decompose along
 * technical layers ("database-service", "web-service") and you get a
 * distributed ball of mud.
 *
 * THE ONE CROSS-CONTEXT COMPROMISE: a loan references its customer BY ID
 * (Long customerId) — the Customer entity lives in account-service now.
 * Consequences we accept (and discuss in class):
 *   - no FK integrity: a loan could reference a deleted customer; production
 *     systems reconcile via events (customer-deleted -> flag loans).
 *   - no JOIN for "loans with customer names": either an API call per loan
 *     (N+1 over HTTP!) or a denormalized copy on the loan row. We return
 *     customerId and let callers resolve it if they care.
 *
 * EVENTS OUT: LoanApproved / LoanRejected go to the "loans" Kafka topic —
 * notification-service turns them into customer messages without this
 * service knowing it exists (publish-subscribe decoupling, Week 7).
 * ============================================================================
 */
@SpringBootApplication
public class LoanServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanServiceApplication.class, args);
    }
}
