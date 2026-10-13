package com.evaitcs.java8.compare;

import java.math.BigDecimal;
import java.util.List;

/**
 * TASK 7 — "is there ANY suspicious transaction (amount > 2000)?".
 *
 * The result is a boolean, and the task has a special property: as soon as we find
 * ONE match we can stop looking. The imperative version expresses that with an early
 * return; the stream version gets it for free.
 *
 * TEACHING POINT: short-circuiting is built in. anyMatch() stops the moment the
 * predicate returns true — it does NOT scan the rest of the list. The loop has to
 * spell that out with `return true` in the middle; the stream has it as a guarantee.
 */
public class TaskAnyMatch {

    private static final BigDecimal SUSPICIOUS = new BigDecimal("2000");

    /*
     * LOOP ANATOMY — early return:
     *
     *   (1) for (Transaction t : transactions) {        <- iteration, by hand
     *   (2)     if (t.getAmount().compareTo(SUSPICIOUS) > 0) {  <- the test
     *   (3)         return true;                        <- SHORT-CIRCUIT: stop right here.
     *   (4)     }
     *   (5) }
     *   (6) return false;                               <- nothing matched
     *
     * This one is already clean, so name the SUBTLER trap: the "flag variable" variant
     * students often write instead —
     *
     *     boolean found = false;
     *     for (...) { if (...) found = true; }   // keeps scanning AFTER finding one!
     *     return found;
     *
     * That version is correct but WASTEFUL: it checks every element even after the answer
     * is known. The early `return true` in (3) is what makes it stop early — and it's
     * easy to forget, especially once the loop body grows. The stream can't forget.
     */
    public static boolean imperative(List<Transaction> transactions) {
        for (Transaction t : transactions) {                   // (1)
            if (t.getAmount().compareTo(SUSPICIOUS) > 0) {     // (2)
                return true;                                   // (3) short-circuit by hand
            }
        }
        return false;                                          // (6)
    }

    /*
     * Same anatomy, mapped one-to-one:
     *   the test (2)     -> anyMatch(predicate)  "does AT LEAST ONE element pass?"
     *   the early return (3) and the fallthrough (6) -> BUILT IN. anyMatch is a
     *     short-circuiting terminal operation: it returns true on the first match and
     *     never pulls another element from the source. Same for allMatch and noneMatch.
     *   the for (1) -> GONE (internal iteration).
     *
     * One line that says exactly the question, and the short-circuit behavior is a
     * documented guarantee rather than something we remembered to code.
     */
    public static boolean withStreams(List<Transaction> transactions) {
        return transactions.stream()
                .anyMatch(t -> t.getAmount().compareTo(SUSPICIOUS) > 0);
    }

    public static void main(String[] args) {
        List<Transaction> transactions = SampleBank.transactions();

        boolean imperative = imperative(transactions);
        boolean streams = withStreams(transactions);

        System.out.println("QUESTION: is there ANY transaction over 2000.00 (suspicious)?");
        System.out.println();
        System.out.println("IMPERATIVE (loop + early return):  " + imperative);
        System.out.println("STREAMS    (anyMatch):             " + streams);
        System.out.println();
        System.out.println("MATCH? " + (imperative == streams));
        System.out.println("POINT: anyMatch short-circuits — it stops at the FIRST match,");
        System.out.println("       exactly like the early `return true`, but as a built-in guarantee.");
    }
}
