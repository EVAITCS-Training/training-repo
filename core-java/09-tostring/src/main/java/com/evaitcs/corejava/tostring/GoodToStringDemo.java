package com.evaitcs.corejava.tostring;

import java.math.BigDecimal;

/**
 * Demo 3 — writing a GOOD toString, and where it immediately pays off.
 *
 * A good résumé answers "who are you?" at a glance: class name + the business keys
 * a human debugging this object needs (id, owner, balance), in a stable, greppable
 * shape — with secrets masked (demo 6) and subclass-safe naming (demo 5).
 *
 * Two respectable styles, both shown below:
 *   - key=value  ("AccountPrinted[id=..., owner=...]") — the Eclipse/IDE default;
 *     greppable and diffable; the right pick for anything that lands in LOGS.
 *   - human sentence ("Ada Lovelace <ada@...>, member since 2019") — the right pick
 *     for objects embedded in USER-facing text.
 * Pick per audience, decide once per class, and be consistent.
 */
public class GoodToStringDemo {

    public static void main(String[] args) {

        banner("1. Three classes, three résumés");

        AccountRaw raw = new AccountRaw("ACC-1001", "Ada Lovelace", new BigDecimal("1250.75"), "4921");
        AccountPrinted account = new AccountPrinted(
                "ACC-1001", "Ada Lovelace", new BigDecimal("1250.75"), "4111111111111234", "4921");
        Customer customer = new Customer("Ada Lovelace", "ada@securebank.example", 2019);

        System.out.println("no override (default) : " + raw);
        System.out.println("key=value (IDE style) : " + account);
        System.out.println("human sentence        : " + customer);
        System.out.println("=> The default tells you WHICH OBJECT; a good toString tells you WHAT IT IS.");

        banner("2. Open AccountPrinted.toString() — four decisions, top to bottom");

        System.out.println("a) StringBuilder, not += (module 04): one buffer, append pieces, one String.");
        System.out.println("b) getClass().getSimpleName(), not hardcoded \"AccountPrinted[\": subclasses");
        System.out.println("   then print their OWN name automatically (InheritedToStringDemo proves it).");
        System.out.println("c) balance.toPlainString(): money never shows up as 1.25075E+3 (module 02).");
        System.out.println("d) secrets MASKED, never exported: card=****1234, pin=**** (demo 6).");

        banner("3. Where it pays off — you almost never call toString() yourself");

        // 3a. A log line: concatenation walks the valueOf road from demo 2 and calls it for you.
        System.out.println("INFO  Transfer of 250.00 approved for " + account);

        // 3b. An exception message: the object becomes part of the error text — the line
        //     a support engineer reads at 2 a.m. WITH the résumé, the message says
        //     "which account failed"; without it, "some object failed".
        try {
            withdraw(account, new BigDecimal("5000.00"));
        } catch (IllegalStateException e) {
            System.out.println("ERROR caught: " + e.getMessage());
        }

        // 3c. String.format("%s", ...) — routes through toString exactly like concat does.
        System.out.println(String.format("DEBUG customer=%s holds %s", customer, account));

        System.out.println("=> In ALL three lines the résumé was exported WITHOUT an explicit");
        System.out.println("   account.toString() call. Write it once; logs, errors and formats use it forever.");
    }

    /** A toy withdrawal so the error message above is real code, not a hypothetical. */
    private static void withdraw(AccountPrinted account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            // "Insufficient funds in AccountPrinted[id=ACC-1001, owner=Ada Lovelace, ...]" —
            // THAT is an error message you can act on.
            throw new IllegalStateException("Insufficient funds in " + account
                    + " for withdrawal of " + amount.toPlainString());
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
