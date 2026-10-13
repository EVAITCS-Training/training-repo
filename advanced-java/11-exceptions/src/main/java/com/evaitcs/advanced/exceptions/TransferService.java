package com.evaitcs.advanced.exceptions;

import java.math.BigDecimal;

/**
 * The SecureBank transfer service — the deep call chain this module throws from.
 * (README "What throw actually does at runtime: stack unwinding".)
 *
 * WHY THREE NESTED METHODS: StackUnwindingDemo and the stackUnwindingPopsFramesInOrder
 * test need a THROW that happens several frames below the CATCH, so the popped frames are
 * visible. The call chain is deliberately tall and each frame has a distinct name:
 *
 *   transfer  ->  authorize  ->  checkBalance  ->  throw
 *   (shallow)                                   (deepest)
 *
 * When checkBalance throws, the exception's StackTraceElement[] snapshots exactly these
 * frames, DEEPEST FIRST: checkBalance, authorize, transfer, then whoever called transfer.
 * That array IS the list of frames the JVM is about to pop while unwinding to the catch.
 *
 * The failure is a typed BankingError.InsufficientFunds carrying a structured context —
 * the design centerpiece — not a string-formatted RuntimeException. Money is BigDecimal.
 */
public class TransferService {

    /**
     * Public entry point: the OUTERMOST business frame. Returns the two rebuilt accounts
     * on success (records are immutable, so a transfer produces NEW Account objects).
     */
    public Account[] transfer(Account from, Account to, BigDecimal amount) {
        authorize(from, amount);                          // descend one frame
        return new Account[] { from.minus(amount), to.plus(amount) };
    }

    /** Middle frame: in a real bank this checks holds, limits, KYC. Here it just descends. */
    private void authorize(Account from, BigDecimal amount) {
        checkBalance(from, amount);                       // descend one more frame
    }

    /** DEEPEST frame: the balance guard. This is where the throw happens. */
    private void checkBalance(Account from, BigDecimal amount) {
        if (!from.canCover(amount)) {
            // Structured context (typed fields), so the catcher never parses the message:
            throw new BankingError.InsufficientFunds(
                    new BankingError.TransferContext(from.id(), amount, from.balance()));
        }
    }
}
