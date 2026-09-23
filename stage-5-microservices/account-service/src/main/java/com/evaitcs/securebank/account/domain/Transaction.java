package com.evaitcs.securebank.account.domain;

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
 * Immutable ledger row — migrated from stage-2 (see the long comment there:
 * append-only history, positive amounts + direction in `type`, correlation
 * via `reference`).
 *
 * WHAT THE SPLIT ADDED: the `reference` on saga rows is payment-service's
 * transfer id (prefix "xfer-"), and COMPENSATION rows carry the suffix
 * "-comp". When the saga's credit step fails and the debit is undone, the
 * ledger shows the full story: debit, compensating credit, same reference
 * family. Auditability across service boundaries lives in this one column.
 */
@Entity
@Table(name = "transactions", indexes = {
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

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 200)
    private String description;

    @Column(length = 64)
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
