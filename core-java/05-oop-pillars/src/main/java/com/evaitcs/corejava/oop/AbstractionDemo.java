package com.evaitcs.corejava.oop;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ABSTRACTION — the base class says WHAT every account must do; the subclasses say HOW.
 * Callers program against the shape (Account, FeePolicy) and never against the gears.
 *
 * And the distinction exams love:
 *   ABSTRACTION   hides DESIGN complexity — you see `Account`, not Savings-vs-Current machinery.
 *   ENCAPSULATION hides DATA/state — balance is private and guarded by access rules.
 * One hides the shape of the machine; the other locks the cabinet.
 */
public class AbstractionDemo {

    public static void main(String[] args) {

        banner("1. WHAT is declared up top; HOW is written down below");
        Account savings = new SavingsAccount(
                "ACC-1001", "Ada Lovelace", new BigDecimal("500.00"), new BigDecimal("0.04"));
        Account current = new CurrentAccount(
                "ACC-2002", "Grace Hopper", new BigDecimal("300.00"), new BigDecimal("200.00"));
        // Both calls go through the SAME abstract signatures. Account promised they exist;
        // the objects answer them differently. The caller's code never changes.
        System.out.println("  savings.describe()      -> " + savings.describe());
        System.out.println("  current.describe()      -> " + current.describe());
        System.out.println("  savings.interestFor(1000.00) = " + savings.interestFor(new BigDecimal("1000.00")));
        System.out.println("  current.interestFor(1000.00) = " + current.interestFor(new BigDecimal("1000.00"))
                + "   <- returning ZERO is a perfectly good HOW");

        banner("2. You cannot instantiate an abstract class");
        // Account a = new Account("ACC-0000", "Nobody", BigDecimal.ZERO);
        // COMPILE ERROR: "Account is abstract; cannot be instantiated."
        // The blueprint has deliberate HOLES (describe, interestFor have no bodies);
        // only a subclass that fills every hole becomes an object.
        System.out.println("  Account.class carries the abstract flag? "
                + Modifier.isAbstract(Account.class.getModifiers()));
        System.out.println("  new Account(...) is a compile error (see the comment above) — but an");
        System.out.println("  abstract class CAN have constructors: super(...) from SavingsAccount");
        System.out.println("  ran Account's a moment ago. Not-instantiable != not-constructible.");

        banner("3. Interface + anonymous inner class — behavior as a plug, the classic way");
        // The fee desk depends on the FeePolicy SHAPE, not on any one fee schedule.
        // Today's schedule is plugged in as an ANONYMOUS INNER CLASS — a nameless
        // implementation built right at the point of use:
        FeePolicy standard = new FeePolicy() {
            @Override
            public BigDecimal feeFor(BigDecimal amount) {
                return amount.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP); // 1%
            }
        };
        FeePolicy premiumFlat = new FeePolicy() {
            @Override
            public BigDecimal feeFor(BigDecimal amount) {
                return new BigDecimal("2.50");   // flat fee, ignores the amount entirely
            }
        };
        BigDecimal transfer = new BigDecimal("300.00");
        System.out.println("  standard.feeFor(300.00)  = " + standard.feeFor(transfer));
        System.out.println("  premiumFlat.feeFor(300.00) = " + premiumFlat.feeFor(transfer));
        System.out.println("  Same calling code, two schedules — swap the plug, keep the socket.");
        // Forward pointer: java8-features/01-lambdas turns the 4-line ceremony above into
        // one arrow — FeePolicy standard = amount -> amount.multiply(ONE_PERCENT); —
        // same interface, same anonymous-object idea, less noise.

        banner("4. Abstraction vs encapsulation — one sentence each");
        System.out.println("  ABSTRACTION:   hide DESIGN complexity behind shapes (Account, FeePolicy).");
        System.out.println("  ENCAPSULATION: hide DATA/state behind access rules (private balance + guards).");
        System.out.println("  A car demonstrates both: you steer via wheel+pedals (abstraction — you");
        System.out.println("  never touch the engine), and the fuel tank is not open-topped (encapsulation).");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
