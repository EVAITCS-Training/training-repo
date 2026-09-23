package com.evaitcs.securebank.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The saga's persisted state — payment-service's ONLY table.
 *
 * CROSS-CONTEXT REFERENCES ARE VALUES: fromAccountId/toAccountId point at
 * rows in ACCOUNT-service's database. No foreign key is possible (different
 * database!) and none is desired — this row must remain meaningful even when
 * the account service is mid-deploy. Distributed data modeling rule:
 * reference by id, verify by API, never join.
 *
 * The `reference` ("xfer-<id>") is the correlation key that appears in
 * account-service's ledger rows AND in the Kafka events — one id to trace a
 * transfer across three systems (logs, ledger, notifications).
 */
@Entity
@Table(name = "transfer")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    @Column(name = "from_account_id", nullable = false)
    private Long fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private Long toAccountId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferStatus status = TransferStatus.PENDING;

    /** Why the saga stopped — surfaced to clients and operators alike. */
    @Column(length = 300)
    private String reason;

    /** The customer whose JWT started this (audit; ownership-checked at the door). */
    @Column(name = "initiator_customer_id", nullable = false)
    private Long initiatorCustomerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Transfer() {
    }

    public Transfer(Long fromAccountId, Long toAccountId, BigDecimal amount, Long initiatorCustomerId) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.initiatorCustomerId = initiatorCustomerId;
        // Generated AT CONSTRUCTION: the column is NOT NULL, so the reference
        // must exist before the first INSERT — a "xfer-<db id>" scheme would
        // need the id first (chicken-and-egg; this exact bug shipped once and
        // the NOT NULL constraint caught it on the first live transfer).
        // A UUID keeps the row insertable immediately and is globally unique,
        // which also future-proofs cross-system correlation.
        this.reference = "xfer-" + java.util.UUID.randomUUID();
    }

    /** Every state transition stamps updatedAt — cheap saga forensics. */
    public void transitionTo(TransferStatus newStatus, String reason) {
        this.status = newStatus;
        this.reason = reason;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Long getInitiatorCustomerId() {
        return initiatorCustomerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
