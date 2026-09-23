package com.evaitcs.securebank.payment.saga;

import com.evaitcs.securebank.payment.client.AccountClient;
import com.evaitcs.securebank.payment.client.AccountServiceUnavailableException;
import com.evaitcs.securebank.payment.client.InsufficientFundsException;
import com.evaitcs.securebank.payment.domain.Transfer;
import com.evaitcs.securebank.payment.domain.TransferStatus;
import com.evaitcs.securebank.payment.events.TransferEventPublisher;
import com.evaitcs.securebank.payment.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * ============================================================================
 * THE TRANSFER SAGA — the single most important class in Stage 5.
 * ============================================================================
 *
 * Stage-2 comparison (open both files side by side in the lecture):
 *   Monolith TransferService:  ONE @Transactional method. Lock both rows
 *     (SELECT FOR UPDATE, ascending ids), withdraw, deposit, write 2 ledger
 *     rows, commit. The database guarantees all-or-nothing. ~30 lines.
 *
 *   Saga (here): the source and target balances live in ANOTHER SERVICE.
 *     There is no shared transaction, no shared lock, no rollback. Instead:
 *
 *       1. persist intent          (PENDING — crash here? nothing moved)
 *       2. debit source            (remote, retried, circuit-broken)
 *       3. credit target           (remote, retried, circuit-broken)
 *       4a. success  -> COMPLETED
 *       4b. step 3 failed -> COMPENSATE step 2 (credit the money BACK,
 *           reference "-comp") -> COMPENSATED
 *       4c. compensation ITSELF failed -> COMPENSATION_FAILED -> page a human
 *
 * GUARANTEES, precisely:
 *   - Money is never created or destroyed WITHOUT a recorded trail.
 *   - Consistency is EVENTUAL: between steps 2 and 3 the system is visibly
 *     inconsistent (money "in flight"). That window is the price of the split.
 *   - Every outcome is a persisted state + an event: auditable, replayable.
 *
 * WHY NO @Transactional ON THIS METHOD: it spans network calls. Holding a
 * local DB transaction open while waiting on HTTP pins connections for
 * seconds and deadlocks under load — the classic distributed anti-pattern.
 * Each state change is its own tiny transaction (repository.save); the saga
 * row is the coordination state, not the DB lock.
 *
 * FAILURE WINDOW HONESTY (lecture discussion):
 * If this JVM crashes BETWEEN "debit succeeded remotely" and "we recorded it",
 * the transfer row says PENDING while money moved. Production answers:
 *   - idempotency keys on debit/credit (retries are no-ops) — flagged in
 *     InternalAccountController,
 *   - a reconciliation job scanning stale PENDING rows,
 *   - or the transactional OUTBOX pattern (write event + state in ONE local
 *     tx, a relay ships it) — the standard fix we describe rather than build
 *     here to keep the saga readable.
 * ============================================================================
 */
@Service
public class TransferSagaService {

    private static final Logger log = LoggerFactory.getLogger(TransferSagaService.class);

    private final TransferRepository transfers;
    private final AccountClient accountClient;
    private final TransferEventPublisher.Publisher events;

    public TransferSagaService(TransferRepository transfers,
                               AccountClient accountClient,
                               TransferEventPublisher.Publisher events) {
        this.transfers = transfers;
        this.accountClient = accountClient;
        this.events = events;
    }

    public Transfer transfer(Long fromAccountId, Long toAccountId, BigDecimal amount, Long initiatorCustomerId) {

        // --- Step 0: local validation (no network needed) --------------------
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Source and target accounts must differ");
        }

        // --- Step 1: persist the INTENT ---------------------------------------
        // Save first so the saga row exists (PENDING) BEFORE any remote call:
        // every ledger row and event can be correlated via the reference even
        // if we die mid-flight. The reference is a UUID minted at construction
        // (see Transfer) so this INSERT is immediately valid.
        Transfer transfer = transfers.save(
                new Transfer(fromAccountId, toAccountId, amount, initiatorCustomerId));
        String reference = transfer.getReference();

        // --- Step 2: ownership check + DEBIT ----------------------------------
        try {
            AccountClient.AccountView source = accountClient.view(fromAccountId);
            if (!source.customerId().equals(initiatorCustomerId)) {
                // 404 semantics upward (no existence leak), FAILED state here.
                fail(transfer, "Initiator does not own source account");
                throw new TransferNotFoundException(fromAccountId);
            }
            accountClient.debit(fromAccountId, amount, reference, "Transfer out (saga)");
        } catch (InsufficientFundsException e) {
            // BUSINESS refusal: nothing moved, nothing to undo.
            fail(transfer, e.getMessage());
            events.publishFailed(reference, fromAccountId, toAccountId, amount, e.getMessage());
            throw e;
        } catch (AccountClient.AccountUnknownException e) {
            fail(transfer, e.getMessage());
            throw new TransferNotFoundException(fromAccountId);
        } catch (AccountServiceUnavailableException e) {
            // TECHNICAL failure before any money moved: safe to just fail.
            fail(transfer, "Source debit failed: " + e.getMessage());
            events.publishFailed(reference, fromAccountId, toAccountId, amount, e.getMessage());
            throw e;
        }

        // --- Step 3: CREDIT — point of no easy return -------------------------
        // The debit HAS happened. From here, any failure MUST compensate:
        // leaving money debited-but-not-credited is exactly what a saga exists
        // to prevent.
        try {
            accountClient.credit(toAccountId, amount, reference, "Transfer in (saga)");
        } catch (InsufficientFundsException | AccountClient.AccountUnknownException |
               AccountServiceUnavailableException e) {
            log.error("Credit leg failed for {} ({}); compensating debit", reference, e.getMessage());
            compensate(transfer, reference, e);
            // compensate() has already set the terminal state + published;
            // rethrow so the controller answers 5xx (the transfer did NOT succeed).
            throw new SagaCompensatedException(reference, transfer.getStatus(), e);
        }

        // --- Step 4a: success --------------------------------------------------
        transfer.transitionTo(TransferStatus.COMPLETED, null);
        transfers.save(transfer);
        events.publishCompleted(reference, fromAccountId, toAccountId, amount);
        log.info("Transfer {} completed: {} -> {} amount {}", reference, fromAccountId, toAccountId, amount);
        return transfer;
    }

    /**
     * Step 4b/4c — undo the debit. Note the "-comp" reference suffix: the
     * account-service ledger will show debit xfer-7 and credit xfer-7-comp,
     * a self-documenting audit trail.
     */
    private void compensate(Transfer transfer, String reference, Throwable originalFailure) {
        try {
            accountClient.credit(transfer.getFromAccountId(), transfer.getAmount(),
                    reference + "-comp", "Compensation: transfer credit failed");
            transfer.transitionTo(TransferStatus.COMPENSATED,
                    "Credit failed, debit reversed: " + originalFailure.getMessage());
            transfers.save(transfer);
            events.publishFailed(reference, transfer.getFromAccountId(), transfer.getToAccountId(),
                    transfer.getAmount(), "credit leg failed; source account re-credited");
        } catch (Exception compensationFailure) {
            // THE ALARM STATE. Money is with neither party in a recorded way.
            transfer.transitionTo(TransferStatus.COMPENSATION_FAILED,
                    "Credit failed AND compensation failed: " + compensationFailure.getMessage());
            transfers.save(transfer);
            events.publishFailed(reference, transfer.getFromAccountId(), transfer.getToAccountId(),
                    transfer.getAmount(), "COMPENSATION FAILED — manual reconciliation required");
            log.error("COMPENSATION FAILED for {} — manual intervention required", reference, compensationFailure);
        }
    }

    private void fail(Transfer transfer, String reason) {
        transfer.transitionTo(TransferStatus.FAILED, reason);
        transfers.save(transfer);
    }

    /** Client-facing outcomes the controller maps to HTTP statuses. */
    public static class SagaCompensatedException extends RuntimeException {
        private final TransferStatus outcome;

        public SagaCompensatedException(String reference, TransferStatus outcome, Throwable cause) {
            super("Transfer " + reference + " failed and was rolled back (" + outcome + ")", cause);
            this.outcome = outcome;
        }

        public TransferStatus getOutcome() {
            return outcome;
        }
    }

    public static class TransferNotFoundException extends RuntimeException {
        public TransferNotFoundException(Long accountId) {
            super("Account " + accountId + " not found");
        }
    }
}
