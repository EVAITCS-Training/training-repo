package com.evaitcs.corejava.access.other;

import com.evaitcs.corejava.access.bank.BankAccount;
// import com.evaitcs.corejava.access.bank.PackagePrivateHelper;
//   javac: error: PackagePrivateHelper is not public in com.evaitcs.corejava.access.bank; cannot be accessed from outside package
//   -> a package-private CLASS cannot even be IMPORTED from another package.

import java.math.BigDecimal;

/**
 * The OUTSIDE-WORLD vantage point: a different package AND not a subclass -
 * the harshest seat in the house. From here only `public` members exist;
 * everything narrower is a compile error, and the errors differ per level
 * (read them beside each commented line - javac literally names the rule
 * that blocked you).
 */
public class UnrelatedClass {

    /** Everything this class can and cannot touch on a BankAccount. */
    public String visibilityReport(BankAccount acct) {
        String id = acct.id;                    // public field: LEGAL
        BigDecimal bal = acct.getBalance();     // public method (the "door"): LEGAL
        boolean pinOk = acct.checkPin("0000");  // public capability: LEGAL (answer only, not the pin)

        // BigDecimal b = acct.balance;
        //   javac: error: balance has protected access in BankAccount
        //   -> protected from outside means "subclasses only, through inheritance";
        //      UnrelatedClass is not a subclass, so even the package half doesn't apply.

        // String note = acct.auditNote;
        //   javac: error: auditNote is not public in BankAccount; cannot be accessed from outside package

        // String p = acct.pin;
        //   javac: error: pin has private access in BankAccount

        // PackagePrivateHelper.describeRules();
        //   javac: error: PackagePrivateHelper is not public in com.evaitcs.corejava.access.bank; cannot be accessed from outside package

        return "from an OTHER PACKAGE, not a subclass (" + getClass().getPackage().getName() + ") I CAN see:\n"
                + "  public           id          = " + id + '\n'
                + "  public method    getBalance()= " + bal.toPlainString() + '\n'
                + "  public method    checkPin(..)= " + pinOk + "  (for the WRONG pin - an answer, never the secret)\n"
                + "and I CANNOT see:\n"
                + "  protected        balance     -> error: balance has protected access in BankAccount\n"
                + "  package-private  auditNote   -> error: auditNote is not public in BankAccount; cannot be accessed from outside package\n"
                + "  private          pin         -> error: pin has private access in BankAccount";
    }
}
