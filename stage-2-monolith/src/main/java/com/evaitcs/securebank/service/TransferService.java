package com.evaitcs.securebank.service;

import com.evaitcs.securebank.domain.Account;
import com.evaitcs.securebank.domain.InsufficientFundsException;
import com.evaitcs.securebank.domain.ResourceNotFoundException;
import com.evaitcs.securebank.domain.Transaction;
import com.evaitcs.securebank.repository.AccountRepository;
import com.evaitcs.securebank.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * THE MOST IMPORTANT CLASS IN STAGE 2 — a money transfer between two accounts.
 *
 * It combines every concurrency + transaction lesson of Weeks 4-6 in ~40 lines:
 *
 *   ACID "A" (Atomicity): debit + credit + 2 ledger rows are ONE transaction.
 *      If ANY step throws, everything rolls back — money never disappears or
 *      duplicates. (Week 3: BEGIN/COMMIT/ROLLBACK, now declarative.)
 *
 *   CONSISTENT LOCK ORDERING: we lock the two accounts in ascending id order.
 *      Without this, transfer(1->2) and transfer(2->1) running simultaneously
 *      could each hold one lock and wait for the other forever = DEADLOCK.
 *      Sorting lock acquisition is the classic fix (and a favorite interview
 *      question — see Week 9 System Design).
 *
 *   PESSIMISTIC LOCKING: findByIdForUpdate = SELECT ... FOR UPDATE. The second
 *      transfer waits for the first to commit, then reads the FRESH balance.
 *
 * In Stage 5 this same logic crosses a SERVICE boundary (payment-service
 * orchestrating account-service) — where a single ACID transaction is no
 * longer possible and a SAGA with compensating actions replaces it.
 * Compare TransferService (here) with stage-5 payment-service to see exactly
 * what microservices cost you.
 */
@Service
public class TransferService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public TransferService(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    /**
     * Move `amount` from one of MY accounts to any target account.
     *
     * @param initiatorCustomerId the customer id from the verified JWT —
     *        NEVER from the request body. Trusting client-supplied identity is
     *        the #1 OWASP mistake; the token claim is the only truth.
     * @return the correlation reference shared by both ledger rows
     */
    @Transactional
    public String transfer(Long fromAccountId, Long toAccountId, BigDecimal amount, Long initiatorCustomerId) {

        // --- Rule checks that need no database -----------------------------
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Source and target accounts must differ");
        }

        // --- Authorization: the initiator must OWN the source account -------
        Account probe = accounts.findById(fromAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", fromAccountId));
        if (!probe.getCustomer().getId().equals(initiatorCustomerId)) {
            // 404 (not 403) — don't confirm the account exists (see AccountService).
            throw new ResourceNotFoundException("Account", fromAccountId);
        }

        // --- Lock BOTH rows in a deterministic order (deadlock prevention) --
        Long firstId = Math.min(fromAccountId, toAccountId);
        Long secondId = Math.max(fromAccountId, toAccountId);
        Account first = lockOrThrow(firstId);
        Account second = lockOrThrow(secondId);
        Account from = firstId.equals(fromAccountId) ? first : second;
        Account to = firstId.equals(toAccountId) ? first : second;

        // --- Move the money via the entities' own guarded methods -----------
        from.withdraw(amount); // may throw InsufficientFundsException -> full rollback
        to.deposit(amount);

        // --- Write the audit trail: TWO rows, ONE shared reference ----------
        String reference = UUID.randomUUID().toString();
        transactions.save(Transaction.debit(from, amount, "Transfer out to account " + to.getId(), reference));
        transactions.save(Transaction.credit(to, amount, "Transfer in from account " + from.getId(), reference));

        // Commit happens when this method returns: Hibernate flushes the two
        // updated balances + two inserts as ONE atomic unit.
        return reference;
    }

    private Account lockOrThrow(Long id) {
        return accounts.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }
}
