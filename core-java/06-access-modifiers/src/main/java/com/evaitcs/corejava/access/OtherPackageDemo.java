package com.evaitcs.corejava.access;

import com.evaitcs.corejava.access.bank.BankAccount;
import com.evaitcs.corejava.access.other.UnrelatedClass;

/**
 * Vantage point 3 of 4: a DIFFERENT package, NOT a subclass - the outside world.
 * Only `public` survives this trip. Everything else is a compile error, and
 * UnrelatedClass carries each forbidden line as a comment beside its real
 * javac message so the class can be projected and read line by line.
 */
public class OtherPackageDemo {

    public static void main(String[] args) {

        banner("1. The outside world knocks on the public door");
        BankAccount acct = BankAccount.sampleAccount();
        System.out.println("acct.id (public field, right here) = " + acct.id);
        acct.deposit(new java.math.BigDecimal("75.50")); // public method: fine from anywhere
        System.out.println("after deposit(75.50): getBalance() = " + acct.getBalance().toPlainString());

        banner("2. What UnrelatedClass (other package, no inheritance) reports");
        UnrelatedClass outsider = new UnrelatedClass();
        System.out.println(outsider.visibilityReport(acct));

        banner("3. The takeaway");
        System.out.println("public  = an API promise, callable from code that does not exist yet.");
        System.out.println("everything narrower = freedom to change the implementation later.");
        System.out.println("That is why 'start private, widen deliberately' is the design rule.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
