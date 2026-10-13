package com.evaitcs.corejava.refs;

import java.math.BigDecimal;

/**
 * References: a variable holds a POINTER to an object on the heap, not the object itself.
 *
 * THE ONE SENTENCE: `Account a2 = a1;` copies the ARROW, not the Account. Two arrows to one
 * object means mutating through either is seen through both — that is aliasing, and it is the
 * root of most "wait, how did THAT change?" bugs.
 */
public class ReferenceSemanticsDemo {

    public static void main(String[] args) {
        banner("1. A reference variable holds a POINTER, not the object");
        Account a1 = new Account("A-1", "Ada", new BigDecimal("100.00"));
        Account a2 = a1; // copies the POINTER — now two arrows aim at ONE heap object
        a2.deposit(new BigDecimal("50.00")); // mutate through a2...
        System.out.println("a1 = " + a1);
        System.out.println("a2 = " + a2);
        System.out.println("(a1 == a2) = " + (a1 == a2) + "   <- == on references asks: SAME OBJECT?");
        System.out.println("Mutating through a2 'changed' a1 because they ARE the same object — there is only one.");

        banner("2. == asks 'same object?'; equals asks 'same content?'");
        Account x = new Account("A-9", "Grace", new BigDecimal("250.00"));
        Account y = new Account("A-9", "Grace", new BigDecimal("250.00")); // same content, DIFFERENT object
        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("(x == y)     = " + (x == y) + "   (two separate heap objects, so false)");
        System.out.println("x.equals(y)  = " + x.equals(y) + "   (Account doesn't override equals yet, so it falls back to ==)");
        System.out.println("-> Module 08 (equals/hashCode) is where we teach Account.equals to compare CONTENT.");

        banner("3. null: the reference that points NOWHERE");
        Account nothing = null;
        System.out.println("nothing = " + nothing);
        try {
            System.out.println(nothing.getBalance()); // dereference null -> no object to call a method on
        } catch (NullPointerException e) {
            System.out.println("caught " + e.getClass().getSimpleName()
                    + " — there is no object behind the pointer, so .getBalance() has nothing to run on.");
        }
        System.out.println("Fix in real code: guard with `if (nothing != null)` (or use Optional — see java8-features).");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
