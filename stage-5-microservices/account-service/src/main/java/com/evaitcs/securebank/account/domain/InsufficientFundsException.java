package com.evaitcs.securebank.account.domain;

import java.math.BigDecimal;

/**
 * Business-rule violation — mapped to HTTP 400 problem+json by
 * web/GlobalExceptionHandler. In the saga, payment-service receives that 400
 * and translates it into TransferFailed + a client-facing error: exceptions
 * cross SERVICE boundaries as HTTP statuses, not as Java types.
 */
public class InsufficientFundsException extends RuntimeException {

    private final Long accountId;
    private final BigDecimal available;
    private final BigDecimal attempted;

    public InsufficientFundsException(Long accountId, BigDecimal available, BigDecimal attempted) {
        super("Account %d has %s available but %s was requested"
                .formatted(accountId, available, attempted));
        this.accountId = accountId;
        this.available = available;
        this.attempted = attempted;
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getAttempted() {
        return attempted;
    }
}
