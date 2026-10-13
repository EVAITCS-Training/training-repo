package com.evaitcs.corejava.ordering;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Demo 5 — the SUBTRACTION TRAP: `return a - b` overflows and the sign flips.
 *
 * The pattern is seductive because it WORKS on small numbers (ages). The same
 * pattern on epoch-millis timestamps or money-as-cents produces differences bigger
 * than Integer.MAX_VALUE; the (int) cast throws away the high bits — INCLUDING the
 * sign — and the comparator starts lying. No exception, no warning: just a wrong
 * order, printed below as evidence.
 */
public class SubtractionTrapDemo {

    public static void main(String[] args) {
        banner("1. Small numbers: subtraction 'works' — that is the bait");

        List<Customer> customers = new ArrayList<Customer>(Arrays.asList(
                new Customer("Dana", 41), new Customer("Ana", 25), new Customer("Bo", 34)));
        Collections.sort(customers, new ByAgeSubtraction());
        System.out.println("customers by (age1 - age2): " + customers);
        System.out.println("Correct! Ages are small, every difference fits in an int. So far...");

        banner("2. Real bank numbers: the SAME pattern breaks");

        // Two transactions ~24.9 DAYS apart. Their long difference exceeds Integer.MAX_VALUE.
        Transaction early = new Transaction("T-1001", "Opening balance", new BigDecimal("0.01"), 1_697_850_000_000L);
        Transaction late  = new Transaction("T-2001", "Payroll",         new BigDecimal("2450.00"), 1_700_000_000_000L);

        long realDiff = late.getTimestampMillis() - early.getTimestampMillis();
        int castDiff = (int) realDiff;
        System.out.println("late.ts - early.ts as long  = " + realDiff + "   (late is NEWER: positive, correct)");
        System.out.println("...after the (int) cast     = " + castDiff + "  (NEGATIVE — the sign FLIPPED)");
        System.out.println("Integer.MAX_VALUE           = " + Integer.MAX_VALUE + "   <- the difference did not fit");

        List<Transaction> broken = new ArrayList<Transaction>(Arrays.asList(late, early));
        Collections.sort(broken, new ByTimestampSubtractionBroken());
        System.out.println("sorted with (int)(t1.ts - t2.ts):");
        for (Transaction t : broken) {
            System.out.println("  " + t.getId() + " ts=" + t.getTimestampMillis());
        }
        System.out.println("WRONG ORDER as evidence: the NEWER transaction sorted first.");
        System.out.println("The comparator said late < early — and TimSort believed it.");

        banner("3. Cents break the same way");

        long bigCents = new BigDecimal("25000000.00").movePointRight(2).longValue();   // $25M in cents
        long smallCents = new BigDecimal("1.00").movePointRight(2).longValue();        // $1 in cents
        long centsDiff = bigCents - smallCents;
        System.out.println("$25,000,000.00 - $1.00 in cents = " + centsDiff + " (long, positive)");
        System.out.println("...as int = " + (int) centsDiff + " (negative!) — the big deposit now sorts BEFORE $1.");

        banner("4. The correct idioms — pick one, never subtract");

        List<Transaction> fixed = new ArrayList<Transaction>(Arrays.asList(late, early));
        Collections.sort(fixed, new ByTimestamp());        // uses Long.compare internally
        System.out.println("sorted with Long.compare(ts1, ts2):");
        for (Transaction t : fixed) {
            System.out.println("  " + t.getId() + " ts=" + t.getTimestampMillis());
        }
        System.out.println("1. Long.compare(a, b) / Integer.compare(a, b)   (Java 7+, says it best)");
        System.out.println("2. a < b ? -1 : (a == b ? 0 : 1)                (works on every Java ever)");
        System.out.println("3. delegate to compareTo: String, BigDecimal, ... (they already got it right)");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
