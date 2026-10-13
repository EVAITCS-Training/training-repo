package com.evaitcs.java8.lambdas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Behavior algebra: once behavior is a value, values can be COMBINED.
 *
 * The functional interfaces ship with default methods (see module 02 — that's exactly
 * how these exist on an interface) that build big behavior from small behavior:
 *
 *   Predicate : and, or, negate        (plus static isEqual, and in later Java versions more)
 *   Function  : andThen, compose       (plus static identity)
 *   Consumer  : andThen
 *   Comparator: thenComparing, reversed, nullsFirst...
 */
public class ComposingDemo {

    // Small, named, single-purpose predicates — the Lego bricks.
    static final Predicate<Transaction> isDeposit = Transaction::isDeposit;
    static final Predicate<Transaction> isLarge =
            t -> t.getAmount().compareTo(new BigDecimal("500.00")) > 0;

    public static void main(String[] args) {
        System.out.println("=== Predicate composition ===");
        // and / or / negate return NEW predicates; nothing is mutated, nothing is subclassed.
        Predicate<Transaction> largeDeposit  = isDeposit.and(isLarge);
        Predicate<Transaction> smallOrWithdrawal = largeDeposit.negate(); // De Morgan for free
        Predicate<Transaction> anyDepositOrLarge = isDeposit.or(isLarge);

        for (Transaction t : Transaction.sampleStatement()) {
            System.out.printf("   %s | largeDeposit=%-5s notLargeDeposit=%-5s depositOrLarge=%s%n",
                    t, largeDeposit.test(t), smallOrWithdrawal.test(t), anyDepositOrLarge.test(t));
        }

        System.out.println();
        System.out.println("=== Function composition: andThen vs compose (order matters!) ===");
        Function<BigDecimal, BigDecimal> addFee =
                a -> a.multiply(new BigDecimal("1.015"));                 // amount + 1.5% fee
        Function<BigDecimal, BigDecimal> round =
                a -> a.setScale(2, RoundingMode.HALF_UP);

        // andThen: this FIRST, then the argument.  compose: the argument FIRST, then this.
        Function<BigDecimal, BigDecimal> feeThenRound = addFee.andThen(round); // (round ∘ addFee)
        Function<BigDecimal, BigDecimal> roundThenFee = addFee.compose(round); // (addFee ∘ round)

        BigDecimal raw = new BigDecimal("86.404");
        System.out.println("   raw amount                : " + raw);
        System.out.println("   addFee.andThen(round)     : " + feeThenRound.apply(raw) + "  <- fee, THEN round (what we want)");
        System.out.println("   addFee.compose(round)     : " + roundThenFee.apply(raw) + "  <- round, THEN fee (rounding is undone!)");
        System.out.println("   mnemonic: andThen reads left-to-right; compose reads right-to-left (like math).");

        System.out.println();
        System.out.println("=== Why this matters for the Stream API ===");
        System.out.println("   filter(isDeposit.and(isLarge))  is a pipeline stage built from two");
        System.out.println("   named conditions — module 04 picks this up with streams.");
    }
}
