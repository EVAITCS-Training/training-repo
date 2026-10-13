package com.evaitcs.corejava.ordering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Demo 4 — Comparator: CUSTOM orders, defined OUTSIDE the class.
 *
 * The SAME list gets sorted three different ways, just by passing a different
 * Comparator object — Transaction itself is never touched. Three shapes, in the
 * order codebases historically acquired them:
 *   1. a named top-level class  (ByAmountDesc — the classic, reusable, testable)
 *   2. an anonymous inner class (the middle step — behavior without a filename)
 *   3. ...and in the next course, the lambda that replaces #2 with one line.
 */
public class ComparatorCustomOrderDemo {

    public static void main(String[] args) {
        banner("1. Named class OUTSIDE Transaction: ByAmountDesc (biggest first)");

        List<Transaction> byAmount = new ArrayList<Transaction>(Transaction.sampleStatement());
        Collections.sort(byAmount, new ByAmountDesc());   // the pre-Java-8 call
        print(byAmount);
        System.out.println("Point at ByAmountDesc.java: the order is a SEPARATE, named, reusable class.");

        banner("2. The middle step: an anonymous inner class, by description");

        // Behavior handed to sort() without giving it a filename. The compiler turns
        // this into a class anyway (ComparatorCustomOrderDemo$1.class) — that ceremony
        // is exactly what Java 8 lambdas delete.
        // java8-features/01-lambdas: Comparator.comparing(Transaction::getDescription) replaces this
        Comparator<Transaction> byDescription = new Comparator<Transaction>() {
            @Override
            public int compare(Transaction t1, Transaction t2) {
                return t1.getDescription().compareTo(t2.getDescription());
            }
        };
        List<Transaction> alphabetical = new ArrayList<Transaction>(Transaction.sampleStatement());
        alphabetical.sort(byDescription);                 // list.sort(c) — Java 8's own method
        print(alphabetical);
        System.out.println("list.sort(c) and Collections.sort(list, c) are the SAME sort, two vintages.");

        banner("3. A third order on the SAME data: ByTimestamp (oldest first)");

        List<Transaction> byTime = new ArrayList<Transaction>(Transaction.sampleStatement());
        Collections.sort(byTime, new ByTimestamp());
        print(byTime);
        System.out.println("One class, one natural order (id) — and unlimited custom ones.");

        banner("4. The same Comparator also BUILDS sorted collections: TreeMap");

        // The comparator is just an object — anywhere ordering is needed, pass it in.
        // new TreeMap<>(c) / new TreeSet<>(c) then use c.compare INSTEAD of compareTo.
        Map<Transaction, String> richestFirst = new TreeMap<Transaction, String>(new ByAmountDesc());
        for (Transaction t : Transaction.sampleStatement()) {
            richestFirst.put(t, t.getDescription());
        }
        System.out.println("TreeMap keyed by Transaction, ordered by ByAmountDesc:");
        for (Transaction key : richestFirst.keySet()) {
            System.out.println("  " + key.getId() + " -> " + richestFirst.get(key));
        }

        // Module 08, again: for a TreeMap, compare() == 0 MEANS "same key" — equals is
        // never consulted. T-9999 has the same AMOUNT as T-1027 Groceries (86.40), so
        // ByAmountDesc says 0 and the put() below replaces the Groceries VALUE — while
        // the map even keeps the ORIGINAL key object (T-1027), not the new one.
        System.out.println("size before = " + richestFirst.size());
        Transaction sameAmount = new Transaction("T-9999", "Mystery", new java.math.BigDecimal("86.40"), 1_700_000_500_000L);
        richestFirst.put(sameAmount, "I replaced Groceries");
        System.out.println("size after putting T-9999 (also 86.40) = " + richestFirst.size());
        for (Transaction key : richestFirst.keySet()) {
            if (key.getAmount().compareTo(new java.math.BigDecimal("86.40")) == 0) {
                System.out.println("  the 86.40 slot now reads: " + key.getId() + " -> " + richestFirst.get(key));
                System.out.println("  (value replaced, and the map KEPT the old key object: " + key.getId() + ", not T-9999)");
            }
        }
        System.out.println("=> compare() == 0 collapses keys. Choose comparator keys that can't tie, or know this.");
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
