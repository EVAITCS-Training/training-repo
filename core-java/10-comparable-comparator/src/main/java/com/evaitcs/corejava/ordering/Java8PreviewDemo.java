package com.evaitcs.corejava.ordering;

import java.util.Comparator;
import java.util.List;

/**
 * Demo 8 (closing) — JAVA 8 PREVIEW: look, don't dwell.
 *
 * Everything in this file is the Java 8 comparator API this module deliberately
 * avoided so you learned the classic idiom first. It is PRINT ONLY — no test in
 * this module asserts anything from here. The named classes, the anonymous class,
 * the if-ladder and the null policy you just saw are what this one expression is
 * compiled down from, conceptually. When it stops looking like magic, you are
 * ready for ../java8-features — this is where that course starts.
 */
public class Java8PreviewDemo {

    public static void main(String[] args) {
        banner("Java 8 preview — comparing / thenComparing / reversed / method references");

        // One line replaces: a ByAmountDesc class + a ChainedOrderDemo ladder + a reversal trick.
        // Transaction::getAmount is a METHOD REFERENCE — "the function that gets the amount".
        Comparator<Transaction> byAmountThenIdReversed = Comparator
                .comparing(Transaction::getAmount)
                .thenComparing(Transaction::getId)
                .reversed();

        List<Transaction> statement = Transaction.sampleStatement();
        statement.sort(byAmountThenIdReversed);
        for (Transaction t : statement) {
            System.out.println("  " + t);
        }

        System.out.println();
        System.out.println("Reads as English: compare BY amount, THEN BY id, REVERSED.");
        System.out.println("Comparator.comparing/thenComparing/reversed are Java 8 APIs —");
        System.out.println("this is where the java8-features course starts (module 01-lambdas).");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
