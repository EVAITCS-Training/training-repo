package com.evaitcs.corejava.access;

import com.evaitcs.corejava.access.bank.BankAccount;

/**
 * Vantage point 1 of 4: INSIDE the class itself. Every member is visible here -
 * including the private pin, and including the private pin of ANOTHER
 * BankAccount object, because `private` means "this CLASS", not "this OBJECT".
 */
public class FromInsideTheClassDemo {

    public static void main(String[] args) {

        banner("1. The class describes its own vision");
        BankAccount acct = BankAccount.sampleAccount();
        System.out.println(acct.visibilityReportFromInside());
        System.out.println("=> inside the class body, ALL FOUR levels are legal.");

        banner("2. private = per CLASS, not per OBJECT (the trap)");
        BankAccount twin = new BankAccount("SB-1002", new java.math.BigDecimal("40.00"), "2468");
        // samePinAs() lives INSIDE BankAccount and reads twin.pin - another object's
        // PRIVATE field. Legal: both objects' privates are visible to their own class.
        System.out.println("acct.samePinAs(twin)              = " + acct.samePinAs(twin)
                + "   <- BankAccount read twin.pin internally");
        System.out.println("a stranger's pin, same amount:    = "
                + new BankAccount("SB-1003", new java.math.BigDecimal("10.00"), "1357").samePinAs(twin));

        banner("3. The private nested class at work");
        // Vault is a PRIVATE static nested class: no code out here can name the type.
        // BankAccount.Vault v = ...;
        //   javac: error: Vault has private access in BankAccount
        // We can still USE it indirectly - checkPin() delegates to Vault.matches():
        System.out.println("acct.checkPin(\"2468\")             = " + acct.checkPin("2468")
                + "   <- BankAccount asked its private Vault internally");
        System.out.println("acct.checkPin(\"0000\")             = " + acct.checkPin("0000"));
        System.out.println("=> the pin itself never crossed the class boundary; only an ANSWER did.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
