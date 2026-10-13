package com.evaitcs.evolution.app.era3;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ERA 3 — module 03's TransferService: the business rule and ONLY the business rule,
 * now called over HTTP by TransferController. Compare it with the two eras below it on
 * the same server:
 *   - era 1 wrapped these four banking lines in ~50 lines of connection/auto-commit/
 *     commit/rollback/close plumbing (JdbcTransferDao);
 *   - era 2 deleted the SQL but still hand-managed Session + Transaction per call;
 *   - era 3: @Transactional's proxy owns begin/commit/rollback (module 03's lesson —
 *     including its traps: self-invocation bypasses the proxy, checked exceptions do
 *     NOT roll back by default; both are proven live in 03-spring-framework-core, so
 *     this copy stays lean and does not re-stage them).
 *
 * The event publisher module 03 had is deliberately absent: this service's job in the
 * capstone is the HTTP round-trip, and stage-2's TransferService is exactly this class
 * plus ownership checks.
 */
@Service
public class TransferService {

    private final JdbcTransferRepository repository;

    /** Constructor injection — the whole dependency list, in the signature (house rule). */
    public TransferService(JdbcTransferRepository repository) {
        this.repository = repository;
    }

    /**
     * The domain exception, same shape in all three eras (era 1's DAO nested it, era 2's
     * DAO nested it, era 3 keeps it here). Still unchecked — the default rollback rule
     * only catches RuntimeException/Error. What CHANGED between eras is only WHO turns
     * it into HTTP: era 1/2 caught it inline in the servlet; era 3 lets it escape to
     * BankApiAdvice, which maps it to 409 + problem+json once, for every controller.
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
     * What the controller maps onto TransferResponse: the ledger reference plus the two
     * balances AFTER the move. Reading them back inside the same @Transactional method
     * is what makes them exact — the transaction has not committed yet, but every
     * JdbcTemplate call joins the same thread-bound connection, so it sees its own writes.
     */
    public record TransferReceipt(String reference, BigDecimal newFromBalance, BigDecimal newToBalance) {
    }

    /**
     * Move money atomically.
     *
     * Walkthrough — what happens when the controller (holding the proxy) calls transfer(2, 3, 25.00):
     *   1. the call lands on the PROXY, not on this code: TransactionInterceptor asks the
     *      DataSourceTransactionManager to BEGIN — one connection, auto-commit off,
     *      bound to this request thread;
     *   2. the method below runs: check funds -> debit/credit -> two ledger rows sharing
     *      a fresh UUID reference (stage-1's reconciliation rule);
     *   3. the two balance reads see this transaction's own writes (same connection);
     *   4. normal return -> the proxy COMMITs all five statements at once;
     *      RuntimeException (InsufficientFunds, IllegalArgument) -> ROLLBACK, nothing landed;
     *   5. the connection is unbound and released. Steps 1, 4 (commit/rollback) and 5
     *      are era 1's ~50 hand-written lines and era 2's hand-managed Session — now
     *      zero lines here.
     */
    @Transactional
    public TransferReceipt transfer(long fromId, long toId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, was: " + amount);
        }
        if (fromId == toId) {
            throw new IllegalArgumentException("cannot transfer to the same account");
        }

        BigDecimal fromBalance = repository.balanceOf(fromId);   // unknown id -> AccountNotFoundException
        if (fromBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(fromId, amount, fromBalance);
        }

        String reference = UUID.randomUUID().toString();
        repository.debit(fromId, amount);
        repository.credit(toId, amount);
        repository.insertLedgerRow(fromId, "DEBIT", amount, "Transfer out", reference);
        repository.insertLedgerRow(toId, "CREDIT", amount, "Transfer in", reference);

        return new TransferReceipt(reference, repository.balanceOf(fromId), repository.balanceOf(toId));
    }

    /** The GET endpoint's read path — no transaction needed for one SELECT, but readOnly documents intent. */
    @Transactional(readOnly = true)
    public AccountView account(long accountId) {
        JdbcTransferRepository.AccountRow row = repository.findAccount(accountId);
        return new AccountView(accountId, row.type(), row.balance());
    }
}
