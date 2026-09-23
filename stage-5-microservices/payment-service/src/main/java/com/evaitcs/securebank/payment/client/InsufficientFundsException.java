package com.evaitcs.securebank.payment.client;

/**
 * account-service refused the movement for BUSINESS reasons (e.g. insufficient
 * funds — it answered HTTP 400 problem+json).
 *
 * THE DISTINCTION THAT DRIVES THE SAGA:
 *   business refusal  -> do NOT retry, do NOT compensate (nothing happened),
 *                        fail the transfer, tell the client why.
 *   technical failure (AccountServiceUnavailableException) -> retry first
 *   (Resilience4j), then give up; if it happens AFTER the debit succeeded,
 *   COMPENSATE.
 * Conflating these two is how money goes missing: retrying a business refusal
 * spams the ledger; compensating one invents money that never moved.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
