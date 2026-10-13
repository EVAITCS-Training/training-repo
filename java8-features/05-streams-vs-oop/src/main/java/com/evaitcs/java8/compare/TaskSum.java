package com.evaitcs.java8.compare;

import java.math.BigDecimal;
import java.util.List;

/**
 * TASK 3 — "total of all transaction amounts" (BigDecimal!).
 *
 * This is the first task where the result is ONE value, not a collection.
 * Collapsing many elements into one has a name: a REDUCTION (a "fold").
 *
 * TEACHING POINT: mutable accumulator vs fold.
 *   Imperative: a local variable that the loop keeps OVERWRITING — `total = total.add(...)`.
 *   Streams: reduce(identity, accumulator) — you hand the library the STARTING value
 *   and the COMBINING rule, and it does the overwriting for you.
 *
 * This is the task where students get lost, so go SLOW on reduce(). The comment
 * block below is written to be read aloud.
 */
public class TaskSum {

    /*
     * LOOP ANATOMY:
     *
     *   (1) BigDecimal total = BigDecimal.ZERO;   <- the ACCUMULATOR: mutable state.
     *                                                 The variable's whole life is being
     *                                                 reassigned on every iteration.
     *   (2) for (Transaction t : transactions) {  <- iteration, by hand
     *   (3)     total = total.add(t.getAmount()); <- THE COMBINING RULE: "add next amount
     *                                                 to what we have so far".
     *   (4) }
     *   (5) return total;
     *
     * Two traps to name out loud:
     *   a) `total.add(...)` WITHOUT reassignment does nothing — BigDecimal is immutable,
     *      add() returns a NEW object. The loop hides this because "total =" looks routine.
     *   b) Starting at `new BigDecimal("0")` vs `BigDecimal.ZERO` vs `null` — the identity
     *      value matters. Pick null and line (3) throws NPE on the first element.
     */
    public static BigDecimal imperative(List<Transaction> transactions) {
        BigDecimal total = BigDecimal.ZERO;            // (1) accumulator, starts at identity
        for (Transaction t : transactions) {           // (2)
            total = total.add(t.getAmount());          // (3) combining rule, by mutation
        }
        return total;                                  // (5)
    }

    /*
     * READ THIS ALOUD — reduce(identity, accumulator) demystified:
     *
     *   reduce(BigDecimal.ZERO, BigDecimal::add)
     *          \______________/  \_____________/
     *           the IDENTITY      the ACCUMULATOR FUNCTION
     *           = line (1) above  = line (3) above
     *           where to start    how to combine "so far" with "next"
     *
     * Mechanically, the library runs YOUR accumulator like a loop:
     *
     *   soFar = ZERO                       (the identity)
     *   soFar = add(soFar, T01.amount)
     *   soFar = add(soFar, T02.amount)
     *   ...                                (it drives; you just supply the rule)
     *   return soFar
     *
     * So lines (1)(2)(3)(5) of the loop collapse into ONE call. The mutation didn't
     * disappear — it moved INSIDE the library, where it's written once and tested
     * everywhere, instead of being re-typed (and re-bugged) in every method we write.
     *
     * Note we first map() to the amounts. Two steps, each saying one thing:
     *   map(Transaction::getAmount)  = "I only care about the amount" (TaskTransform's lesson)
     *   reduce(ZERO, BigDecimal::add) = "add them all up"
     *
     * WHY the 2-arg reduce and not the 1-arg one? reduce(ZERO, add) RETURNS BigDecimal —
     * it can't be empty, because an empty stream just yields the identity. The 1-arg
     * reduce(add) returns Optional<BigDecimal> because there might be no elements.
     * Starting from an identity is both simpler AND safer. (Optional again — module 03.)
     */
    public static BigDecimal withStreams(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static void main(String[] args) {
        List<Transaction> transactions = SampleBank.transactions();

        BigDecimal imperative = imperative(transactions);
        BigDecimal streams = withStreams(transactions);

        System.out.println("QUESTION: what is the total of ALL transaction amounts?");
        System.out.println();
        System.out.println("IMPERATIVE (accumulator + loop):            " + imperative.toPlainString());
        System.out.println("STREAMS    (map then reduce(ZERO, ::add)):  " + streams.toPlainString());
        System.out.println();
        // compareTo, not equals: BigDecimal.equals also compares SCALE, so 3 vs 3.00
        // are "not equal" under equals() but identical in value. Classic money trap.
        System.out.println("MATCH? " + (imperative.compareTo(streams) == 0));
        System.out.println("POINT: reduce(identity, accumulator) = start value + combining rule.");
        System.out.println("       The mutation moves into the library; you only state the rule.");
    }
}
