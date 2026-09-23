package com.evaitcs.securebank.loan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Loan entity — migrated from stage-2 with ONE structural change:
 * `@ManyToOne Customer customer` became `Long customerId`. The Customer
 * table lives in another service's database; JPA relationships cannot cross
 * that boundary (see LoanServiceApplication for the trade-off discussion).
 *
 * The state machine (approve/reject guards) and the annuity math are
 * UNCHANGED — same formula, same explicit scale+rounding discipline:
 *   M = P·r / (1 − (1+r)^(−n)),  r = annualRate/1200
 */
@Entity
@Table(name = "loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cross-context reference BY VALUE — no FK exists or is wanted. */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principal;

    @Column(name = "annual_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualRate;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.APPLIED;

    @Column(name = "monthly_payment", precision = 15, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt = Instant.now();

    protected Loan() {
    }

    public Loan(Long customerId, BigDecimal principal, BigDecimal annualRate, int termMonths) {
        this.customerId = customerId;
        this.principal = principal;
        this.annualRate = annualRate;
        this.termMonths = termMonths;
    }

    public void approve() {
        if (status != LoanStatus.APPLIED) {
            throw new IllegalStateException("Only APPLIED loans can be approved, current status: " + status);
        }
        BigDecimal r = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(r).pow(termMonths);
        this.monthlyPayment = principal.multiply(r).multiply(factor)
                .divide(factor.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
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

    public Long getCustomerId() {
        return customerId;
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
