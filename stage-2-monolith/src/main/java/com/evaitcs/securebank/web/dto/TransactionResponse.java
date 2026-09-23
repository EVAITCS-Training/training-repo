package com.evaitcs.securebank.web.dto;

import com.evaitcs.securebank.domain.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

/** One ledger row as JSON (statement lines, recent activity). */
public record TransactionResponse(
        Long id,
        String type,          // DEBIT | CREDIT
        BigDecimal amount,    // always positive; type carries the direction
        String description,
        String reference,     // correlates the two halves of a transfer
        Instant occurredAt    // Jackson 3 writes Instants as ISO-8601 strings by default
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getType().name(), t.getAmount(),
                t.getDescription(), t.getReference(), t.getOccurredAt());
    }
}
