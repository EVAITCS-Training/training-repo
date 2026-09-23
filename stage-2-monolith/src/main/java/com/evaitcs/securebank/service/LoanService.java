package com.evaitcs.securebank.service;

import com.evaitcs.securebank.domain.Customer;
import com.evaitcs.securebank.domain.Loan;
import com.evaitcs.securebank.domain.LoanStatus;
import com.evaitcs.securebank.domain.ResourceNotFoundException;
import com.evaitcs.securebank.repository.CustomerRepository;
import com.evaitcs.securebank.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lending workflow: apply -> approve/reject.
 *
 * WORKFLOW RULES live here (only APPLIED loans can be approved — the entity
 * guards the transition), while AUTHORIZATION rules live in the controller
 * (@PreAuthorize("hasRole('TELLER')") on the approve endpoint). Splitting
 * "what the business allows" from "who may ask for it" keeps both testable
 * and readable.
 *
 * In Stage 5 this class becomes the loan-service — note it already touches
 * only loan data (+ a customer lookup), which is WHY the bounded-context
 * split is clean.
 */
@Service
public class LoanService {

    /**
     * Product configuration as a constant for now. Real systems read rates
     * from config (Stage 5: the Config Server!) or a pricing engine.
     */
    public static final BigDecimal STANDARD_ANNUAL_RATE = new BigDecimal("7.50");

    private final LoanRepository loans;
    private final CustomerRepository customers;

    public LoanService(LoanRepository loans, CustomerRepository customers) {
        this.loans = loans;
        this.customers = customers;
    }

    @Transactional
    public Loan apply(Long customerId, BigDecimal principal, int termMonths) {
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal must be positive");
        }
        if (termMonths < 6 || termMonths > 360) {
            throw new IllegalArgumentException("Term must be between 6 and 360 months");
        }
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        return loans.save(new Loan(customer, principal, STANDARD_ANNUAL_RATE, termMonths));
    }

    /** Teller view: the queue of applications awaiting a decision. */
    @Transactional(readOnly = true)
    public List<Loan> pending() {
        return loans.findByStatusOrderByAppliedAtAsc(LoanStatus.APPLIED);
    }

    /** Customer view: only their own applications. */
    @Transactional(readOnly = true)
    public List<Loan> listForCustomer(Long customerId) {
        return loans.findByCustomerIdOrderByAppliedAtDesc(customerId);
    }

    /**
     * Ownership-checked read. `staff` callers (teller/admin — determined from
     * the JWT role in the controller) bypass the ownership filter; customers
     * may only read their own loans. Same "404 instead of 403" privacy rule.
     */
    @Transactional(readOnly = true)
    public Loan get(Long loanId, Long customerId, boolean staff) {
        Loan loan = loans.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        if (!staff && !loan.getCustomer().getId().equals(customerId)) {
            throw new ResourceNotFoundException("Loan", loanId);
        }
        return loan;
    }

    /** Staff-only transition (enforced by @PreAuthorize in LoanController). */
    @Transactional
    public Loan approve(Long loanId) {
        Loan loan = loans.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        loan.approve(); // IllegalStateException if not APPLIED -> mapped to 409 by the handler
        return loan;
    }

    @Transactional
    public Loan reject(Long loanId) {
        Loan loan = loans.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        loan.reject();
        return loan;
    }
}
