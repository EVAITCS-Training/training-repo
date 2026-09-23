package com.evaitcs.securebank.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * POST /api/transfers body.
 *
 * Notice what is ABSENT: no "fromCustomerId". The initiator's identity comes
 * from the verified JWT, never from the request body — a client-supplied
 * "from" field would let anyone debit anyone (OWASP Broken Access Control).
 * The service cross-checks that fromAccountId belongs to the token's customer.
 */
public record TransferRequest(
        @NotNull Long fromAccountId,
        @NotNull Long toAccountId,

        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount
) {
}
