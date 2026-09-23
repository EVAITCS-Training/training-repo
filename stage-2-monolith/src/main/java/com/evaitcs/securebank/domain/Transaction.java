package com.evaitcs.securebank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An immutable ledger entry — one row per money movement. Maps to the
 * `transactions` table (stage-1 schema).
 *
 * AUDIT-LOG PRINCIPLE: we NEVER update a balance without writing a
 * Transaction. The account balance is the "current truth"; the transactions
 * table is the "history of how it got there". Statements (Week 3 SQL:
 * SELECT ... WHERE account_id = ? ORDER BY occurred_at DESC) read this table.
 *
 * TRANSFER CORRELATION: a transfer produces TWO transactions (a DEBIT on the
 * source account, a CREDIT on the target) sharing the same `reference` (a
 * UUID). That is how you reconstruct both halves of a transfer without a
 * separate join table — the same trick payment systems use for idempotency
 * keys and reconciliation.
 */
@Entity
@Table(name = "transactions", indexes = {
        // Index the columns we filter/sort by — same reasoning as the
        // CREATE INDEX statements in stage-1/queries.
        @Index(name = "idx_tx_account_time", columnList = "account_id, occurred_at")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    /** Always stored POSITIVE; `type` tells you the direction. Storing
     *  signed amounts duplicates the sign in two places (type + amount)
     *  which can drift apart — a classic data-modeling smell. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 200)
    private String description;

    /** Correlation id linking the two halves of a transfer (or null). */
    @Column(length = 36)
    private String reference;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    protected Transaction() {
    }

    public static Transaction debit(Account account, BigDecimal amount, String description, String reference) {
        return new Transaction(account, TransactionType.DEBIT, amount, description, reference);
    }

    public static Transaction credit(Account account, BigDecimal amount, String description, String reference) {
        return new Transaction(account, TransactionType.CREDIT, amount, description, reference);
    }

    private Transaction(Account account, TransactionType type, BigDecimal amount,
                        String description, String reference) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.reference = reference;
    }

    // No setters — a ledger entry is IMMUTABLE. You never "edit history";
    // you append a correcting entry (that's how real banks do reversals).
    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public TransactionType getType() {
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

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
