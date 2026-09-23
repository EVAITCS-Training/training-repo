package com.evaitcs.securebank.repository;

import com.evaitcs.securebank.domain.Loan;
import com.evaitcs.securebank.domain.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Loan lookups — derived queries with MULTIPLE conditions and enum filters. */
public interface LoanRepository extends JpaRepository<Loan, Long> {

    /** WHERE customer_id = ? ORDER BY applied_at DESC */
    List<Loan> findByCustomerIdOrderByAppliedAtDesc(Long customerId);

    /**
     * "And" in the name = multiple WHERE clauses. Enum parameters bind to
     * their STRING representation because of @Enumerated(EnumType.STRING).
     * Used by the teller dashboard: "loans waiting for a decision".
     */
    List<Loan> findByStatusOrderByAppliedAtAsc(LoanStatus status);
}
