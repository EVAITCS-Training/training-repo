package com.evaitcs.securebank.loan.config;

import com.evaitcs.securebank.loan.domain.Loan;
import com.evaitcs.securebank.loan.repository.LoanRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Seeds ONE pending application so the teller queue (and the lecture demo)
 * has something to decide on first boot — mirroring stage-1/stage-2's seeded
 * loan (customer 1, 10,000 @ 7.5%, 36 months).
 *
 * customerId = 1 references ALICE IN ACCOUNT-SERVICE'S DATABASE (cross-context
 * id contract; see LoanServiceApplication). No FK, no join — just a value both
 * seeders agree on. That agreement is the coupling distributed systems always
 * inherit; naming it here is the point.
 */
@Component
public class LoanDataSeeder implements ApplicationRunner {

    private final LoanRepository loans;

    public LoanDataSeeder(LoanRepository loans) {
        this.loans = loans;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (loans.count() > 0) {
            return;
        }
        loans.save(new Loan(1L, new BigDecimal("10000.00"), new BigDecimal("7.50"), 36));
    }
}
