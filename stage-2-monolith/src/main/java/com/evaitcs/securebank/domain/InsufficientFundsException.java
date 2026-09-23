package com.evaitcs.securebank.domain;

import java.math.BigDecimal;

/**
 * DOMAIN EXCEPTION — thrown when a business rule is violated (as opposed to
 * a technical failure like "database unreachable").
 *
 * CONNECTION: web/GlobalExceptionHandler catches this and converts it into
 * an HTTP 400 response with an RFC 7807 "ProblemDetail" JSON body. That is
 * the modern Spring way to report errors: machines get a standardized shape,
 * humans get a readable detail message.
 *
 * WHY A CUSTOM EXCEPTION (not IllegalArgumentException)?
 * The type itself carries meaning. The handler can map InsufficientFunds ->
 * 400 and ResourceNotFound -> 404 without inspecting message strings.
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
