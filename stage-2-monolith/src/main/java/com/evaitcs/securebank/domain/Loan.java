package com.evaitcs.securebank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * A loan application and its lifecycle. Maps to the `loan` table (stage-1).
 *
 * STATE MACHINE: APPLIED -> APPROVED (or REJECTED) -> PAID_OFF.
 * The transitions are enforced by approve()/reject() below — you cannot
 * approve a loan twice or reject an approved one. Modeling lifecycle rules
 * as guarded methods (instead of letting anyone set the status field) is the
 * same "rich domain" idea as Account.deposit/withdraw.
 *
 * WHY LOANS MATTER IN THIS COURSE:
 * In Stage 5, loans become their own microservice (loan-service) — the
 * "Lending bounded context" from the Week 7 DDD lesson. Keeping the logic
 * self-contained here is what makes that split painless later.
 */
@Entity
@Table(name = "loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principal;

    /** Annual percentage rate, e.g. 7.50 means 7.5% per year. */
    @Column(name = "annual_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualRate;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.APPLIED;

    /** Filled in on approval — the fixed monthly installment. */
    @Column(name = "monthly_payment", precision = 15, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt = Instant.now();

    protected Loan() {
    }

    public Loan(Customer customer, BigDecimal principal, BigDecimal annualRate, int termMonths) {
        this.customer = customer;
        this.principal = principal;
        this.annualRate = annualRate;
        this.termMonths = termMonths;
    }

    /**
     * APPROVED transition — performed by a teller/admin (authorization is
     * enforced one layer up, in LoanController via @PreAuthorize).
     *
     * AMORTIZATION MATH (the standard annuity formula):
     *   monthly payment M = P * r / (1 - (1 + r)^-n)
     * where P = principal, r = monthly rate (annual/12/100), n = term months.
     *
     * MONEY RULE #3: divide with EXPLICIT scale + RoundingMode. BigDecimal
     * division without a rounding mode throws ArithmeticException whenever
     * the true result doesn't terminate (e.g. 1/3) — a production outage
     * waiting to happen.
     */
    public void approve() {
        if (status != LoanStatus.APPLIED) {
            throw new IllegalStateException("Only APPLIED loans can be approved, current status: " + status);
        }
        BigDecimal r = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusR = BigDecimal.ONE.add(r);
        // (1 + r)^-n  via pow() on the inverted base — BigDecimal has no
        // fractional exponentiation, so we compute (1+r)^n then divide.
        BigDecimal factor = onePlusR.pow(termMonths);
        BigDecimal payment = principal.multiply(r).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        this.monthlyPayment = payment;
        this.status = LoanStatus.APPROVED;
    }

    public void reject() {
        if (status != LoanStatus.APPLIED) {
            throw new IllegalStateException("Only APPLIED loans can be rejected, current status: " + status);
        }
        this.status = LoanStatus.REJECTED;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }
}
