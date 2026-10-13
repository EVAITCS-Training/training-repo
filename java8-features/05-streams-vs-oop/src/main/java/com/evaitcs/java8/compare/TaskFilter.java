package com.evaitcs.java8.compare;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TASK 1 — "all deposits over 100".
 *
 * The warm-up. Both versions are short, which is exactly why we start here:
 * with only a few lines on the projector, the class can see the STRUCTURAL
 * difference instead of drowning in code.
 *
 * TEACHING POINT: the imperative loop mixes WHAT with HOW.
 *   WHAT we want: "deposits over 100"        — one business rule.
 *   HOW to get it: create list, iterate, test, add, return — four mechanical steps.
 * The stream version lets the WHAT be the whole method body; the HOW is library code
 * that has been tested by millions of users instead of re-written (and re-bugged) by us.
 */
public class TaskFilter {

    private static final BigDecimal THRESHOLD = new BigDecimal("100");

    /*
     * LOOP ANATOMY (number the lines on the projector — students will recognize
     * that only line (3) is the actual question; the rest is ceremony):
     *
     *   (1) List<Transaction> result = new ArrayList<>();  <- create the collection BY HAND
     *   (2) for (Transaction t : transactions) {           <- iteration bookkeeping BY HAND
     *   (3)     if (isDeposit && amount > 100)             <- THE BUSINESS RULE (finally!)
     *   (4)         result.add(t);                         <- accumulation BY HAND
     *   (5) }
     *   (6) return result;
     *
     * Trap to name out loud: forget line (1) and nothing compiles; write `>=` instead
     * of `>` in line (3) and it compiles fine and is silently wrong. The loop gives
     * you many places to make a mistake that has nothing to do with banking.
     */
    public static List<Transaction> imperative(List<Transaction> transactions) {
        List<Transaction> result = new ArrayList<>();          // (1)
        for (Transaction t : transactions) {                   // (2)
            if (t.isDeposit()                                  // (3)
                    && t.getAmount().compareTo(THRESHOLD) > 0) { //     BigDecimal: compareTo, NEVER ==
                result.add(t);                                 // (4)
            }
        }
        return result;                                         // (6)
    }

    /*
     * Same anatomy, mapped one-to-one:
     *   the if-condition (3) -> filter(predicate)   "keep elements that pass"
     *   the add + return (4)(6) -> collect(toList()) "gather survivors into a list"
     *   the for (2) -> GONE. That is internal iteration: we never touch an index
     *   or a cursor; the stream drives itself and calls OUR lambda per element.
     */
    public static List<Transaction> withStreams(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.isDeposit())
                .filter(t -> t.getAmount().compareTo(THRESHOLD) > 0)
                .collect(Collectors.toList()); // Java 8: collect(toList()), NOT Stream.toList()
    }

    public static void main(String[] args) {
        List<Transaction> transactions = SampleBank.transactions();

        List<Transaction> imperative = imperative(transactions);
        List<Transaction> streams = withStreams(transactions);

        System.out.println("QUESTION: which transactions are deposits over 100.00?");
        System.out.println();
        System.out.println("IMPERATIVE (for + if + add):");
        for (Transaction t : imperative) {
            System.out.println("  " + t);
        }
        System.out.println();
        System.out.println("STREAMS (filter + collect):");
        for (Transaction t : streams) {
            System.out.println("  " + t);
        }
        System.out.println();
        System.out.println("MATCH? " + imperative.equals(streams));
        System.out.println("POINT: the loop mixed WHAT (the rule) with HOW (iterate/collect).");
        System.out.println("       The stream version is ONLY the what.");
    }
}
