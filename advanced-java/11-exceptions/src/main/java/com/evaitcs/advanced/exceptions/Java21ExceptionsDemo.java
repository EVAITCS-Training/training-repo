package com.evaitcs.advanced.exceptions;

/**
 * Demo 7 — JAVA 21 CORNER: helpful NullPointerExceptions, and exhaustiveness you can trust.
 * (README "Java 21 corner".)
 *
 * Two modern upgrades to the exception story:
 *
 *  1. HELPFUL NullPointerExceptions (JEP 358, on by default since Java 15). Before this, an
 *     NPE's message was simply null — you stared at a line with five dereferences and guessed
 *     which one was the culprit. Now the JVM computes a precise message naming the null
 *     reference AND the operation that failed: 'Cannot invoke "Account.id()" because "a" is null'.
 *
 *  2. SEALED + pattern-matching switch make exhaustive error handling a COMPILE-TIME property
 *     (module 01). Because BankingError closes its family, the switch in BankingErrorsDemo
 *     needs no default branch, and adding a fourth permitted type breaks compilation everywhere
 *     until each new case is handled. getPermittedSubclasses() lets us SEE the closed family.
 */
public class Java21ExceptionsDemo {

    public static void main(String[] args) {
        banner("1. Helpful NullPointerException (JEP 358): the JVM names the null");

        Account missing = null;                       // a reference that should have been an account
        try {
            String id = missing.id();                 // dereference null -> NPE with a DETAILED message
            System.out.println("never reached: " + id);
        } catch (NullPointerException e) {
            System.out.println("step 1: CAUGHT -> " + e.getClass().getSimpleName());
            System.out.println("step 2: getMessage() = " + e.getMessage());
            System.out.println("        WHY it happened: the JVM saw `missing` was null at the .id() call and said so.");
            System.out.println("step 3: pre-JEP-358 this message was simply 'null' — you had to guess which reference.");
            System.out.println("verdict: the helpful message names the null AND the operation, so the bug is located for you.");
        }

        banner("2. Sealed family: the compiler KNOWS every case, so switches are exhaustive");

        Class<?>[] permitted = BankingError.class.getPermittedSubclasses();
        System.out.println("step 1: BankingError.getPermittedSubclasses() lists the CLOSED family:");
        for (Class<?> subtype : permitted) {
            System.out.println("        permits -> " + subtype.getSimpleName());
        }
        System.out.println("step 2: BankingErrorsDemo.customerMessage switches over all " + permitted.length
                + " cases with NO default branch — javac proves it exhaustive.");
        System.out.println("step 3: add a fourth permitted error tomorrow and that switch STOPS COMPILING until handled.");
        System.out.println("verdict: exhaustiveness is a compile-time guarantee, not a code-review hope (module 01).");

        banner("3. Why this pair matters together");
        System.out.println("step 1: helpful NPEs make the UNEXPECTED failure (a bug) self-describing at the crash site.");
        System.out.println("step 2: sealed + switch make the EXPECTED failures (domain errors) impossible to forget.");
        System.out.println("step 3: together they shrink the space of 'an error happened and nobody handled/named it well'.");
        System.out.println("verdict: Java 21 improves both ends — the accidental throw and the deliberate one.");

        banner("4. In one sentence");
        System.out.println("Java 21 makes the accidental NullPointerException tell you exactly what was null, and the");
        System.out.println("deliberate sealed error family impossible to switch over incompletely — both checked for you.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
