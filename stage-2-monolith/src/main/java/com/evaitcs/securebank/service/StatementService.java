package com.evaitcs.securebank.service;

import com.evaitcs.securebank.domain.Transaction;
import com.evaitcs.securebank.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Statement queries — READ side of the ledger.
 *
 * Separating reads from writes (even in a monolith!) is the seed of the CQRS
 * idea discussed in Week 7 API design: writes go through AccountService/
 * TransferService with locks and invariants; reads are cheap, paginated,
 * and can later be served from replicas or caches without touching the
 * write path.
 *
 * Ownership is ALREADY enforced by the controller calling
 * AccountService.getOwnedAccount(...) first — services compose like that
 * instead of duplicating checks.
 */
@Service
public class StatementService {

    private final TransactionRepository transactions;

    public StatementService(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    /** One account's history, newest first, one PAGE at a time. */
    @Transactional(readOnly = true)
    public Page<Transaction> accountStatement(Long accountId, Pageable pageable) {
        return transactions.findByAccountIdOrderByOccurredAtDesc(accountId, pageable);
    }

    /** "Recent activity" across ALL of a customer's accounts (dashboard box). */
    @Transactional(readOnly = true)
    public Page<Transaction> recentActivity(Long customerId, Pageable pageable) {
        return transactions.findRecentForCustomer(customerId, pageable);
    }
}
