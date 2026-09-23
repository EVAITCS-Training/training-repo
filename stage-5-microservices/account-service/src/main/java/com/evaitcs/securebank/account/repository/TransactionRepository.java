package com.evaitcs.securebank.account.repository;

import com.evaitcs.securebank.account.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Ledger queries — paged statements (see stage-2 StatementService comments). */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByAccountIdOrderByOccurredAtDesc(Long accountId, Pageable pageable);
}
