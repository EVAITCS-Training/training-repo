package com.evaitcs.corejava.refs;

import java.math.BigDecimal;

/**
 * Java is PASS-BY-VALUE — always. There is no pass-by-reference in Java, full stop.
 *
 * THE RULE (say it out loud): you always pass a COPY OF THE BITS in the variable.
 *   - For a primitive, the bits ARE the value, so the method gets its own number.
 *   - For a reference, the bits are the ADDRESS, so the method gets a copy of the address —
 *     a second arrow to the SAME object.
 * That one distinction explains all three cases below.
 */
public class PassByValueDemo {

    public static void main(String[] args) {
        banner("Case (a): swap two ints — the caller's values do NOT change");
        int a = 1, b = 2;
        System.out.println("caller before : a=" + a + " b=" + b);
        swap(a, b);
        System.out.println("caller after  : a=" + a + " b=" + b + "   <-- unchanged; swap only swapped its OWN copies");

        banner("Case (b): reassign the parameter to a NEW object — caller's reference unchanged");
        Account acc = new Account("A-1", "Ada", new BigDecimal("100.00"));
        System.out.println("caller before : " + acc);
        reassignToNew(acc);
        System.out.println("caller after  : " + acc + "   <-- still Ada; the method repointed only ITS copy of the pointer");

        banner("Case (c): MUTATE the passed object — caller DOES see it");
        System.out.println("caller before : " + acc);
        mutate(acc);
        System.out.println("caller after  : " + acc + "   <-- changed; the copied pointer addresses the SAME object");

        System.out.println();
        System.out.println("THE RULE: you always pass a COPY of the bits in the variable.");
        System.out.println("For an int the bits ARE the value; for a reference the bits are the ADDRESS.");
        System.out.println("So a method can never repoint YOUR variable, but it CAN mutate the object you both point at.");
    }

    // Each method receives COPIES. swap gets copies of two ints...
    static void swap(int x, int y) {
        int t = x; x = y; y = t; // ...and swaps only those local copies
    }

    // reassignToNew gets a copy of the ADDRESS; re-pointing the copy leaves the caller's arrow alone.
    static void reassignToNew(Account acc) {
        acc = new Account("A-2", "Bob", new BigDecimal("999.00"));
    }

    // mutate gets the same copy of the ADDRESS, but FOLLOWS it to the shared object and changes it.
    static void mutate(Account acc) {
        acc.deposit(new BigDecimal("50.00"));
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
