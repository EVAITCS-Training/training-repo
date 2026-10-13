package com.evaitcs.corejava.access.bank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF, from the SAME-PACKAGE vantage point. This test class lives
 * in com.evaitcs.corejava.access.bank, so package-private and protected
 * members of BankAccount are directly reachable BY NAME in this source file -
 * every legal access below is compiled evidence for the matrix in the README.
 * Run with: .\mvnw.cmd -pl 06-access-modifiers test
 */
class SamePackageAccessTest {

    @Test
    @DisplayName("same package can read AND write the package-private field directly")
    void packagePrivateFieldIsOpenToThePackage() {
        BankAccount acct = BankAccount.sampleAccount();

        assertEquals("account opened", acct.auditNote); // direct FIELD access - no getter needed
        acct.auditNote = "quarterly review";             // direct write - the package is a trust zone
        acct.flagForAudit("touched by SamePackageAccessTest"); // package-private METHOD

        System.out.println("read/wrote acct.auditNote directly from the same package:");
        System.out.println("  auditNote = " + acct.auditNote);
        assertTrue(acct.auditNote.contains("touched by SamePackageAccessTest"));
    }

    @Test
    @DisplayName("protected INCLUDES the package: same-package non-subclass reads balance")
    void protectedIsVisibleInSamePackageToo() {
        BankAccount acct = BankAccount.sampleAccount();

        BigDecimal direct = acct.balance; // legal HERE: protected = package + subclasses-anywhere
        // This exact line in UnrelatedClass (other package, non-subclass) is:
        //   error: balance has protected access in BankAccount

        System.out.println("same-package direct read: acct.balance = " + direct.toPlainString());
        assertEquals(0, direct.compareTo(new BigDecimal("250.00")));
        assertEquals(acct.getBalance(), direct, "the field and the public getter are the same room, different doors");
    }

    @Test
    @DisplayName("private stays private EVEN in the same package - only the class itself gets in")
    void privateStaysPrivateEvenInTheSamePackage() {
        BankAccount acct = BankAccount.sampleAccount();

        // String p = acct.pin;
        //   javac: error: pin has private access in BankAccount
        //   -> same package, different CLASS: still a wall. private is class-level.

        System.out.println("same package, but `acct.pin` is a compile error (see comment above).");
        System.out.println("the only door: acct.checkPin(\"2468\") = " + acct.checkPin("2468"));
        assertTrue(acct.checkPin("2468"));
        assertFalse(acct.checkPin("0000"));
    }

    @Test
    @DisplayName("a package-private CLASS is usable inside the package")
    void packagePrivateClassIsVisibleToThePackage() {
        // SamePackageClass.askTheHiddenHelper() calls PackagePrivateHelper.describeRules();
        // both are in the bank package, so the package-private class name compiles here.
        String rules = new SamePackageClass().askTheHiddenHelper();

        System.out.println("PackagePrivateHelper says: " + rules);
        assertTrue(rules.contains("NO modifier"));
    }
}
