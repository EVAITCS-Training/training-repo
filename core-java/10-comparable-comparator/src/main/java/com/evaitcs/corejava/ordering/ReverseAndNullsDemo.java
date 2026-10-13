package com.evaitcs.corejava.ordering;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Demo 7 — two "reverse" tools that are NOT the same thing, and the null grenade.
 *
 *   Collections.reverseOrder()  -> a COMPARATOR that inverts the natural order.
 *                                  Changes HOW elements are compared during a sort.
 *   Collections.reverse(list)   -> MUTATES an existing list end-for-end.
 *                                  No comparisons happen at all.
 *
 * Then: comparators crash on null fields (NPE from deep inside sort), and the
 * pre-Java-8 fix — an explicit null policy inside compare() (ByOwnerNullSafe).
 */
public class ReverseAndNullsDemo {

    public static void main(String[] args) {
        banner("1. Collections.reverseOrder() — sort descending, still a SORT");

        List<Transaction> descending = Transaction.sampleStatement();
        Collections.sort(descending, Collections.reverseOrder()); // works BECAUSE Transaction is Comparable
        printIds("newest id first", descending);
        System.out.println("reverseOrder() is just a Comparator that flips the sign of compareTo.");

        banner("2. Collections.reverse(list) — NOT a sort, a mutation");

        List<Transaction> mutated = Transaction.sampleStatement();
        Collections.sort(mutated);                 // natural order first
        printIds("sorted ascending ", mutated);
        Collections.reverse(mutated);              // flip whatever order happens to be there
        printIds("after reverse()  ", mutated);
        System.out.println("Same result HERE only because we reversed a sorted list.");
        System.out.println("reverse() on an UNSORTED list gives an unsorted list, backwards.");
        System.out.println("=> reverseOrder changes COMPARISON; reverse changes the LIST. Different tools.");

        banner("3. Nulls: a comparator crashes where the sort runs, not where the null was set");

        List<Account> accounts = new ArrayList<Account>(Arrays.asList(
                new Account("A-01", "Dana", new BigDecimal("100.00")),
                new Account("A-02", null,   new BigDecimal("7.50")),   // joint account, owner not registered yet
                new Account("A-03", "Ana",  new BigDecimal("42.00"))));
        try {
            Collections.sort(accounts, new ByOwner());
        } catch (NullPointerException e) {
            System.out.println("CAUGHT -> " + e.getClass().getName() + ": " + e.getMessage());
            System.out.println("The NPE fires INSIDE compare(), called from deep inside java.util —");
            System.out.println("the stack trace blames the sort machinery, not the null owner.");
        }

        banner("4. The pre-Java-8 fix: a null POLICY inside compare()");

        Collections.sort(accounts, new ByOwnerNullSafe());   // nulls first, decided by us
        for (Account a : accounts) {
            System.out.println("  " + a);
        }
        System.out.println("ByOwnerNullSafe decides: null owner sorts FIRST (incomplete records surface).");
        System.out.println("The policy must be SYMMETRIC (null-vs-x and x-vs-null agree) or the");
        System.out.println("sign contract from Demo 2 breaks.");
        System.out.println("Java 8 packages this exact policy: Comparator.nullsFirst(...) — forward-ref only.");
    }

    private static void printIds(String label, List<Transaction> statement) {
        StringBuilder ids = new StringBuilder(label + ": ");
        for (Transaction t : statement) {
            ids.append(t.getId()).append(' ');
        }
        System.out.println(ids.toString().trim());
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
