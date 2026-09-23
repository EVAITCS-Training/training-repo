package com.evaitcs.securebank.loan.service;

import com.evaitcs.securebank.loan.domain.Loan;
import com.evaitcs.securebank.loan.domain.LoanStatus;
import com.evaitcs.securebank.loan.domain.ResourceNotFoundException;
import com.evaitcs.securebank.loan.events.LoanEventPublisher;
import com.evaitcs.securebank.loan.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lending workflow — stage-2's LoanService with two microservice upgrades:
 *   1. customer references are plain ids (no entity, no FK — see Loan.java),
 *   2. decisions PUBLISH EVENTS after committing (approve -> LoanApproved).
 *
 * Event placement detail worth noticing: publish happens AFTER the state
 * transition is saved but INSIDE the same method — not inside the DB
 * transaction boundary semantics (Kafka send is async anyway). The failure
 * window between "committed" and "published" is exactly what the OUTBOX
 * pattern closes (LoanEventPublisher comment).
 */
@Service
public class LoanService {

    public static final BigDecimal STANDARD_ANNUAL_RATE = new BigDecimal("7.50");

    private final LoanRepository loans;
    private final LoanEventPublisher.Publisher events;

    public LoanService(LoanRepository loans, LoanEventPublisher.Publisher events) {
        this.loans = loans;
        this.events = events;
    }

    @Transactional
    public Loan apply(Long customerId, BigDecimal principal, int termMonths) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal must be positive");
        }
        if (termMonths < 6 || termMonths > 360) {
            throw new IllegalArgumentException("Term must be between 6 and 360 months");
        }
        return loans.save(new Loan(customerId, principal, STANDARD_ANNUAL_RATE, termMonths));
    }

    @Transactional(readOnly = true)
    public List<Loan> pending() {
        return loans.findByStatusOrderByAppliedAtAsc(LoanStatus.APPLIED);
    }

    @Transactional(readOnly = true)
    public List<Loan> listForCustomer(Long customerId) {
        return loans.findByCustomerIdOrderByAppliedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public Loan get(Long loanId, Long customerId, boolean staff) {
        Loan loan = loans.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        if (!staff && !loan.getCustomerId().equals(customerId)) {
            throw new ResourceNotFoundException("Loan", loanId);
        }
        return loan;
    }

    /** Staff-only (enforced by @PreAuthorize in the controller). */
    @Transactional
    public Loan approve(Long loanId) {
        Loan loan = loans.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        loan.approve(); // guards the transition; computes monthly payment
        Loan saved = loans.save(loan);
        events.publishApproved(saved.getId(), saved.getCustomerId(), saved.getPrincipal(),
                saved.getTermMonths(), saved.getMonthlyPayment());
        return saved;
    }

    @Transactional
    public Loan reject(Long loanId) {
        Loan loan = loans.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        loan.reject();
        Loan saved = loans.save(loan);
        events.publishRejected(saved.getId(), saved.getCustomerId());
        return saved;
    }
}
