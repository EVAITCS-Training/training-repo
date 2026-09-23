package com.evaitcs.securebank.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * ACCOUNT SERVICE — the Accounts bounded context (customers, accounts, ledger).
 * ============================================================================
 *
 * WHAT MOVED HERE from stage-2: Customer/Account/Transaction entities, their
 * repositories, AccountService + StatementService, the accounts/statements
 * controllers, and the account-related seed data. The DOMAIN LOGIC IS
 * UNCHANGED — same invariants (no overdraft), same locking (FOR UPDATE),
 * same audit discipline (every movement writes a ledger row). Decomposition
 * relocated code; it did not rewrite it.
 *
 * OWNED DATA: accountdb — customer, account, transactions tables. NO other
 * service may touch them (database per service). auth-service stores a
 * customer_id VALUE; loan-service stores a customer_id VALUE; neither can
 * JOIN to this database, by construction.
 *
 * TWO API SURFACES (the key new concept in this service):
 *   PUBLIC  /accounts/**, /customers/**   — via the gateway, JWT-protected,
 *                                           called by the frontend.
 *   INTERNAL /internal/accounts/**        — NO gateway route exists for these.
 *                                           payment-service calls them
 *                                           service-to-service (discovery +
 *                                           Docker/K8s network), to debit and
 *                                           credit during the transfer saga.
 *                                           The perimeter IS the routing table:
 *                                           what the gateway doesn't forward,
 *                                           the outside world cannot reach.
 * ============================================================================
 */
@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
