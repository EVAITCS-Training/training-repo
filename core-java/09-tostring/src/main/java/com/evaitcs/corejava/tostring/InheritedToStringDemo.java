package com.evaitcs.corejava.tostring;

import java.math.BigDecimal;

/**
 * Demo 5 — toString and inheritance: whose résumé prints for a subclass?
 *
 * Method lookup walks UP the class hierarchy, but `this` inside the method that runs
 * is always the RUNTIME object. Two consequences, both printed below:
 *
 *   - A subclass with no toString INHERITS the superclass's. If that implementation
 *     HARDCODES the class name ("Account["), every subclass prints a LIE by omission:
 *     LegacySavings prints "Account[...]" and its interestRate is invisible.
 *   - If the superclass builds the name with getClass().getSimpleName(), subclasses
 *     print their OWN name automatically — the recommended trick. Top-level
 *     SavingsAccount extends AccountPrinted and writes no toString at all.
 *
 * Fix A (patch each subclass: override + super.toString() + append) vs Fix B (the
 * getClass() trick in the base, written ONCE): B is recommended — A relies on every
 * future subclass author remembering; B cannot be forgotten.
 */
public class InheritedToStringDemo {

    /** The "before" base: fine fields, but the class name is HARDCODED into the output. */
    static class LegacyAccount {
        private final String id;
        private final String owner;
        private final BigDecimal balance;

        LegacyAccount(String id, String owner, BigDecimal balance) {
            this.id = id;
            this.owner = owner;
            this.balance = balance;
        }

        @Override
        public String toString() {
            // Hardcoded "Account[" — written once, wrong forever for every subclass.
            return "Account[id=" + id + ", owner=" + owner
                    + ", balance=" + balance.toPlainString() + "]";
        }
    }

    /** Adds state, writes no toString — and inherits the LIE. */
    static class LegacySavings extends LegacyAccount {
        private final BigDecimal interestRate;

        LegacySavings(String id, String owner, BigDecimal balance, BigDecimal interestRate) {
            super(id, owner, balance);
            this.interestRate = interestRate;
        }

        BigDecimal getInterestRate() { return interestRate; }
    }

    /** Fix A: the subclass overrides, REUSES super.toString() and appends its own fields. */
    static class LegacySavingsPatched extends LegacyAccount {
        private final BigDecimal interestRate;

        LegacySavingsPatched(String id, String owner, BigDecimal balance, BigDecimal interestRate) {
            super(id, owner, balance);
            this.interestRate = interestRate;
        }

        @Override
        public String toString() {
            // super.toString() reuses the base résumé; the subclass adds what only it knows.
            return "LegacySavingsPatched[interestRate=" + interestRate.toPlainString()
                    + ", base=" + super.toString() + "]";
        }
    }

    public static void main(String[] args) {

        banner("1. The lie: LegacySavings prints as 'Account[...]'");

        LegacySavings legacy = new LegacySavings(
                "ACC-2002", "Grace Hopper", new BigDecimal("9300.10"), new BigDecimal("3.25"));
        System.out.println("legacy        : " + legacy);
        System.out.println("its real type : " + legacy.getClass().getSimpleName());
        System.out.println("interestRate  : " + legacy.getInterestRate() + "   <- IN the object, NOT in the output");
        System.out.println("=> The inherited method ran with `this` = a LegacySavings, but the");
        System.out.println("   hardcoded name says 'Account'. The midnight debugger is misled twice:");
        System.out.println("   wrong type, missing field.");

        banner("2. Fix A: override in the subclass, reuse super.toString()");

        LegacySavingsPatched patched = new LegacySavingsPatched(
                "ACC-2002", "Grace Hopper", new BigDecimal("9300.10"), new BigDecimal("3.25"));
        System.out.println("patched       : " + patched);
        System.out.println("=> Correct and complete — but EVERY subclass author must remember this.");
        System.out.println("   One forgetful teammate, and the lie is back.");

        banner("3. Fix B (recommended): getClass().getSimpleName() in the base, written once");

        SavingsAccount savings = new SavingsAccount(
                "ACC-2002", "Grace Hopper", new BigDecimal("9300.10"),
                "5500000000005678", "7788", new BigDecimal("3.25"));
        System.out.println("savings       : " + savings);
        System.out.println("declared in   : AccountPrinted — SavingsAccount writes NO toString at all");
        System.out.println("=> The NAME is right automatically: the method is inherited, but");
        System.out.println("   getClass() asks the RUNTIME object, and that is a SavingsAccount.");
        System.out.println("   Honest caveat: interestRate is still absent — a base class cannot");
        System.out.println("   print fields it has never heard of. Subclasses that ADD state should");
        System.out.println("   still override to append it; the trick guarantees the name never lies.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
