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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A row of stage-1's `account` table — the entity this module leans on hardest.
 * (README sections "The ORM bargain", "Entity lifecycle", "Optimistic locking".)
 *
 * Three teaching points live in this one class:
 *   1. balance is BigDecimal(scale 2) — money is never a double, in any era
 *      (advanced-java/12 and stage-1's house rule).
 *   2. version is @Version — stage-1 planted the column and module 01's DAO bumped it
 *      by hand inside its UPDATE. Here Hibernate manages it: every flush that changes
 *      the row writes version + 1 and guards with WHERE version = <what I read>.
 *      That guard is optimistic locking (OptimisticLockingDemo).
 *   3. type is an ENUM mapped with @Enumerated(STRING) — the CHECK constraint's
 *      two values become compile-time-safe Java.
 *
 * The customer/ledgerEntries collections are LAZY: loading an Account does NOT drag in
 * its owner or its ledger until code touches them (LazyAndNPlusOneDemo).
 */
@Entity
@Table(name = "account")
public class Account {

    /** stage-1's chk_account_type CHECK ('CHECKING','SAVINGS'), as a type-safe enum. */
    public enum AccountType {
        CHECKING, SAVINGS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * MANY accounts to ONE customer. @JoinColumn(name = "customer_id") names the FK
     * column that already exists in stage-1's schema — Hibernate does not invent
     * columns, it maps the ones the DBA drew. LAZY: the owner loads only on demand.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AccountType type;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    /**
     * The optimistic-lock column. Hibernate reads it on load, then on the flush that
     * writes a change issues: UPDATE account SET ..., version = version + 1
     * WHERE id = ? AND version = <the value this session loaded>. If another session
     * committed first, that WHERE matches ZERO rows -> OptimisticLockException
     * (OptimisticLockingDemo proves it; README "Optimistic locking").
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** The ledger rows for this account (stage-1's `transactions`). LAZY, mappedBy the FK on the child. */
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();

    public Account() {
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

    public long getVersion() {
        return version;
    }

    public List<LedgerEntry> getLedgerEntries() {
        return ledgerEntries;
    }

    /**
     * The setter DirtyCheckingDemo leans on: change this in a transaction, commit, and
     * Hibernate writes the UPDATE for you — the ORM bargain (README section 2). We keep
     * setters minimal on purpose: an entity is not a free-for-all DTO, it models state
     * the database also holds.
     */
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        // Deliberately does NOT touch customer or ledgerEntries: those are lazy, and a
        // toString() that initializes them after the session closed would throw
        // LazyInitializationException (the README's loudest trap). Print identity only.
        return "Account{id=" + id + ", type=" + type + ", balance=" + balance + ", version=" + version + "}";
    }
}
