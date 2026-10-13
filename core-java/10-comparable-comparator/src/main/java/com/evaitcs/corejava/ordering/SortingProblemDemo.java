package com.evaitcs.corejava.ordering;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Demo 1 — THE PROBLEM: Java cannot guess your business order.
 *
 * Collections.sort works on Strings and Integers out of the box because those classes
 * implement Comparable. On a class that does not, the same call blows up at RUNTIME
 * with a ClassCastException — caught and printed below, message and all.
 *
 * The nested Transaction here is "yesterday's Transaction": just id + amount, no
 * ordering. The module's real Transaction (top-level, same package) implements
 * Comparable — ComparableNaturalOrderDemo shows what it learned and why.
 */
public class SortingProblemDemo {

    /** Transaction BEFORE we taught it an order — deliberately does NOT implement Comparable. */
    static class Transaction {
        private final String id;
        private final BigDecimal amount;

        Transaction(String id, BigDecimal amount) {
            this.id = id;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return id + " (" + amount.toPlainString() + ")";
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void main(String[] args) {
        banner("1. Types WITH a natural order sort out of the box");

        List<String> owners = new ArrayList<String>(Arrays.asList("Dana", "Ana", "Bo"));
        Collections.sort(owners);              // String implements Comparable<String>
        System.out.println("owners sorted   : " + owners);

        List<Integer> amounts = new ArrayList<Integer>(Arrays.asList(1200, 86, 2450));
        Collections.sort(amounts);             // Integer implements Comparable<Integer>
        System.out.println("amounts sorted  : " + amounts);

        banner("2. The SAME call on a class WITHOUT Comparable — at RUNTIME");

        // A RAW list is the only way this even compiles. That is no accident:
        // Collections.sort(List<T>) DEMANDS T extends Comparable, so typed code with
        // yesterday's Transaction is a COMPILE error — generics are the seatbelt.
        // Raw types switch the seatbelt off, and the failure moves to runtime.
        List statement = new ArrayList();
        statement.add(new Transaction("T-1002", new BigDecimal("1200.00")));
        statement.add(new Transaction("T-1001", new BigDecimal("2450.00")));
        try {
            Collections.sort(statement);
            System.out.println("sorted: " + statement);   // never reached
        } catch (ClassCastException e) {
            System.out.println("CAUGHT -> " + e.getClass().getName() + ": " + e.getMessage());
            // The message names the exact crime: our Transaction "cannot be cast to"
            // java.lang.Comparable — the sort machinery tried to treat each element
            // as Comparable and the JVM said: it isn't one.
        }

        banner("3. Why Java cannot help — and its two-part answer");

        System.out.println("Sort the statement by amount? by id? oldest first? biggest first?");
        System.out.println("Every answer is a BUSINESS decision — the compiler has no way to guess.");
        System.out.println("So Java makes YOU write the order, and gives exactly two tools:");
        System.out.println("  Comparable  — the order defined INSIDE the class (its NATURAL order)");
        System.out.println("  Comparator  — orders defined OUTSIDE the class (CUSTOM, as many as you want)");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
