package com.evaitcs.corejava.oop;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * POLYMORPHISM — the payoff the other pillars were building toward.
 * "One interface, many behaviors": one List<Account>, one loop, and every object
 * does its OWN thing when called.
 *
 * The rule to recite until it sticks:
 *   the COMPILE-TIME type of the reference decides what you MAY CALL;
 *   the RUNTIME type of the object decides what actually RUNS.
 * (That second half has a formal name — dynamic dispatch — and the README draws it.)
 */
public class PolymorphismDemo {

    public static void main(String[] args) {

        banner("1. One list, two kinds of account, zero if-statements");
        List<Account> accounts = new ArrayList<Account>();
        accounts.add(new SavingsAccount(
                "ACC-1001", "Ada Lovelace", new BigDecimal("500.00"), new BigDecimal("0.04")));
        accounts.add(new CurrentAccount(
                "ACC-2002", "Grace Hopper", new BigDecimal("300.00"), new BigDecimal("200.00")));

        for (Account a : accounts) {
            // The loop variable's compile-time type is Account. Which describe() runs?
            // The RUNTIME object decides, on every single call — that is dynamic dispatch.
            System.out.println("  " + a.getClass().getSimpleName() + " says: " + a.describe());
        }

        banner("2. The SAME call, different behavior: withdraw(400.00)");
        for (Account a : accounts) {
            a.withdraw(new BigDecimal("400.00"));   // one line of caller code, two rulebooks
            System.out.println("  " + a.getId() + " balance after withdraw(400.00): " + a.getBalance());
        }
        System.out.println("  Savings stopped at 100.00 (floor 0); Current went to -100.00 (floor -200).");
        System.out.println("  The caller never asked which kind it was holding. THAT is the point.");

        banner("3. Compile-time type decides what you MAY call");
        Account ref = accounts.get(0);              // the object inside IS a SavingsAccount...
        // ref.applyInterest();                     // ...but this line is a COMPILE ERROR:
                                                    // Account does not declare applyInterest(),
                                                    // and the compiler only ever reads the
                                                    // REFERENCE type, never the object.
        System.out.println("  ref points at a SavingsAccount, yet ref.applyInterest() would not compile:");
        System.out.println("  the compiler checks the reference type (Account), not the runtime object.");

        banner("4. instanceof + cast — the classic downcast idiom (ASK, then CAST)");
        for (Account a : accounts) {
            if (a instanceof SavingsAccount) {          // 1. ASK the runtime object
                SavingsAccount s = (SavingsAccount) a;  // 2. CAST — now the compiler is satisfied
                s.applyInterest();                      // 3. ...and the subclass-only method is legal
                System.out.println("  interest applied: " + s.describe());
            }
        }
        System.out.println("  Casting WITHOUT asking first is how ClassCastException happens —");
        System.out.println("  instanceof is the seatbelt. (Java 16 merges ask+cast into");
        System.out.println("  `if (a instanceof SavingsAccount s)` — README notes it; we drill the classic.)");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
