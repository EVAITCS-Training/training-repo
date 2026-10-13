package com.evaitcs.evolution.app.era3;

/**
 * ERA 3 — the domain exception for "that account does not exist".
 *
 * WHY it exists as its own type: BankApiAdvice must turn failures into HTTP responses,
 * and it can only be as precise as the exception vocabulary the service layer speaks.
 * Spring's JdbcTemplate already translates "no row" into EmptyResultDataAccessException,
 * but that is an INFRASTRUCTURE word; the boundary to HTTP should speak a DOMAIN word
 * (stage-2 does exactly this with its ResourceNotFoundException). The advice maps this
 * to 404 + problem+json in one place — no controller ever mentions a status code.
 */
public class AccountNotFoundException extends RuntimeException {

    private final long accountId;

    public AccountNotFoundException(long accountId) {
        super("no account with id " + accountId);
        this.accountId = accountId;
    }

    public long accountId() {
        return accountId;
    }
}
