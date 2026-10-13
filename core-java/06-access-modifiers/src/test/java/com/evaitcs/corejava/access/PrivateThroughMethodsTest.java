package com.evaitcs.corejava.access;

import com.evaitcs.corejava.access.bank.BankAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for section 4: private fields + public methods = CAPABILITY,
 * not just visibility. The pin is never readable from here (any package other
 * than... well, ANY package - this test is not even in the bank package), yet
 * everything that legitimately needs to happen around it still can.
 */
class PrivateThroughMethodsTest {

    @Test
    @DisplayName("pin is never readable - only checkable: the door hands out answers, not secrets")
    void pinIsCheckableButNeverReadable() {
        BankAccount acct = BankAccount.sampleAccount();

        // String p = acct.pin;
        //   javac: error: pin has private access in BankAccount
        // (identical error even from the SAME package - see SamePackageAccessTest)

        System.out.println("acct.pin         -> compile error (pin has private access in BankAccount)");
        System.out.println("acct.checkPin(\"2468\") = " + acct.checkPin("2468"));
        System.out.println("acct.checkPin(\"0000\") = " + acct.checkPin("0000"));
        assertTrue(acct.checkPin("2468"));
        assertFalse(acct.checkPin("0000"));
    }

    @Test
    @DisplayName("capability: rotating the pin REQUIRES the current pin - access with a proof attached")
    void rotatePinRequiresProof() {
        BankAccount acct = BankAccount.sampleAccount();

        SecurityException refused = assertThrows(SecurityException.class,
                () -> acct.rotatePin("0000", "1357")); // wrong current pin
        System.out.println("rotatePin(\"0000\", \"1357\") threw: " + refused.getMessage());
        assertTrue(acct.checkPin("2468"), "failed rotation must not damage the old pin");

        acct.rotatePin("2468", "1357"); // proof provided
        System.out.println("rotatePin(\"2468\", \"1357\") accepted; checkPin(\"1357\") = " + acct.checkPin("1357"));
        assertTrue(acct.checkPin("1357"));
        assertFalse(acct.checkPin("2468"));
    }

    @Test
    @DisplayName("the public doors VALIDATE - the invariant module 05 promised, now enforced")
    void publicMethodsGuardTheInvariant() {
        BankAccount acct = BankAccount.sampleAccount();

        assertThrows(IllegalArgumentException.class, () -> acct.deposit(new BigDecimal("-5.00")));
        assertThrows(IllegalArgumentException.class, () -> acct.deposit(null));
        assertThrows(IllegalStateException.class, () -> acct.withdraw(new BigDecimal("999999.00")));
        assertThrows(IllegalArgumentException.class, () -> new BankAccount("SB-9", BigDecimal.ONE, "abcd"));

        System.out.println("deposit(-5) / deposit(null) / overdraft / pin=\"abcd\" all rejected.");
        System.out.println("balance untouched: " + acct.getBalance().toPlainString());
        assertEquals(0, acct.getBalance().compareTo(new BigDecimal("250.00")));
        System.out.println("=> a public FIELD could be assigned ANYTHING; a public METHOD decides.");
    }

    @Test
    @DisplayName("private is per CLASS, not per OBJECT: samePinAs reads another object's private pin")
    void privateIsPerClassNotPerObject() {
        BankAccount a = BankAccount.sampleAccount();
        BankAccount b = new BankAccount("SB-1002", new BigDecimal("40.00"), "2468");
        BankAccount c = new BankAccount("SB-1003", new BigDecimal("10.00"), "1357");

        // Inside BankAccount.samePinAs(): `other.pin` - legal, because that code
        // lives in the same CLASS body. From out here it would be a compile error.
        System.out.println("a.samePinAs(b) = " + a.samePinAs(b) + "   (BankAccount read b.pin internally)");
        System.out.println("a.samePinAs(c) = " + a.samePinAs(c));
        assertTrue(a.samePinAs(b));
        assertFalse(a.samePinAs(c));
    }

    @Test
    @DisplayName("toString never leaks the private field - logging is a stranger")
    void toStringDoesNotLeakThePin() {
        BankAccount acct = BankAccount.sampleAccount();
        String text = acct.toString();

        System.out.println("acct.toString() = " + text);
        assertFalse(text.contains("2468"), "the pin must never appear in toString");
        assertTrue(text.contains("SB-1001"));
    }
}
