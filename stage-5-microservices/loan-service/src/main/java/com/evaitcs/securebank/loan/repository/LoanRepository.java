package com.evaitcs.securebank.loan.repository;

import com.evaitcs.securebank.loan.domain.Loan;
import com.evaitcs.securebank.loan.domain.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByCustomerIdOrderByAppliedAtDesc(Long customerId);

    List<Loan> findByStatusOrderByAppliedAtAsc(LoanStatus status);
}
