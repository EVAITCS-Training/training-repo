package com.evaitcs.advanced.gof;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Demo 2 — STRATEGY: one algorithm slot, interchangeable implementations.
 * (README section "Strategy: the one you already wrote in core-java".)
 *
 * Problem: the fee depends on the product — flat, percentage, or free above a
 * threshold — and products change every quarter. Solution: put the fee RULE behind
 * one interface and let the calculator receive it as a parameter.
 *
 * You already wrote this pattern: a Comparator IS a Strategy (core-java 10 —
 * Collections.sort keeps its skeleton, you swap the ordering algorithm).
 *
 * Java 21 note: the classic GoF shape is 1 interface + 3 classes + a context object.
 * Here the family is a SEALED interface of RECORDS and the calculator is one
 * exhaustive pattern-matching switch — same pattern, a fraction of the ceremony
 * (module 01 proved sealed + switch is compiler-checked exhaustiveness).
 */
public class FeeStrategyDemo {

    /**
     * The Strategy family. SEALED, so the switch in FeeCalculator knows every
     * implementation: add a fourth policy tomorrow and the calculator stops
     * compiling until it handles the new case (module 01's exhaustiveness).
     */
    public sealed interface FeePolicy permits FlatFee, PercentFee, FreeOverThreshold { }

    /** Always the same flat fee, whatever the amount. */
    public record FlatFee(BigDecimal flat) implements FeePolicy { }

    /** A rate applied to the amount; scale-2 money, HALF_UP like every bank rounding. */
    public record PercentFee(BigDecimal rate) implements FeePolicy { }

    /** Free from `threshold` upward; below it, a flat fallback fee applies. */
    public record FreeOverThreshold(BigDecimal threshold, BigDecimal otherwiseFee) implements FeePolicy { }

    /**
     * The Context: holds NO rule of its own. The policy arrives as a parameter,
     * so the swap happens at the CALL SITE, at runtime, with zero subclasses.
     */
    public static final class FeeCalculator {

        public BigDecimal feeFor(FeePolicy policy, BigDecimal amount) {
            // Record patterns deconstruct each strategy; no default branch exists
            // because the sealed family is exhaustively covered.
            return switch (policy) {
                case FlatFee(var flat) -> flat.setScale(2, RoundingMode.HALF_UP);
                case PercentFee(var rate) -> amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                case FreeOverThreshold(var threshold, var otherwise) ->
                        amount.compareTo(threshold) >= 0
                                ? new BigDecimal("0.00")
                                : otherwise.setScale(2, RoundingMode.HALF_UP);
            };
        }
    }

    public static void main(String[] args) {
        banner("1. One calculator, three interchangeable fee algorithms");

        FeeCalculator calculator = new FeeCalculator();          // built ONCE...
        BigDecimal amount = new BigDecimal("250.00");

        System.out.println("step 1: amount                     = " + amount.toPlainString());
        BigDecimal flat = calculator.feeFor(new FlatFee(new BigDecimal("2.50")), amount);
        System.out.println("step 2: FlatFee(2.50)              -> fee = " + flat.toPlainString());
        BigDecimal percent = calculator.feeFor(new PercentFee(new BigDecimal("0.01")), amount);
        System.out.println("step 3: PercentFee(0.01)           -> fee = " + percent.toPlainString()
                + "   (250.00 * 1%, scale 2 HALF_UP)");
        FreeOverThreshold free = new FreeOverThreshold(new BigDecimal("200.00"), new BigDecimal("1.00"));
        BigDecimal waived = calculator.feeFor(free, amount);
        System.out.println("step 4: FreeOverThreshold(200/1)   -> fee = " + waived.toPlainString()
                + "   (250.00 >= 200.00, so free)");
        BigDecimal belowThreshold = calculator.feeFor(free, new BigDecimal("150.00"));
        System.out.println("step 5: same policy, amount 150.00 -> fee = " + belowThreshold.toPlainString()
                + "   (below threshold, fallback applies)");
        System.out.println("verdict: the calculator never changed — only the policy did. That swap IS the pattern.");

        banner("2. Where you already met it: a Comparator IS a Strategy");

        System.out.println("core-java 10: Collections.sort(list, comparator).");
        System.out.println("The sort skeleton (merging, swapping) is fixed in the JDK;");
        System.out.println("the ORDERING algorithm arrives as a parameter — exactly this shape.");
        System.out.println("Since Java 8 the strategy is often just a lambda: Comparator.comparing(Account::id).");

        banner("3. The Java 21 shrink: same pattern, less ceremony");

        System.out.println("classic GoF (1994): interface FeePolicy + class FlatFee + class PercentFee");
        System.out.println("                    + class FreeOverThreshold + a context class  = 5 files.");
        System.out.println("Java 21 here    : sealed interface + 3 records + 1 exhaustive switch = 1 screen.");
        System.out.println("verdict: the pattern did not change — records and sealed types removed its boilerplate.");

        banner("4. Trap: when NOT to use Strategy");

        System.out.println("If the 'variants' are just different NUMBERS (fee 2.50 vs fee 3.00),");
        System.out.println("you need a constant or a config row — not a new type per number.");
        System.out.println("Strategy pays off when the ALGORITHM differs, not when only data differs.");

        banner("5. In one sentence");
        System.out.println("Strategy turns 'which algorithm?' into a parameter, so the code that");
        System.out.println("USES the algorithm never has to know which one it got.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
