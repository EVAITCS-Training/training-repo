package com.evaitcs.securebank.account.repository;

import com.evaitcs.securebank.account.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Same three query styles as stage-2 (derived / JPQL / locking) — see the
 * annotated version in stage-2-monolith for the full lesson. findByIdForUpdate
 * backs BOTH the public withdraw endpoint and the internal saga debit.
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByCustomerIdOrderByTypeAsc(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE) // SELECT ... FOR UPDATE
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
