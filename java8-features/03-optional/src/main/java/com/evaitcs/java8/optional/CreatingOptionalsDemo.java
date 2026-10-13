package com.evaitcs.java8.optional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * The three factories — and WHEN to use which (this is a design decision, not trivia):
 *
 *   Optional.of(x)         x must NOT be null. If it is, you get an NPE right HERE, at the
 *                          creation site — fail fast where the bug is, not three frames later.
 *                          Use it when a null would mean "someone broke the contract".
 *   Optional.ofNullable(x) x MAY be null (map miss, nullable column, unset profile field).
 *                          A null quietly becomes Optional.empty(). This is the workhorse.
 *   Optional.empty()       the ready-made "no value" — what you RETURN from a lookup miss
 *                          or a factory with nothing to give. It is a cached singleton; cheap.
 *
 * THE TRAP: Optional.of(aNullableThing) is the most common beginner mistake — you have
 * just re-created the NPE you were trying to eliminate, only earlier in the stack trace.
 */
public class CreatingOptionalsDemo {

    public static void main(String[] args) {
        Account alice = Account.seedAccounts().get(0); // always present in seed data
        Customer bob = Customer.sampleCustomers().get(1); // email was never set -> null

        // ---------- 1. of(): fail fast when null is a BUG ----------
        Optional<Account> wrapped = Optional.of(alice);
        System.out.println("Optional.of(account)   -> " + wrapped);
        try {
            Optional<String> boom = Optional.of(bob.getEmail()); // Bob's email IS null...
            System.out.println(boom);                            // ...never reached
        } catch (NullPointerException e) {
            System.out.println("Optional.of(null)      -> threw NullPointerException AT THE CREATION SITE");
            System.out.println("  That is a FEATURE: the null was a contract violation, and of() refuses to");
            System.out.println("  smuggle it deeper into the program where the stack trace stops helping.");
        }

        // ---------- 2. ofNullable(): when absence is LEGAL ----------
        Optional<String> bobEmail = Optional.ofNullable(bob.getEmail());
        Optional<String> aliceEmail = Optional.ofNullable(alice.getOwner());
        System.out.println();
        System.out.println("ofNullable(\"Alice Nguyen\") -> " + aliceEmail);
        System.out.println("ofNullable(null)           -> " + bobEmail + "   isPresent()=" + bobEmail.isPresent());
        System.out.println("  Note: the result is EMPTY, not null. The box is never null — only its contents.");

        // ---------- 3. empty(): a named "no result" ----------
        Optional<Account> nothing = Optional.empty();
        System.out.println("Optional.empty()           -> " + nothing + "   (a shared singleton — cheap to return)");

        // ---------- Optional is a VALUE: equality looks inside the box ----------
        System.out.println();
        System.out.println("empty().equals(empty())                -> " + Optional.empty().equals(Optional.empty()));
        System.out.println("of(5000.00).equals(of(5000.00))        -> "
                + Optional.of(new BigDecimal("5000.00")).equals(Optional.of(new BigDecimal("5000.00"))));
        System.out.println("of(5000.00).equals(empty())            -> "
                + Optional.of(new BigDecimal("5000.00")).equals(Optional.empty()));

        // ---------- The anti-pattern that undoes everything ----------
        // Optional<Account> opt = null;   // compiles! and now you need a null check on
        //                                 // the thing whose ONLY JOB was to end null checks.
        // Point at this comment in class: an Optional variable should never itself be null.
        System.out.println();
        System.out.println("bobEmail != null -> " + (bobEmail != null) + "   (an Optional is never null; that would defeat the purpose)");
    }
}
