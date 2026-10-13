package com.evaitcs.evolution.app.era2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * ERA 2 — one row of stage-1's `transactions` table: the append-only ledger where
 * module 01's DAO hand-inserted the DEBIT+CREDIT pair sharing one `reference`.
 * Copied from module 02, plus ONE addition: the constructor below. Module 02 only ever
 * READ ledger rows; the capstone WRITES them through Hibernate, so the entity needs a
 * way to be built — everything else is unchanged.
 *
 * @Table(name = "transactions") because the Java name and the SQL name differ: the
 * class says what a row IS (a ledger entry); the table keeps the era's name.
 * `type` stays a String (not an enum): the ledger is append-only history, and the
 * CHECK constraint ('DEBIT','CREDIT') guards the values at the only door that matters.
 */
@Entity
@Table(name = "transactions")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The FK to `account`, as an object. Module 01 set this column with ps.setLong(1, accountId). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "type", nullable = false, length = 10)
    private String type;                                   // 'DEBIT' | 'CREDIT'

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", length = 200)
    private String description;

    /** stage-1's reconciliation rule: both halves of a transfer share one reference UUID. */
    @Column(name = "reference", length = 36)
    private String reference;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt = OffsetDateTime.now();

    public LedgerEntry() {
    }

    /**
     * The capstone's addition: build one half of a transfer's ledger pair. session.persist(this)
     * then writes the INSERT — module 01's insertLedgerRow() was a PreparedStatement with five
     * ps.setX calls; here the object graph IS the statement.
     */
    public LedgerEntry(Account account, String type, BigDecimal amount, String description, String reference) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.reference = reference;
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getReference() {
        return reference;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
