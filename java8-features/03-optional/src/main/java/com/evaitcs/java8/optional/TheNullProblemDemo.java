package com.evaitcs.java8.optional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Act one: WHY Optional had to exist. Two distinct problems with null:
 *
 * 1. THE CRASH. Tony Hoare, who put null references into ALGOL W in 1965, calls it his
 *    "billion dollar mistake": "I couldn't resist the temptation to put in a null
 *    reference, simply because it was so easy to implement." Every NPE since is the bill
 *    coming due — and it always lands far from the code that produced the null.
 *
 * 2. THE AMBIGUITY (the subtler one). A single value `null` is forced to mean at least
 *    three different things: "not found", "not set yet", and "legally has no value".
 *    The caller CANNOT tell them apart, so the caller guesses — and guesses wrong.
 */
public class TheNullProblemDemo {

    public static void main(String[] args) {
        LegacyAccountRepository legacy = new LegacyAccountRepository();
        List<Customer> customers = Customer.sampleCustomers();

        // ---------- Problem 1: the crash ----------
        Account found = legacy.findById("ACC-1001");
        System.out.println("findById(\"ACC-1001\") -> " + found);

        Account missing = legacy.findById("ACC-9999"); // a typo'd id — an ordinary, legal mistake
        System.out.println("findById(\"ACC-9999\") -> " + missing);
        try {
            // The caller who didn't read the Javadoc. Watch WHERE the exception fires:
            // the bug was the unchecked lookup two lines up, but the NPE detonates here.
            BigDecimal balance = missing.getBalance();
            System.out.println(balance); // never reached
        } catch (NullPointerException e) {
            System.out.println("BOOM: NullPointerException — and notice it has NO message.");
            System.out.println("  The stack trace points at .getBalance(), not at the lookup that leaked the null.");
        }

        // ---------- Problem 2: ambiguity — three nulls, three meanings ----------
        Customer bob = customers.get(1);  // email == null
        Customer dana = customers.get(2); // primaryAccountId == null
        Account bobAccount = legacy.findById(bob.getPrimaryAccountId());

        System.out.println();
        System.out.println("Three nulls walk into a bar — and look IDENTICAL to the type system:");
        System.out.println("  legacy.findById(\"ACC-9999\")          -> null  meaning: NOT FOUND (typo? deleted? who knows)");
        System.out.println("  bob.getEmail()                         -> " + bob.getEmail() + "     meaning: NOT SET YET (we never asked him)");
        System.out.println("  dana.getPrimaryAccountId()             -> " + dana.getPrimaryAccountId() + "     meaning: LEGALLY HAS NO ACCOUNT (walk-in customer)");
        System.out.println("  bobAccount.getNullableCreditLimit()    -> " + bobAccount.getNullableCreditLimit() + " meaning: LEGALLY HAS NO LIMIT (it's a debit account)");
        System.out.println("  ^ Four different situations, one value. `if (x == null)` cannot ask which one you got.");

        // ---------- The cost: every caller hand-writes the same defensive wall ----------
        System.out.println();
        System.out.println("So pre-Java-8 callers ALL wrote this, at EVERY call site:");
        printBalanceDefensively(legacy, "ACC-1001");
        printBalanceDefensively(legacy, "ACC-9999");
        System.out.println("  ...and the one caller who forgot the check is the one who pages you at 3am.");
    }

    /**
     * The pre-Java-8 "safe" caller. Three lines of ceremony around one idea, repeated
     * everywhere — and safety only holds if EVERY caller remembers. Optional replaces
     * this with .map(...).orElse(...): the check becomes impossible to forget because
     * the only way to get the value OUT is to state what happens when it is absent.
     */
    private static void printBalanceDefensively(LegacyAccountRepository repo, String id) {
        Account a = repo.findById(id);
        BigDecimal balance = BigDecimal.ZERO;
        if (a != null) {
            balance = a.getBalance();
        }
        System.out.println("  defensive lookup of " + id + " -> balance " + balance.toPlainString());
    }
}
