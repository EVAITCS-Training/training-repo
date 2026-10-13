package com.evaitcs.evolution.hibernate;

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
import java.time.OffsetDateTime;

/**
 * A row of stage-1's `loan` table — included so the mapping tour covers every table
 * stage-1 seeded, and to show a second enum column (status) plus a nullable money
 * column (monthly_payment, null until the loan is APPROVED).
 * (README section "Bootstrap"; registered with MetadataSources alongside the others.)
 *
 * Like Account, the customer link is LAZY: listing loans never drags in owners until
 * a getter touches them. principal / annualRate / monthlyPayment are all BigDecimal —
 * rates included, because a rate is money-per-money and double would round it wrong.
 */
@Entity
@Table(name = "loan")
public class Loan {

    /** stage-1's chk_loan_status CHECK ('APPLIED','APPROVED','REJECTED','PAID_OFF'), type-safe. */
    public enum LoanStatus {
        APPLIED, APPROVED, REJECTED, PAID_OFF
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "principal", nullable = false, precision = 15, scale = 2)
    private BigDecimal principal;

    @Column(name = "annual_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualRate;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status;

    /** Nullable money: stage-1 leaves this NULL until the loan is approved and amortized. */
    @Column(name = "monthly_payment", precision = 15, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "applied_at", nullable = false)
    private OffsetDateTime appliedAt = OffsetDateTime.now();

    public Loan() {
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

    public OffsetDateTime getAppliedAt() {
        return appliedAt;
    }
}
