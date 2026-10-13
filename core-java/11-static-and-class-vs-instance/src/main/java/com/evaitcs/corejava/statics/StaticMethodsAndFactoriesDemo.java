package com.evaitcs.corejava.statics;

import java.math.BigDecimal;

/**
 * Demo 2 - static METHODS: no object, no `this`, and why that is a feature.
 *
 * A static method belongs to the class, so it can run before any object exists -
 * but for the same reason it cannot touch instance fields: which object's field
 * would it even mean? The compiler refuses to guess (exact javac messages below).
 *
 * When to use static methods: factories, utilities, and main() itself.
 */
public class StaticMethodsAndFactoriesDemo {

    public static void main(String[] args) {

        banner("1. A static method runs with NO object - there is no `this`");
        // Nothing of ours has been constructed yet in this method; statics do not care.
        System.out.println("BankUtils.formatMoney(1234.5) = " + BankUtils.formatMoney(new BigDecimal("1234.5")));
        System.out.println("Math.max(3, 7)                = " + Math.max(3, 7));
        System.out.println("Math has no instances either - you never write `new Math()`.");

        banner("2. WHY it cannot touch instance fields - the exact compile errors");
        // If you pasted these into BankAccount itself, javac would refuse both:
        //     public static String nope() { return balance; }
        //         javac: error: non-static variable balance cannot be referenced from a static context
        //     public static String alsoNope() { return this.toString(); }
        //         javac: error: non-static variable this cannot be referenced from a static context
        // The legal static version takes the object as a PARAMETER (see BankUtils.isOverdrawn):
        // data comes IN through the argument list, never through a missing `this`.
        System.out.println("`balance` lives in ONE PARTICULAR object. A static method has no");
        System.out.println("instance in scope - WHICH balance would it mean? The compiler refuses to guess.");
        System.out.println("The mirror image IS legal: an instance method reads statics freely,");
        System.out.println("because every object knows its class:");
        BankAccount acc = new BankAccount("Ann", new BigDecimal("10.00"));
        System.out.println("  acc.describe() = " + acc.describe());

        banner("3. The STATIC FACTORY - why it beats a constructor");
        BankAccount savings = BankAccount.createSavings("Erin");
        System.out.println("BankAccount.createSavings(\"Erin\") -> " + savings.describe());
        System.out.println("A constructor can only ever be called BankAccount(...). A static factory:");
        System.out.println("  1. has a NAME - createSavings says what it builds;");
        System.out.println("  2. can return a CACHED/shared instance - Integer.valueOf does exactly");
        System.out.println("     this, which is why -128..127 are == (module 02 BoxingAndCacheDemo);");
        System.out.println("  3. can return any SUBTYPE of its return type (module 07: code to the interface);");
        System.out.println("  4. need not create a new object at all.");

        banner("4. Why must main() be static? (module 01 callback)");
        System.out.println("The JVM calls main() BEFORE any object of your classes exists -");
        System.out.println("there is no instance to call it on, so it must belong to the class.");
        System.out.println("Same rule, same reason, one method you have typed a hundred times.");
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
