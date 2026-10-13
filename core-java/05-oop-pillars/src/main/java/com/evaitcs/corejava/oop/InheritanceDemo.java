package com.evaitcs.corejava.oop;

import java.math.BigDecimal;

/**
 * INHERITANCE — `extends` buys code reuse, but only if you know EXACTLY what is inherited:
 *
 *   inherited:     FIELDS — yes, even private ones: every SavingsAccount object physically
 *                  contains Account's id/owner/balance slots; subclass code just cannot
 *                  mention them by name and goes through getters instead.
 *                  METHODS — deposit/withdraw/getBalance are used below, zero lines rewritten.
 *   NOT inherited: CONSTRUCTORS. Every subclass writes its own and calls super(...) as its
 *                  FIRST line — the parent's half of the object is built before the child's.
 *
 * The second act is the trap every cohort hits: an "override" with a typo'd signature
 * silently becomes an OVERLOAD — compiles clean, never called. @Override is the alarm bell.
 */
public class InheritanceDemo {

    public static void main(String[] args) {

        banner("1. What a subclass gets for free — and what it must write itself");
        SavingsAccount s = new SavingsAccount(
                "ACC-1001", "Ada Lovelace", new BigDecimal("500.00"), new BigDecimal("0.04"));
        s.deposit(new BigDecimal("100.00"));    // inherited from Account — not rewritten here
        s.withdraw(new BigDecimal("50.00"));    // inherited invariant: floor 0
        System.out.println("  deposit/withdraw/getBalance came down from Account for free.");
        System.out.println("  describe()/interestFor() did NOT: they are abstract up there,");
        System.out.println("  so the subclass was FORCED to write the HOW.");
        System.out.println("  " + s.describe());
        System.out.println();
        System.out.println("  Can an abstract class have a constructor? YES — super(id, owner, balance)");
        System.out.println("  just ran inside Account to build the parent half of this very object.");
        System.out.println("  Abstract = cannot be INSTANTIATED, not cannot be CONSTRUCTED. (Exam favorite.)");

        banner("2. The typo trap: log(Object) where log(String) was meant");
        AuditLogger sneaky = new SneakyLogger();
        System.out.println("  Calling log(\"deposit 250.00\") on a SneakyLogger held as AuditLogger:");
        sneaky.log("deposit 250.00");
        System.out.println("  ^^ The BASE method ran — look at the class name it printed: the object");
        System.out.println("     IS a SneakyLogger, yet its log(Object) never fired. That method is a");
        System.out.println("     perfectly legal OVERLOAD that nobody ever calls. It compiles. It just");
        System.out.println("     never runs. This is WHY @Override exists: wear the annotation and this");
        System.out.println("     file would NOT compile — the compiler screams instead of shrugging.");

        banner("3. Done right: @Override, and super.method() extends instead of replaces");
        AuditLogger proper = new LoudLogger();
        proper.log("withdraw 100.00");
        System.out.println("  ^^ LoudLogger called super.log(...) FIRST — the base behavior still");
        System.out.println("     happens — then added its own line. Extend, don't replace.");

        banner("4. Overriding changes the RULES, not just the text");
        Account current = new CurrentAccount(
                "ACC-2002", "Grace Hopper", new BigDecimal("300.00"), new BigDecimal("200.00"));
        current.withdraw(new BigDecimal("400.00"));   // fine: floor is -200.00 down here
        System.out.println("  CurrentAccount.withdraw(400.00) on a 300.00 balance -> " + current.getBalance()
                + " (overdraft at work).");
        System.out.println("  The SAME call on a SavingsAccount would have been refused. One signature,");
        System.out.println("  two rulebooks — the runtime object picks which one applies. (Polymorphism, next door.)");
    }

    /** The base behavior everyone inherits. */
    static class AuditLogger {
        void log(String event) {
            System.out.println("    [audit] " + event + "   <- logged by " + getClass().getSimpleName());
        }
    }

    /**
     * MEANT to shout about every event; WROTE log(Object) instead of log(String).
     * No @Override here on purpose — with the annotation this class would not compile,
     * which is precisely the annotation's job.
     */
    static class SneakyLogger extends AuditLogger {
        void log(Object event) {
            System.out.println("    [SNEAKY] this line NEVER prints through an AuditLogger reference");
        }
    }

    /** Same intention, correct signature, reuse-before-extend. */
    static class LoudLogger extends AuditLogger {
        @Override
        void log(String event) {
            super.log(event);   // first: the inherited behavior still happens...
            System.out.println("    [loud] ...and the override ADDED this line (super.method() = extend, not replace)");
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
