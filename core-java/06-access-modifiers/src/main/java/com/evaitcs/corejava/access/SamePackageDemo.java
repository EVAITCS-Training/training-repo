package com.evaitcs.corejava.access;

import com.evaitcs.corejava.access.bank.BankAccount;
import com.evaitcs.corejava.access.bank.SamePackageClass;

/**
 * Vantage point 2 of 4: the SAME PACKAGE.
 *
 * THIS demo class does NOT live in the bank package - it cannot touch
 * auditNote itself (the line below fails here exactly as it fails in
 * OtherPackageDemo). So it borrows the eyes of SamePackageClass, which DOES
 * live in com.evaitcs.corejava.access.bank, and prints what that class sees.
 * That indirection is the lesson: package-private access is about WHERE THE
 * CODE LIVES, not who is asking.
 */
public class SamePackageDemo {

    public static void main(String[] args) {

        banner("1. What THIS class (com.evaitcs.corejava.access) may do");
        BankAccount acct = BankAccount.sampleAccount();
        // String note = acct.auditNote;
        //   javac: error: auditNote is not public in BankAccount; cannot be accessed from outside package
        System.out.println("acct.auditNote from here? NO - this demo is in the 'access' package,");
        System.out.println("not the 'bank' package. Same-package rights belong to the CODE's address.");

        banner("2. Borrowing same-package eyes: SamePackageClass (lives IN bank)");
        SamePackageClass insider = new SamePackageClass();
        System.out.println(insider.visibilityReport(acct));

        banner("3. Direct field surgery from inside the package");
        insider.overwriteAuditNote(acct, "note REWRITTEN by SamePackageClass");
        System.out.println("auditNote is now: " + insider.readBack(acct));
        System.out.println("=> package-private members are fully read/write for the package -");
        System.out.println("   that's why the package is your MODULE boundary: everything inside");
        System.out.println("   trusts everything else inside.");

        banner("4. The package can even see its own hidden classes");
        System.out.println(insider.askTheHiddenHelper());
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
