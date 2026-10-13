package com.evaitcs.corejava.refs;

/**
 * int overflow: the classic SILENT bug.
 *
 * THE TRAP: an int is a fixed 32-bit slot. Add 1 past the top and the bits just roll over to
 * the bottom — no exception, no warning, no log line. The number quietly becomes negative and
 * your program keeps going with garbage. This is the #1 reason counting money in int/long cents
 * eventually bites someone.
 */
public class OverflowDemo {

    public static void main(String[] args) {
        banner("1. MAX_VALUE + 1 wraps to MIN_VALUE — silently");
        int max = Integer.MAX_VALUE;
        System.out.println("Integer.MAX_VALUE            = " + max);
        System.out.println("Integer.MAX_VALUE + 1        = " + (max + 1) + "   <-- rolled over, no error thrown");
        System.out.println("Integer.MIN_VALUE            = " + Integer.MIN_VALUE);
        System.out.println("(MAX_VALUE + 1 == MIN_VALUE) = " + ((max + 1) == Integer.MIN_VALUE));

        banner("2. A realistic bank bug: counting CENTS in an int");
        // $21,000,000.00 expressed in cents is 2,100,000,000 — already close to Integer.MAX_VALUE.
        int cents = 2_100_000_000;
        cents += 100_000_000; // the bank credits another $1,000,000.00 (in cents) -> overflows past the top
        System.out.println("cents after a big deposit    = " + cents + "   <-- went NEGATIVE; money vanished");

        banner("3. Two fixes");
        // Fix A: use a WIDER type. Cast BEFORE the math so the addition happens in 64-bit.
        long wide = (long) Integer.MAX_VALUE + 1;
        System.out.println("(long) MAX_VALUE + 1         = " + wide + "   <-- long has room, no wrap");
        // Fix B: Math.addExact throws instead of wrapping — fail LOUD, not silent (added in Java 8).
        try {
            int boom = Math.addExact(Integer.MAX_VALUE, 1);
            System.out.println("this line never runs: " + boom);
        } catch (ArithmeticException e) {
            System.out.println("Math.addExact(MAX_VALUE, 1)  threw " + e.getClass().getSimpleName() + " (loud, catchable)");
        }
        System.out.println();
        System.out.println("Rule: for MONEY don't even trust long cents for big totals — use BigDecimal (next demo).");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
