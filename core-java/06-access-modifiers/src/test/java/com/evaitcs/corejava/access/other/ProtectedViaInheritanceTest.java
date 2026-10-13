package com.evaitcs.corejava.access.other;

import com.evaitcs.corejava.access.bank.BankAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests as PROOF, from the SUBCLASS-IN-ANOTHER-PACKAGE vantage point - the
 * protected subtlety, both halves: what compiles (this.balance) and what
 * doesn't (otherAccount.balance), with the real javac messages printed.
 */
class ProtectedViaInheritanceTest {

    @Test
    @DisplayName("subclass in another package reads its OWN inherited balance: this.balance")
    void subclassReadsItsOwnInheritedBalance() {
        SubclassInOtherPackage sub = new SubclassInOtherPackage("SB-2002", new BigDecimal("100.00"));

        BigDecimal direct = sub.readMyOwnBalanceDirectly(); // internally: return this.balance;

        System.out.println("readMyOwnBalanceDirectly() = " + direct.toPlainString()
                + "   <- protected field, other package, via inheritance");
        assertEquals(0, direct.compareTo(new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("subclass WRITES its inherited field and getBalance() sees the change - one storage")
    void depositFlowsThroughTheInheritedField() {
        SubclassInOtherPackage sub = new SubclassInOtherPackage("SB-2002", new BigDecimal("100.00"));

        sub.deposit(new BigDecimal("50.00"));       // public door mutates the protected field
        BigDecimal viaField = sub.readMyOwnBalanceDirectly(); // reads the SAME field directly
        sub.applyInterest(new BigDecimal("0.10"));  // subclass writes this.balance itself

        System.out.println("after deposit(50) + applyInterest(10%):");
        System.out.println("  via inherited field = " + viaField.toPlainString() + " (before interest)");
        System.out.println("  via public getter   = " + sub.getBalance().toPlainString() + " (after interest)");
        assertEquals(0, viaField.compareTo(new BigDecimal("150.00")));
        assertEquals(0, sub.getBalance().compareTo(new BigDecimal("165.00")));
    }

    @Test
    @DisplayName("the trap: subclass CANNOT read another account's protected field through a BankAccount reference")
    void subclassCannotReadAnotherAccountsProtectedField() {
        SubclassInOtherPackage sub = new SubclassInOtherPackage("SB-2002", new BigDecimal("100.00"));
        BankAccount other = BankAccount.sampleAccount();

        // BigDecimal theirs = other.balance;
        //   javac: error: balance has protected access in BankAccount
        // WHY: `other` is typed BankAccount. Protected-from-another-package is
        // access to YOUR inherited copy (JLS 6.6.2), not a master key to every
        // BankAccount in existence - otherwise "extends" would leak the whole bank.

        System.out.println("sub IS a BankAccount, `other` IS a BankAccount, yet:");
        System.out.println("  this.balance   -> COMPILES (my inherited copy)");
        System.out.println("  other.balance  -> error: balance has protected access in BankAccount");
        System.out.println("the public door is the intended route: other.getBalance() = "
                + other.getBalance().toPlainString());
        assertEquals(0, other.getBalance().compareTo(new BigDecimal("250.00")));
        System.out.println(sub.explainWhatIsBlocked(other));
    }

    @Test
    @DisplayName("the rule is about TYPES: a receiver typed as the subclass itself IS allowed")
    void receiverTypedAsOwnSubclassIsAllowed() {
        SubclassInOtherPackage a = new SubclassInOtherPackage("SB-2002", new BigDecimal("100.00"));
        SubclassInOtherPackage b = new SubclassInOtherPackage("SB-2003", new BigDecimal("55.00"));

        BigDecimal peeked = a.peekAtSiblingBalance(b); // internally: sibling.balance - COMPILES

        System.out.println("a.peekAtSiblingBalance(b) = " + peeked.toPlainString()
                + "   <- allowed: receiver typed SubclassInOtherPackage (our own type)");
        System.out.println("Same field as the failing `other.balance` - the receiver's TYPE is the whole difference.");
        assertEquals(0, peeked.compareTo(new BigDecimal("55.00")));
    }
}
