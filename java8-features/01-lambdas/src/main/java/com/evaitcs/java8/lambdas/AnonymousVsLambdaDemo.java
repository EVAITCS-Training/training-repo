package com.evaitcs.java8.lambdas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The "before / after" slide, in code. Same job — sort a statement by amount —
 * done the Java 7 way and the Java 8 way.
 *
 * TEACHING POINT: the lambda version is not just shorter. It says WHAT to compare
 * ("by amount") instead of spelling out HOW to be a Comparator object.
 */
public class AnonymousVsLambdaDemo {

    public static void main(String[] args) {
        List<Transaction> statement = Transaction.sampleStatement();

        // ---------- Java 7: the anonymous inner class ----------
        // Five lines of ceremony to express one idea. Everything except the
        // comparison expression is noise the compiler already knew.
        Comparator<Transaction> byAmountOldWay = new Comparator<Transaction>() {
            @Override
            public int compare(Transaction t1, Transaction t2) {
                return t1.getAmount().compareTo(t2.getAmount());
            }
        };

        // ---------- Java 8, step 1: the lambda ----------
        Comparator<Transaction> byAmountLambda =
                (t1, t2) -> t1.getAmount().compareTo(t2.getAmount());

        // ---------- Java 8, step 2: the method reference + library comparator ----------
        // comparing() takes a Function that EXTRACTS the sort key.
        // Transaction::getAmount reads exactly like English: "compare transactions BY amount".
        // This is the form we want students to write in production code.
        Comparator<Transaction> byAmountIdiomatic =
                Comparator.comparing(Transaction::getAmount);

        // All three produce the same order — run the tests to see this asserted.
        printSorted("Java 7 anonymous class", statement, byAmountOldWay);
        printSorted("Java 8 lambda", statement, byAmountLambda);
        printSorted("Java 8 comparing(::)", statement, byAmountIdiomatic);

        // Bonus: comparators compose — reversed(), thenComparing().
        // Point at this: behavior built FROM behavior, which is what lambdas enabled.
        List<Transaction> biggestFirst = new ArrayList<>(statement);
        biggestFirst.sort(Comparator.comparing(Transaction::getAmount).reversed());
        System.out.println();
        System.out.println("largest first (reversed()):");
        System.out.println("  " + biggestFirst.get(0));

        // And sorting by a String key uses the same trick:
        List<Transaction> byDescription = new ArrayList<>(statement);
        byDescription.sort(Comparator.comparing(Transaction::getDescription));
        System.out.println("alphabetically by description, first = " + byDescription.get(0).getDescription());

        // Collections.sort still works (it's the pre-Java-8 API), but prefer list.sort(...):
        Collections.sort(biggestFirst, byAmountIdiomatic);
    }

    private static void printSorted(String label, List<Transaction> statement, Comparator<Transaction> c) {
        List<Transaction> sorted = new ArrayList<>(statement);
        sorted.sort(c);
        System.out.println();
        System.out.println(label + ":");
        for (Transaction t : sorted) {
            System.out.println("  " + t);
        }
    }
}
