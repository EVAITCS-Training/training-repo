package com.evaitcs.corejava.oop;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The contrast examiners live on: OVERLOADING vs OVERRIDING.
 *
 *   OVERLOADING — same CLASS, same name, DIFFERENT signatures. The compiler picks
 *                 one at COMPILE time, by the static type of the arguments. Nothing
 *                 is dispatched; it is name-sharing, decided before the program runs.
 *   OVERRIDING  — INHERITANCE, same signature, new body. The JVM picks at RUNTIME,
 *                 by the actual class of the object. That pick is dynamic dispatch.
 *
 * Deposit(BigDecimal)/deposit(String) on Account provides the overloads; the
 * Account -> Savings/Current family provides the overrides.
 */
public class OverloadVsOverrideDemo {

    public static void main(String[] args) {

        banner("1. OVERLOADING — picked at compile time, by the argument's static type");
        SavingsAccount acc = new SavingsAccount(
                "ACC-1001", "Ada Lovelace", new BigDecimal("100.00"), new BigDecimal("0.04"));
        acc.deposit(new BigDecimal("25.00"));   // compiler chose deposit(BigDecimal) — before anything ran
        acc.deposit("25.00");                   // compiler chose deposit(String)     — before anything ran
        System.out.println("  after deposit(new BigDecimal(\"25.00\")) and deposit(\"25.00\"): " + acc.getBalance());

        Object mystery = "25.00";               // runtime: a String. compile-time: an Object.
        // acc.deposit(mystery);                // COMPILE ERROR: there is no deposit(Object).
                                                // The String hiding inside `mystery` is IRRELEVANT —
                                                // overload choice reads the DECLARED type only.
        acc.deposit((String) mystery);          // the CAST re-types the expression -> deposit(String) wins
        System.out.println("  deposit((String) mystery) worked; deposit(mystery) would not even compile.");
        System.out.println("  Proof enough: overload selection never looks at the runtime object.");

        banner("2. OVERRIDING — picked at runtime, by the object's actual class");
        List<Account> accounts = new ArrayList<Account>();
        accounts.add(acc);
        accounts.add(new CurrentAccount(
                "ACC-2002", "Grace Hopper", new BigDecimal("300.00"), new BigDecimal("200.00")));
        BigDecimal sum = new BigDecimal("1000.00");
        for (Account a : accounts) {
            // ONE call site, compiled ONCE against Account.interestFor(...).
            // Every iteration the JVM re-picks the body from the RUNTIME class.
            System.out.println("  interestFor(1000.00) on " + a.getClass().getSimpleName()
                    + " = " + a.interestFor(sum));
        }
        System.out.println("  Same signature, same reference type, different bodies — that is overriding.");

        banner("3. Side by side (the exam answer, printed)");
        System.out.println("                  OVERLOADING                OVERRIDING");
        System.out.println("  lives in        one class (or family)     subclass vs superclass");
        System.out.println("  signature       must DIFFER               must MATCH");
        System.out.println("  decided         compile time (static)     runtime (dynamic dispatch)");
        System.out.println("  decided by      argument's declared type  object's actual class");
        System.out.println("  purpose         convenience names         change behavior per type");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
