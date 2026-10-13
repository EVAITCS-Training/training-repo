package com.evaitcs.advanced.exceptions;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Demo 5 — CUSTOM EXCEPTIONS THE RIGHT WAY: a sealed, typed banking-error family.
 * (README "Custom exceptions the RIGHT way" — the module's design centerpiece.)
 *
 * The anti-pattern: bake every number into one formatted MESSAGE STRING. The caller who
 * needs the shortfall must REGEX-PARSE its own exception — brittle, and it re-derives a
 * number the thrower already had in hand.
 *
 * The pattern (BankingError, this module's sealed interface): each error carries its data
 * as TYPED fields (a TransferContext record, a reason, a limit). The caller reads
 * error.context().shortfall() and gets a BigDecimal. And because the family is SEALED, a
 * pattern-matching switch over BankingError is EXHAUSTIVE with no default — the compiler
 * knows every case (module 01, "Java 21 corner"). Add a fourth error tomorrow and this
 * switch stops compiling until someone handles it: a compiler-enforced to-do list.
 *
 * This is EXACTLY the shape stage-2 turns into an RFC 7807 ProblemDetail at the HTTP edge.
 */
public class BankingErrorsDemo {

    public static void main(String[] args) {
        banner("1. ANTI-PATTERN — a message-only exception forces the caller to PARSE STRINGS");

        LegacyInsufficientFundsException legacy =
                new LegacyInsufficientFundsException("ACC-1001", new BigDecimal("50.00"), new BigDecimal("80.00"));
        System.out.println("step 1: the ONLY thing the legacy exception carries is getMessage():");
        System.out.println("        \"" + legacy.getMessage() + "\"");
        // To recover the shortfall, the caller must parse the human sentence with a regex.
        Matcher m = Pattern.compile("has (\\S+) available but (\\S+) was requested").matcher(legacy.getMessage());
        if (m.find()) {
            BigDecimal available = new BigDecimal(m.group(1));
            BigDecimal attempted = new BigDecimal(m.group(2));
            System.out.println("step 2: caller regex-parsed available=" + available + ", attempted=" + attempted
                    + ", shortfall=" + attempted.subtract(available));
        }
        System.out.println("verdict: fragile — change one word of the message and the parsing breaks. The thrower");
        System.out.println("        HAD those BigDecimals; the caller had to dig them back out of a String.");

        banner("2. PATTERN — a typed context record: read the number, never parse it");

        BankingError.InsufficientFunds typed = new BankingError.InsufficientFunds(
                new BankingError.TransferContext("ACC-1001", new BigDecimal("80.00"), new BigDecimal("50.00")));
        System.out.println("step 1: typed.context().attempted() = " + typed.context().attempted() + "   (a BigDecimal field)");
        System.out.println("step 2: typed.context().available() = " + typed.context().available() + "   (a BigDecimal field)");
        System.out.println("step 3: typed.context().shortfall() = " + typed.context().shortfall() + "   (computed, no parsing)");
        System.out.println("verdict: the caller gets the shortfall as a BigDecimal by calling an accessor. Nothing to parse.");

        banner("3. SEALED + pattern-matching switch: exhaustive customer messages, no default");

        // Three failures from the closed BankingError family. One switch turns each into a
        // customer-facing sentence. There is NO default branch: the sealed family is complete,
        // so the compiler proves the switch is exhaustive.
        List<BankingError> failures = List.of(
                new BankingError.InsufficientFunds(
                        new BankingError.TransferContext("ACC-1001", new BigDecimal("80.00"), new BigDecimal("50.00"))),
                new BankingError.AccountFrozen("ACC-2002", "reported lost"),
                new BankingError.DailyLimitExceeded("ACC-3003", new BigDecimal("5000.00"), new BigDecimal("7500.00")));

        for (BankingError error : failures) {
            System.out.println("step: " + error.getClass().getSimpleName() + " -> " + customerMessage(error));
        }
        System.out.println("verdict: exhaustive over a SEALED family = the compiler is your error-handling checklist.");
        System.out.println("        Add a fourth BankingError tomorrow and every switch like this one FAILS TO COMPILE");
        System.out.println("        until handled — that is module 01's exhaustiveness, applied to failures.");

        banner("4. The domain actually throwing it: TransferService, caught by the family type");

        Account checking = new Account("ACC-1001", new BigDecimal("50.00"));
        Account savings = new Account("ACC-2002", new BigDecimal("0.00"));
        try {
            new TransferService().transfer(checking, savings, new BigDecimal("80.00"));
        } catch (BankingError error) {                 // catch by the SEALED SUPERTYPE...
            System.out.println("step 1: CAUGHT -> " + error.getClass().getSimpleName() + " (caught as BankingError)");
            System.out.println("step 2: accountId() from the shared accessor = " + error.accountId());
            System.out.println("step 3: customer message = " + customerMessage(error));   // ...then switch on the case
            System.out.println("verdict: throw typed, catch by family, switch on the concrete case — the whole pattern.");
        }

        banner("5. In one sentence");
        System.out.println("A sealed family of typed exceptions lets the caller switch on the CASE and read FIELDS,");
        System.out.println("instead of parsing one string — and the compiler checks the switch handles every case.");
    }

    /**
     * The exhaustive, customer-facing translation of the sealed BankingError family.
     * NO default branch: the switch covers every permitted subtype, so javac accepts it as
     * exhaustive. This is the method stage-2 wraps into an RFC 7807 ProblemDetail response.
     */
    static String customerMessage(BankingError error) {
        return switch (error) {
            case BankingError.InsufficientFunds e ->
                    "Declined: account " + e.accountId() + " is short by " + e.context().shortfall() + ".";
            case BankingError.AccountFrozen e ->
                    "Declined: account " + e.accountId() + " is frozen (" + e.reason() + "). Please contact support.";
            case BankingError.DailyLimitExceeded e ->
                    "Declined: account " + e.accountId() + " would exceed its daily limit of " + e.limit() + ".";
        };
    }

    /**
     * The ANTI-PATTERN: a message-only exception. All structure is baked into a String, so a
     * caller who needs a number must parse the sentence. Kept here ONLY to contrast with the
     * typed BankingError family above.
     */
    static final class LegacyInsufficientFundsException extends RuntimeException {
        LegacyInsufficientFundsException(String accountId, BigDecimal available, BigDecimal attempted) {
            super("Account " + accountId + " has " + available + " available but " + attempted + " was requested");
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
