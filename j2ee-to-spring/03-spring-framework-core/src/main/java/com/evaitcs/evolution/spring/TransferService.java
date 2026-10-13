package com.evaitcs.evolution.spring;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The business rule — and ONLY the business rule. (README section
 * "@Transactional: the proxy owns the transaction".)
 *
 * Put this next to module 01's JdbcTransferDao.transfer(): that method was ~55 lines and
 * ~4 of them were banking. This one is banking, top to bottom: check funds, two updates,
 * two ledger rows sharing a reference, one event. The connection, the auto-commit flag,
 * the commit, the rollback and the close all moved OUT — into the proxy that
 * @Transactional puts around this bean (AopAndEventsDemo proves the proxy exists).
 *
 * THE DEFAULT ROLLBACK RULE, stated exactly (the interview question, the outage report):
 *   RuntimeException and Error roll the transaction back. CHECKED exceptions DO NOT —
 *   the proxy lets them through and COMMITS. debitThenCheckedFailure below is the trap,
 *   live. The fix is @Transactional(rollbackFor = ...) — or the house rule: domain
 *   failures are unchecked (module 01's InsufficientFundsException already was).
 *
 * The event is published on the SUCCESS path only, INSIDE the transaction. Nuance: a plain
 * @EventListener hears it BEFORE the commit — fine here because nothing after it can fail
 * except the commit itself; production code that must not act on rolled-back work uses
 * @TransactionalEventListener(phase = AFTER_COMMIT). stage-5 sends its Kafka messages from
 * exactly that phase.
 */
public class TransferService {

    private final JdbcTransferRepository repository;
    private final ApplicationEventPublisher publisher;

    /** Constructor injection — the whole dependency list, in the signature (house rule). */
    public TransferService(JdbcTransferRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    /**
     * The J2EE-era domain exception, same shape as module 01's — still unchecked, because
     * the rollback rule above only catches unchecked ones by default.
     */
    public static class InsufficientFundsException extends RuntimeException {
        private final long accountId;
        private final BigDecimal attempted;
        private final BigDecimal available;

        public InsufficientFundsException(long accountId, BigDecimal attempted, BigDecimal available) {
            super("account " + accountId + " has " + available + ", cannot send " + attempted);
            this.accountId = accountId;
            this.attempted = attempted;
            this.available = available;
        }

        public long accountId() {
            return accountId;
        }

        public BigDecimal attempted() {
            return attempted;
        }

        public BigDecimal available() {
            return available;
        }
    }

    /**
     * Move money atomically. Returns the ledger reference both halves share.
     *
     * Walkthrough — what happens when a CALLER (holding the proxy) calls transfer(1, 3, 100.00):
     *   1. the call lands on the PROXY, not on this code: TransactionInterceptor asks the
     *      transaction manager to BEGIN — one connection, auto-commit off, bound to the thread;
     *   2. the AuditAspect's @Around advice records "before" (demo 6 — same proxy mechanism);
     *   3. the method below runs: every JdbcTemplate call silently joins the thread-bound
     *      connection (DataSourceUtils) — that is why this class never mentions transactions;
     *   4. the event is published, still inside the transaction;
     *   5. normal return -> the proxy COMMITs; RuntimeException/Error -> it ROLLBACKs and
     *      rethrows; a CHECKED exception -> it COMMITs anyway (the trap, banner 4 of
     *      TransactionalTransferDemo);
     *   6. the connection is unbound and released. Steps 1-2 and 5-6 are the ~50 lines
     *      module 01's DAO wrote by hand — now zero lines here.
     */
    @Transactional
    public String transfer(long fromId, long toId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, was: " + amount);
        }
        if (fromId == toId) {
            throw new IllegalArgumentException("cannot transfer to the same account");
        }

        BigDecimal fromBalance = repository.balanceOf(fromId);
        if (fromBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(fromId, amount, fromBalance);
        }

        String reference = UUID.randomUUID().toString();
        repository.debit(fromId, amount);
        repository.credit(toId, amount);
        repository.insertLedgerRow(fromId, "DEBIT", amount, "Transfer out", reference);
        repository.insertLedgerRow(toId, "CREDIT", amount, "Transfer in", reference);

        publisher.publishEvent(new TransferExecutedEvent(reference, fromId, toId, amount, Instant.now()));
        return reference;
    }

    /**
     * THE SELF-INVOCATION TRAP, as an exhibit (AopAndEventsDemo banner 3).
     *
     * this.transfer(...) below calls the RAW OBJECT's method — 'this' is the target the
     * proxy wraps, never the proxy itself. So the inner call gets NO advice and NO
     * transaction, even though transfer() is annotated. Each of its statements then runs
     * on its own auto-commit connection: the money still moves, silently un-atomic.
     * Same mechanism, same trap for @Transactional and @Async everywhere — stage-2 included.
     */
    public String selfInvokedTransfer(long fromId, long toId, BigDecimal amount) {
        return transfer(fromId, toId, amount);
    }

    /**
     * Rollback you can WATCH: the debit executes, then a RuntimeException fires — the proxy
     * erases the write (TransactionalTransferDemo banner 3 proves the balance is restored).
     */
    @Transactional
    public void debitThenRuntimeFailure(long accountId, BigDecimal amount) {
        repository.debit(accountId, amount);
        throw new IllegalStateException("runtime failure AFTER a write — the default rule rolls this back");
    }

    /**
     * The famous trap, live: the debit executes, then a CHECKED exception fires — and the
     * default rule COMMITS the debit. Money gone, no error in the ledger. Fix:
     * @Transactional(rollbackFor = Exception.class), or throw unchecked (house rule).
     */
    @Transactional
    public void debitThenCheckedFailure(long accountId, BigDecimal amount) throws Exception {
        repository.debit(accountId, amount);
        throw new Exception("checked failure AFTER a write — the default rule does NOT roll this back");
    }
}
