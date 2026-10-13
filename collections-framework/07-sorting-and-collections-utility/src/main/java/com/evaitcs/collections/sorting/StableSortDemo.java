package com.evaitcs.collections.sorting;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Demo 1 — SORTING IS STABLE: equal elements keep their relative order.
 *
 * Collections.sort(list) is a thin delegate: since Java 8 it simply calls list.sort(null),
 * and the algorithm underneath is TIMSORT — a merge/insertion hybrid that runs O(n log n),
 * exploits existing runs of order (real data is often nearly sorted), and above all is
 * STABLE: when the comparator says "tie" (returns 0), the element that came FIRST in the
 * input still comes first in the output. Nothing in the signature promises this; the
 * implementation does, and the javadoc states it.
 *
 * Why a banker cares: sorting the statement by day must not scramble the order of five
 * transactions that happened the same millisecond; sorting customers by last name must
 * keep same-name customers in account-number order if that is how they arrived. Stability
 * is what makes multi-key sorting COMPOSABLE: sort by the minor key first, then by the
 * major key, and ties on the major key keep the minor order (the cheap man's thenComparing).
 *
 * This module does NOT re-teach Comparable vs Comparator or the subtraction trap — that
 * is core-java module 10 (the anonymous Comparator below uses Long.compare, the fix that
 * module's SubtractionTrapDemo motivates). The lambda form of the same comparator is
 * java8-features/01-lambdas.
 */
public class StableSortDemo {

    public static void main(String[] args) {
        banner("1. Natural order — Collections.sort(list) calls compareTo ON THE ELEMENTS");

        List<Transaction> statement = Transaction.sampleStatement();
        System.out.println("arrival order : " + idsOf(statement));
        Collections.sort(statement);                 // == statement.sort(null) since Java 8
        System.out.println("natural order : " + idsOf(statement) + "   (Transaction.compareTo = by id, core-java 10)");
        System.out.println("The statement ARRIVED id-sorted (chronological ids) — and TimSort noticed: it");
        System.out.println("detects existing runs of order and merges them, so nearly-sorted input is nearly free.");

        banner("2. Custom order — an anonymous Comparator, and a tie to watch");

        List<Transaction> byTime = Transaction.sampleStatement();
        // Classic idiom: anonymous inner class implementing Comparator (no lambdas in main
        // code — java8-features 01 collects this ceremony). Long.compare, NEVER a cast of
        // a subtraction: (int)(a - b) overflows on timestamps (core-java 10's trap).
        Comparator<Transaction> byTimestamp = new Comparator<Transaction>() {
            @Override
            public int compare(Transaction a, Transaction b) {
                return Long.compare(a.getTimestampMillis(), b.getTimestampMillis());
            }
        };
        System.out.println("before (arrival order, ts shown):");
        for (Transaction t : byTime) {
            System.out.println("  " + t.getId() + "  ts=" + t.getTimestampMillis() + "  (" + t.getDescription() + ")");
        }
        System.out.println("NOTE the tie: T-1001 and T-1004 BOTH have ts=2000. The tie is the experiment.");

        Collections.sort(byTime, byTimestamp);       // == byTime.sort(byTimestamp)
        System.out.println("after sorting by timestamp:");
        for (Transaction t : byTime) {
            System.out.println("  " + t.getId() + "  ts=" + t.getTimestampMillis() + "  (" + t.getDescription() + ")");
        }
        System.out.println("The tie kept arrival order: T-1001 (input index 0) is STILL before");
        System.out.println("T-1004 (input index 3). That is STABILITY — TimSort never swaps equal elements.");

        banner("3. reverseOrder() vs reverse(list) — one compares, one mutates (back-ref core-java 10)");

        List<Transaction> ids = Transaction.sampleStatement();
        Collections.sort(ids);                                   // ascending by id first
        Collections.sort(ids, Collections.reverseOrder());       // a COMPARATOR that flips comparisons
        System.out.println("sort(list, reverseOrder()) -> " + idsOf(ids) + "   (descending via comparison)");

        List<Transaction> flipped = Transaction.sampleStatement();
        Collections.sort(flipped);                               // ascending again...
        Collections.reverse(flipped);                            // ...then physically flipped end-for-end
        System.out.println("sort then reverse(list)    -> " + idsOf(flipped) + "   (no comparisons at all)");
        System.out.println("Same RESULT here only because the list was sorted first. reverseOrder() is a");
        System.out.println("Comparator you can hand to any sort or TreeMap; reverse(list) is a mutation.");
        System.out.println("(core-java 10, ReverseAndNullsDemo — this is the callback, not the lesson.)");
    }

    /** Small helper: the ids of a statement, in list order, for one-line printing. */
    private static String idsOf(List<Transaction> transactions) {
        StringBuilder ids = new StringBuilder();
        ids.append("[");
        for (Transaction t : transactions) {
            if (ids.length() > 1) {
                ids.append(", ");
            }
            ids.append(t.getId());
        }
        return ids.append("]").toString();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
