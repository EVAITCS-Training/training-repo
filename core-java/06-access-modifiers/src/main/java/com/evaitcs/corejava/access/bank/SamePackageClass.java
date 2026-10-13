package com.evaitcs.corejava.access.bank;

import java.math.BigDecimal;

/**
 * The SAME-PACKAGE vantage point: a different class, but inside
 * com.evaitcs.corejava.access.bank. From here the package-private member is
 * wide open, protected is open too (protected = package PLUS subclasses),
 * public is open - and private is STILL a wall. Same package buys you three
 * of the four levels, never the fourth.
 *
 * This class is `public` only so the demo runner in com.evaitcs.corejava.access
 * can ask it for a report; the interesting access happens inside the methods.
 */
public class SamePackageClass {

    /** What the bank package can see that the outside world cannot. */
    public String visibilityReport(BankAccount acct) {
        String note = acct.auditNote;      // package-private field: same package => LEGAL
        BigDecimal bal = acct.balance;     // protected field: the "package" half of protected => LEGAL
        String id = acct.id;               // public: legal everywhere
        acct.flagForAudit("same-package audit sweep"); // package-private METHOD: legal here

        // String p = acct.pin;
        //   javac: error: pin has private access in BankAccount
        //   -> private is CLASS-level, not package-level. Same package changes nothing.

        return "from the SAME PACKAGE (inside " + getClass().getPackage().getName() + ") I CAN see:\n"
                + "  public           id        = " + id + '\n'
                + "  protected        balance   = " + bal.toPlainString() + "  (protected INCLUDES the package)\n"
                + "  package-private  auditNote = " + acct.auditNote + "  (just wrote to it!)\n"
                + "and I CANNOT see:\n"
                + "  private          pin       -> error: pin has private access in BankAccount";
    }

    /** Direct field surgery the outside world could never do - the audit trail. */
    public void overwriteAuditNote(BankAccount acct, String note) {
        acct.auditNote = note; // legal: package-private field, same package
    }

    /** Reads the package-private field back out - again, legal only in here. */
    public String readBack(BankAccount acct) {
        return acct.auditNote;
    }

    /**
     * Same-package classes can even see each other's NON-PUBLIC classes:
     * PackagePrivateHelper has no `public` keyword, yet this call compiles -
     * while the identical line in UnrelatedClass (other package) does not.
     */
    public String askTheHiddenHelper() {
        return PackagePrivateHelper.describeRules();
    }
}
