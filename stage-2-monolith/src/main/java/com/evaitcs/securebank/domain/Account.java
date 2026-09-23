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
import jakarta.persistence.Version;

import java.math.BigDecimal;

/**
 * The bank account — the heart of the domain. Maps to the `account` table
 * from stage-1 (including the CHECK (balance >= 0) business rule, which we
 * ALSO enforce in Java below so callers get a good error message instead of
 * a raw DB constraint violation).
 *
 * DESIGN CHOICE — "RICH" DOMAIN MODEL:
 *   deposit()/withdraw() live ON the entity, not in the service. The entity
 *   guards its own invariant (balance can never go negative), so NO code path
 *   can corrupt it. The service layer orchestrates (transactions, locking,
 *   recording history); the entity owns its own rules. This split is what
 *   keeps business logic from smearing across controllers.
 *
 * CONCURRENCY (Weeks 4-5 topic, real-world critical):
 *   Two simultaneous transfers could both read balance=100 and both withdraw
 *   100 ("lost update" race — the exact bug you saw in the Week 1
 *   ProcessVsThread demo, but at database scale). We defend twice:
 *   1. @Version — optimistic locking. Hibernate adds `WHERE version = ?` to
 *      UPDATEs; if another transaction modified the row first, the UPDATE
 *      affects 0 rows and Hibernate throws OptimisticLockException.
 *   2. TransferService additionally uses SELECT ... FOR UPDATE (pessimistic
 *      locking) so conflicting transfers queue up instead of failing.
 */
@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * MANY-TO-ONE: many accounts point at one customer — the Java mirror of
     * the account.customer_id foreign key. optional=false = NOT NULL column.
     * LAZY = don't fetch the Customer row until something touches it.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /**
     * Enums map to VARCHAR with @Enumerated(STRING). STRING (not the default
     * ORDINAL) stores "CHECKING"/"SAVINGS" — readable in SQL, and safe when
     * you later add/reorder enum constants.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    /**
     * MONEY RULE #1: never use float/double for money (binary floating point
     * cannot represent 0.10 exactly — errors accumulate). BigDecimal is exact
     * decimal arithmetic. precision=15, scale=2 matches DECIMAL(15,2) in SQL.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    /** Optimistic-lock counter, incremented by Hibernate on every UPDATE. */
    @Version
    private Long version;

    protected Account() {
    }

    public Account(Customer customer, AccountType type) {
        this.customer = customer;
        this.type = type;
    }

    /**
     * Business method — the ONLY way money enters an account.
     * MONEY RULE #2: always specify scale + rounding explicitly.
     */
    public void deposit(BigDecimal amount) {
        requirePositive(amount);
        this.balance = this.balance.add(amount);
    }

    /**
     * Business method — the ONLY way money leaves an account.
     * The invariant (balance >= 0, same as the DB CHECK constraint) is
     * enforced here first, so the failure is a domain exception with a clear
     * message rather than an opaque SQL error.
     */
    public void withdraw(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(id, balance, amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    private static void requirePositive(BigDecimal amount) {
        // compareTo (not equals!) — BigDecimal.equals considers scale, so
        // 1.0 != 1.00 with equals(), but compareTo says they're numerically equal.
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive, got: " + amount);
        }
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public AccountType getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
