package com.evaitcs.securebank.repository;

import com.evaitcs.securebank.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Account lookups. Shows THREE ways to express a query, in increasing
 * control — pick the simplest one that does the job:
 *
 * 1. DERIVED (method name parsed):  findByCustomerIdOrderByTypeAsc
 * 2. JPQL (@Query, object-oriented SQL against ENTITIES, not tables)
 * 3. Locking variants of either, for concurrency control
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * 1) DERIVED QUERY. Name grammar: find + By + <property> + OrderBy + <property> + Asc.
     *    -> SELECT * FROM account WHERE customer_id = ? ORDER BY type
     *    Used by the dashboard ("list MY accounts").
     */
    List<Account> findByCustomerIdOrderByTypeAsc(Long customerId);

    /**
     * 1b) DERIVED with a JOIN across a relationship: "customer.id" navigates
     *     Account -> Customer in JPQL terms; Spring generates the join.
     */
    List<Account> findByCustomerEmail(String email);

    /**
     * 2) JPQL @Query — same result, but the query is explicit. Use @Query
     *    when the method name would become unreadable or you need joins,
     *    aggregates, or fetch control. Note: JPQL uses ENTITY/FIELD names
     *    (Account.customer), SQL uses TABLE/COLUMN names (account.customer_id).
     *
     *    "JOIN FETCH" eagerly loads the customer IN THE SAME QUERY — this is
     *    the standard fix for the N+1 SELECT problem (Week 6 lesson: 1 query
     *    for N accounts + N queries for their customers = N+1; JOIN FETCH = 1).
     */
    @Query("""
            SELECT a FROM Account a
            JOIN FETCH a.customer
            WHERE a.customer.id = :customerId
            """)
    List<Account> findAllWithCustomer(@Param("customerId") Long customerId);

    /**
     * 3) PESSIMISTIC WRITE LOCK — "SELECT ... FOR UPDATE".
     *
     *    The row is locked in the database until the surrounding transaction
     *    commits/rolls back. Any other transaction trying to lock the same
     *    row BLOCKS (queues) instead of reading stale data. TransferService
     *    uses this so two concurrent transfers between the same accounts are
     *    serialized — no lost updates, no negative balances.
     *
     *    Trade-off vs @Version (optimistic): pessimistic = callers wait
     *    (correct, slightly slower under contention); optimistic = callers
     *    fail and retry (fast, but you must handle the retry). Real banking
     *    cores typically use pessimistic locks on balances.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
