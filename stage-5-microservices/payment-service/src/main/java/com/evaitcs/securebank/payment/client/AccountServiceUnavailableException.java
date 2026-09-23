package com.evaitcs.securebank.payment.client;

/**
 * account-service could not be reached or failed technically (down, timeout,
 * circuit OPEN, 5xx). Retries already happened (@Retry) or were skipped
 * because the circuit breaker is protecting the failing service. The saga
 * treats this as "unknown/failed" — and if the debit had already succeeded,
 * it must COMPENSATE.
 */
public class AccountServiceUnavailableException extends RuntimeException {

    public AccountServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountServiceUnavailableException(String message) {
        super(message);
    }
}
