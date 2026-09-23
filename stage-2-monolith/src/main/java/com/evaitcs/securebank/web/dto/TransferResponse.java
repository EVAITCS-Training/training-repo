package com.evaitcs.securebank.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transfer receipt. `reference` is the UUID shared by both ledger rows —
 * give it to support and they can find both halves of the movement
 * (SELECT * FROM transactions WHERE reference = ?). Real payment APIs return
 * exactly this kind of correlation id.
 */
public record TransferResponse(
        String reference,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        Instant occurredAt
) {
}
