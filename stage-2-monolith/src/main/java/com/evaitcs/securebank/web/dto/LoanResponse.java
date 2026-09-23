package com.evaitcs.securebank.web.dto;

import com.evaitcs.securebank.domain.Loan;

import java.math.BigDecimal;
import java.time.Instant;

/** Loan JSON shape — status and (once approved) the monthly payment. */
public record LoanResponse(
        Long id,
        BigDecimal principal,
        BigDecimal annualRate,
        int termMonths,
        String status,
        BigDecimal monthlyPayment, // null until APPROVED
        Instant appliedAt
) {
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(loan.getId(), loan.getPrincipal(), loan.getAnnualRate(),
                loan.getTermMonths(), loan.getStatus().name(), loan.getMonthlyPayment(),
                loan.getAppliedAt());
    }
}
