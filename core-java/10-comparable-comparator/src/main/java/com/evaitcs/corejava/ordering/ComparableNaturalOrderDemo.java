package com.evaitcs.corejava.ordering;

import java.util.Collections;
import java.util.List;

/**
 * Demo 3 — Comparable: the NATURAL order, defined INSIDE the class.
 *
 * Point at Transaction.java while this runs:
 *     public class Transaction implements Comparable<Transaction> {
 *         public int compareTo(Transaction other) { return id.compareTo(other.id); }
 *
 * The order travels WITH the class. Every Collections.sort(list), every no-arg
 * TreeSet/TreeMap, every code path in the JDK that needs "the obvious order" of a
 * Transaction now agrees, because there is exactly one place it can come from.
 */
public class ComparableNaturalOrderDemo {

    public static void main(String[] args) {
        banner("1. Collections.sort with NO comparator — the order came with the class");

        List<Transaction> statement = Transaction.sampleStatement();
        System.out.println("as inserted:");
        print(statement);

        Collections.sort(statement);           // calls a.compareTo(b) on the ELEMENTS
        System.out.println("after Collections.sort(statement) — no comparator passed anywhere:");
        print(statement);

        banner("2. compareTo delegates to String.compareTo — module 03");

        System.out.println("\"T-1005\".compareTo(\"T-1040\") = " + "T-1005".compareTo("T-1040")
                + "   <- lexicographic, char by char: '0' vs '4' at index 4");
        System.out.println("TRAP: \"T-10\".compareTo(\"T-9\") = " + "T-10".compareTo("T-9")
                + "  -> \"T-10\" sorts BEFORE \"T-9\"!");
        System.out.println("Lexicographic order is NOT numeric order — that is why real banks");
        System.out.println("zero-pad ids (T-0010) or use fixed-width ids.");

        banner("3. Exactly ONE natural order per class — the limitation");

        System.out.println("Collections.sort(statement) can now only ever mean ONE thing: by id.");
        System.out.println("The teller wants biggest-amount-first. The auditor wants oldest-first.");
        System.out.println("A class has exactly one compareTo slot — and Transaction spent it on id.");
        System.out.println("=> We need orders that live OUTSIDE the class and can be PASSED IN: Comparator.");
    }

    private static void print(List<Transaction> statement) {
        for (Transaction t : statement) {
            System.out.println("  " + t);
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
