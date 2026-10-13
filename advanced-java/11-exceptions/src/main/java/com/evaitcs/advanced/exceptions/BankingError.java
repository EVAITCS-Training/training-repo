package com.evaitcs.advanced.exceptions;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The SEALED banking-error family — this module's design centerpiece.
 * (README "Custom exceptions the RIGHT way" and "Java 21 corner".)
 *
 * WHY A SEALED ABSTRACT *CLASS* (not interface): an interface cannot extend Throwable, so a
 * `catch (BankingError e)` clause would not compile. BankingError extends RuntimeException,
 * which makes the whole family throwable AND catchable by the shared supertype — you can
 * `catch (BankingError e)` and then switch on the concrete case. Sealing still CLOSES the
 * family, so the switch is exhaustive with no default.
 *
 * WHY SEALED (module 01, "Java 21 corner"): every permitted subclass is listed here, in this
 * one file, so the compiler knows the complete set. A pattern-matching switch over
 * BankingError can be EXHAUSTIVE with no default branch — add a fourth error tomorrow and
 * every switch over BankingError in the codebase stops compiling until someone handles it.
 * That is a compiler-enforced to-do list for error handling.
 *
 * WHY STRUCTURED CONTEXT, NOT STRING MESSAGES (the whole point):
 * Each error carries its data as TYPED fields (a TransferContext record, a reason String,
 * a limit BigDecimal). A caller reads error.context().shortfall() and gets a BigDecimal.
 * The anti-pattern — BankingErrorsDemo's LegacyInsufficientFunds — bakes the same numbers
 * into one formatted message string, forcing the caller to REGEX-PARSE its own exception
 * to recover a number it already had. Typed fields beat string parsing; the test
 * typedContextBeatsStringParsing proves you can compute the shortfall with no parsing.
 *
 * NOTE ON RECORDS: a record cannot extend a class, so an exception can never BE a record.
 * The structured context IS a record (TransferContext) that the exception CARRIES — that is
 * the Java-21 way to give an exception typed, immutable, component-style data.
 */
public sealed abstract class BankingError extends RuntimeException {

    /** Common constructor: every banking error is a RuntimeException with a human message. */
    protected BankingError(String message) {
        super(message);
    }

    /** Every banking error can name the account it happened to — the shared accessor. */
    public abstract String accountId();

    /**
     * The structured context of a money movement: which account, how much was attempted,
     * how much was actually available. A RECORD, so the components are typed accessors.
     * shortfall() is the number a caller actually wants — computed, never string-parsed.
     */
    public record TransferContext(String accountId, BigDecimal attempted, BigDecimal available) {

        /** attempted minus available, at scale 2 — the shortfall, as a BigDecimal. */
        public BigDecimal shortfall() {
            return attempted.subtract(available).setScale(2, RoundingMode.HALF_UP);
        }
    }

    /** Thrown when the source account cannot cover the attempted amount. final => a valid sealed subclass. */
    public static final class InsufficientFunds extends BankingError {

        private final TransferContext context;

        public InsufficientFunds(TransferContext context) {
            // The message is for HUMANS (logs, a screen). The context record is for CODE.
            super("Account %s has %s available but %s was requested"
                    .formatted(context.accountId(), context.available(), context.attempted()));
            this.context = context;
        }

        @Override
        public String accountId() {
            return context.accountId();
        }

        /** Typed access to the numbers — a caller computes shortfall() without parsing anything. */
        public TransferContext context() {
            return context;
        }
    }

    /** Thrown when the account is frozen (legal hold, reported lost, ...). Carries a reason. */
    public static final class AccountFrozen extends BankingError {

        private final String accountId;
        private final String reason;

        public AccountFrozen(String accountId, String reason) {
            super("Account %s is frozen: %s".formatted(accountId, reason));
            this.accountId = accountId;
            this.reason = reason;
        }

        @Override
        public String accountId() {
            return accountId;
        }

        public String reason() {
            return reason;
        }
    }

    /** Thrown when a single transfer would push the day's total past the account's limit. */
    public static final class DailyLimitExceeded extends BankingError {

        private final String accountId;
        private final BigDecimal limit;
        private final BigDecimal attempted;

        public DailyLimitExceeded(String accountId, BigDecimal limit, BigDecimal attempted) {
            super("Account %s daily limit %s exceeded by attempted %s"
                    .formatted(accountId, limit, attempted));
            this.accountId = accountId;
            this.limit = limit;
            this.attempted = attempted;
        }

        @Override
        public String accountId() {
            return accountId;
        }

        public BigDecimal limit() {
            return limit;
        }

        public BigDecimal attempted() {
            return attempted;
        }
    }
}
