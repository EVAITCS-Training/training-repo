package com.evaitcs.securebank.repository;

import com.evaitcs.securebank.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Ledger queries. Demonstrates PAGING — the professional way to return lists
 * that could grow unbounded (Week 7 API design: never expose an unbounded
 * collection; a customer with 10 years of transactions would OOM your API).
 *
 * Pageable is assembled by Spring MVC from query parameters automatically:
 *   GET /api/accounts/1/transactions?page=0&size=20&sort=occurredAt,desc
 * The Page<T> result carries content + total count + page metadata, which
 * the controller maps into a paginated response DTO.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByAccountIdOrderByOccurredAtDesc(Long accountId, Pageable pageable);

    /** The dashboard's "recent activity" box — top N across all of a customer's accounts. */
    @org.springframework.data.jpa.repository.Query("""
            SELECT t FROM Transaction t
            WHERE t.account.customer.id = :customerId
            ORDER BY t.occurredAt DESC
            """)
    Page<Transaction> findRecentForCustomer(@org.springframework.data.repository.query.Param("customerId") Long customerId,
                                            Pageable pageable);
}
