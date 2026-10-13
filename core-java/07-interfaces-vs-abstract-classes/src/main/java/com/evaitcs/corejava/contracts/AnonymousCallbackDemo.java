package com.evaitcs.corejava.contracts;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Claim #6 of the module: pre-Java-8, the ONLY way to hand behavior to a method
 * was to wrap it in an OBJECT — an anonymous inner class implementing a
 * one-method (callback) interface. Five lines of ceremony per idea.
 *
 * This is the "before" picture that java8-features/01-lambdas deletes. Both
 * anonymous classes below carry the forward pointer at the exact spot.
 * Only two anonymous classes in the whole module — the pattern, not a habit.
 */
public class AnonymousCallbackDemo {

    /**
     * The callee: settle any amount, add "the fee" — but WHAT the fee is stays
     * the CALLER's decision, passed in as a FeeCalculator. Classic strategy via
     * callback. totalOf does not know (or care) which fee policy arrives.
     */
    public static BigDecimal totalOf(BigDecimal amount, FeeCalculator feePolicy) {
        BigDecimal fee = feePolicy.feeFor(amount);   // THE CALLBACK — the callee calls back in
        System.out.println("   amount " + amount + " + fee " + fee);
        return amount.add(fee).setScale(2, RoundingMode.HALF_UP);
    }

    public static void main(String[] args) {

        banner("1. Callback #1 — the FLAT fee policy, as an anonymous inner class");

        // `new FeeCalculator() { ... }` = "one object of a class with no name that
        // implements FeeCalculator, built right here". It compiles to its own file
        // (AnonymousCallbackDemo$1.class) — a whole class for one expression.
        // java8-features/01-lambdas: this becomes ONE ARROW:  a -> new BigDecimal("1.50")
        FeeCalculator flatFee = new FeeCalculator() {
            @Override
            public BigDecimal feeFor(BigDecimal amount) {
                return new BigDecimal("1.50");
            }
        };

        BigDecimal amount = new BigDecimal("250.00");
        BigDecimal total = totalOf(amount, flatFee);
        System.out.println("   flat policy   -> total " + total);
        System.out.println("   anonymous class compiled to: " + flatFee.getClass().getName());

        banner("2. Callback #2 — the PERCENTAGE policy, same interface, different object");

        // The callee totalOf() is UNCHANGED — that is the power of the contract:
        // new behavior, zero edits to existing code.
        // java8-features/01-lambdas: this becomes ONE ARROW:  a -> a.multiply(RATE)...
        FeeCalculator twoPercent = new FeeCalculator() {
            @Override
            public BigDecimal feeFor(BigDecimal amount) {
                return amount.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
            }
        };

        total = totalOf(amount, twoPercent);
        System.out.println("   2% policy     -> total " + total);
        System.out.println("   anonymous class compiled to: " + twoPercent.getClass().getName());

        banner("3. What we just paid for the ceremony");

        System.out.println("Two ideas ('1.50 flat', '2% of amount') cost ~5 lines and 2 extra .class files each.");
        System.out.println("Everything except the ONE expression is noise the compiler already knew:");
        System.out.println("  new FeeCalculator() { @Override public BigDecimal feeFor(BigDecimal amount) { ... } }");
        System.out.println("Java 8 keeps the INTERFACE (still the target type!) and deletes the wrapping:");
        System.out.println("  FeeCalculator flatFee = a -> new BigDecimal(\"1.50\");");
        System.out.println("=> java8-features/01-lambdas picks up exactly here.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
