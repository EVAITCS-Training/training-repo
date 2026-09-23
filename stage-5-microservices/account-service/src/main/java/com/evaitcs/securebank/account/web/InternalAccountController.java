package com.evaitcs.securebank.account.web;

import com.evaitcs.securebank.account.service.AccountService;
import com.evaitcs.securebank.account.web.Dtos.AccountView;
import com.evaitcs.securebank.account.web.Dtos.MoneyMovementRequest;
import com.evaitcs.securebank.account.web.Dtos.MoneyMovementResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * INTERNAL API — the saga primitives payment-service drives.
 *
 * REACHABILITY (the security model in one picture):
 *
 *   internet -> nginx -> gateway :8080
 *                         │  routes: /api/auth, /api/accounts, /api/transfers, ...
 *                         │  NO route matches /internal/**  -> 404 at the door
 *                         ▼
 *                 account-service :8082
 *                         ▲
 *   payment-service ──────┘  lb://account-service/internal/accounts/{id}/debit
 *   (inside the compose/k8s network; discovered via Eureka)
 *
 * So: publicly unreachable BY ABSENCE OF A ROUTE, reachable internally by
 * discovery name. In Kubernetes you'd add a NetworkPolicy so only
 * payment-service pods can open this port at all, and mTLS if you need
 * cryptographic caller identity (see infra/k8s + AccountService comments).
 *
 * IDEMPOTENCY NOTE (lecture topic): if payment-service retries a timed-out
 * debit, the same `reference` arrives twice. A production implementation
 * would check "have I seen this reference?" and no-op (the transactions
 * table already indexes it). We surface the issue in comments rather than
 * silently ignore it — retries+duplicates are THE classic distributed bug.
 */
@RestController
@RequestMapping("/internal/accounts")
public class InternalAccountController {

    private final AccountService accountService;

    public InternalAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** Locked read for the saga's ownership + balance checks. */
    @GetMapping("/{id}")
    public AccountView view(@PathVariable Long id) {
        return AccountView.from(accountService.view(id));
    }

    /** Debit primitive — 400 problem+json on insufficient funds. */
    @PostMapping("/{id}/debit")
    public MoneyMovementResponse debit(@PathVariable Long id,
                                       @Valid @RequestBody MoneyMovementRequest request) {
        return MoneyMovementResponse.from(accountService.debit(
                id, request.amount(), request.reference(), request.description()));
    }

    /** Credit primitive — also the COMPENSATION path (reference ends "-comp"). */
    @PostMapping("/{id}/credit")
    public MoneyMovementResponse credit(@PathVariable Long id,
                                        @Valid @RequestBody MoneyMovementRequest request) {
        return MoneyMovementResponse.from(accountService.credit(
                id, request.amount(), request.reference(), request.description()));
    }
}
