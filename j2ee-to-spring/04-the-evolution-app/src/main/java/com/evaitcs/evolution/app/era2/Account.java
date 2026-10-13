package com.evaitcs.evolution.app.era2;

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
 * ERA 2 — a row of stage-1's `account` table. Copied from module 02 unchanged except
 * its package. The three teaching points travel with it:
 *   1. balance is BigDecimal(scale 2) — money is never a double, in any era.
 *   2. version is @Version: module 01's DAO bumped that column BY HAND inside its
 *      UPDATE ("version = version + 1"); here Hibernate manages it — every flush that
 *      changes the row writes version + 1 and guards with WHERE version = <what this
 *      session read>. The capstone's era-2 test asserts the bump happens with no code.
 *   3. type is an enum mapped with @Enumerated(STRING) — the CHECK constraint's two
 *      values become compile-time-safe Java.
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

    /** MANY accounts to ONE customer; LAZY — the owner loads only on demand. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AccountType type;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    /** The optimistic-lock column — see point 2 of the class javadoc. */
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
     * The setter the whole ORM bargain rides on: change this inside a transaction and
     * Hibernate writes the UPDATE at flush time — dirty checking. Era 2's DAO does
     * exactly that, and never spells the word UPDATE.
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
        // LazyInitializationException (module 02's loudest trap). Print identity only.
        return "Account{id=" + id + ", type=" + type + ", balance=" + balance + ", version=" + version + "}";
    }
}
