package com.evaitcs.corejava.contracts;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;

/**
 * Claim #3 of the module: the TEMPLATE METHOD is the abstract class killer app —
 * a fixed algorithm skeleton written ONCE in the parent, with blanks that
 * subclasses fill. An interface can declare the blanks, but pre-Java-8 it can
 * never ship the skeleton.
 *
 * Here: Account.processMonthEnd() — interest, then fee, then audit. Always in
 * that order, because compliance says so. `final` = subclasses may NOT break
 * the skeleton; they only choose the NUMBERS, never the STEPS.
 */
public class TemplateMethodDemo {

    public static void main(String[] args) {

        banner("1. The skeleton vs the blanks (read Account.processMonthEnd first)");

        System.out.println("final processMonthEnd() in Account:                 WHO WROTE IT");
        System.out.println("  step 1: interest = balance * interestRate()       <- BLANK  (abstract, public)");
        System.out.println("  step 2: deposit(interest) when > 0                <- SKELETON (concrete, once)");
        System.out.println("  step 3: fee = monthlyFee()                        <- BLANK  (abstract, PROTECTED)");
        System.out.println("  step 4: withdraw(fee) when > 0                    <- SKELETON (guards 0.00 blanks)");
        System.out.println("  step 5: recordAudit(\"month-end: ...\")             <- SKELETON, uses STATE");
        System.out.println("=> the parent owns the ORDER; the subclasses own the NUMBERS.");

        banner("2. Same skeleton call, two different families fill it differently");

        SavingsAccount savings = new SavingsAccount("S-1001", new BigDecimal("1000.00"));
        CurrentAccount current = new CurrentAccount("C-2001", new BigDecimal("500.00"), new BigDecimal("200.00"));

        System.out.println();
        System.out.println("savings.processMonthEnd()  -> rate 0.03, fee 0.00");
        System.out.print(savings.processMonthEnd());
        System.out.println("   balance now: " + savings.getBalance() + "   (1000 + 30.00 interest - 0 fee)");

        System.out.println();
        System.out.println("current.processMonthEnd()  -> rate 0.00, fee 2.00");
        System.out.print(current.processMonthEnd());
        System.out.println("   balance now: " + current.getBalance() + "   (500 + 0 interest - 2.00 fee)");

        System.out.println("=> identical steps, different blanks. The CALLER just says processMonthEnd()");
        System.out.println("   and cannot tell (or care) which subclass runs — that is polymorphism,");
        System.out.println("   steered by the parent's final skeleton.");

        banner("3. Why `final`? Proof by reflection, and the error it prevents");

        boolean isFinal = Modifier.isFinal(method("processMonthEnd", Account.class).getModifiers());
        boolean rateIsAbstract = Modifier.isAbstract(method("interestRate", Account.class).getModifiers());
        System.out.println("Account.processMonthEnd is final?    " + isFinal
                + "   <- subclasses may NOT break the skeleton");
        System.out.println("Account.interestRate is abstract?    " + rateIsAbstract
                + "   <- subclasses MUST fill the blank");

        // A rogue subclass that tries to reorder (or skip) the compliance steps:
        //   class GreedyAccount extends Account {
        //       @Override public String processMonthEnd() { return "no fee today"; }
        //   }
        // javac, word for word:
        //   error: processMonthEnd() in GreedyAccount cannot override processMonthEnd() in Account
        //   overridden method is final
        System.out.println();
        System.out.println("A GreedyAccount that overrides processMonthEnd() to skip the fee gets:");
        System.out.println("   error: processMonthEnd() in GreedyAccount cannot override processMonthEnd() in Account");
        System.out.println("   overridden method is final");
        System.out.println("=> `final` on a template method is not micro-management; it is the POLICY.");
    }

    private static java.lang.reflect.Method method(String name, Class<?> type) {
        for (java.lang.reflect.Method m : type.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        throw new IllegalStateException("no such method: " + name);
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
