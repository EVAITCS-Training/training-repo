package com.evaitcs.corejava.access.other;

import com.evaitcs.corejava.access.bank.BankAccount;

import java.math.BigDecimal;

/**
 * The SUBCLASS vantage point - and the subtlest seat in the house.
 *
 * This class lives in a DIFFERENT package but EXTENDS BankAccount, so it
 * inherits the protected balance. "Inherits" is the operative word: the
 * protected member is reachable on THIS object (this.balance) because the
 * subclass owns an inherited copy - but NOT on some other BankAccount
 * reference. JLS 6.6.2: a protected member may be accessed from outside its
 * package only by code responsible for THAT object's implementation, i.e.
 * through the subclass's own type.
 *
 * WHY the rule exists: protection is about YOUR inherited copy. Without it,
 * any class anywhere could extend BankAccount and then read the balances of
 * EVERY account in the system through a plain reference - "protected" would
 * collapse into "public with extra steps".
 */
public class SubclassInOtherPackage extends BankAccount {

    /** Demo pin only - real code would never hard-code one. */
    public SubclassInOtherPackage(String id, BigDecimal openingBalance) {
        super(id, openingBalance, "9999");
    }

    /**
     * ALLOWED: `this.balance` - the inherited copy. This single line is the
     * entire point of protected-from-another-package. It compiles HERE and
     * nowhere else outside the bank package.
     */
    public BigDecimal readMyOwnBalanceDirectly() {
        return this.balance;
    }

    /**
     * ALLOWED, and worth pausing on: the receiver's declared type is
     * SubclassInOtherPackage - our OWN subclass type - so JLS 6.6.2 is
     * satisfied even though `sibling` is a different object. The rule checks
     * the TYPE you access through, not object identity.
     */
    public BigDecimal peekAtSiblingBalance(SubclassInOtherPackage sibling) {
        return sibling.balance;
    }

    /** The forbidden accesses, with the real javac messages. */
    public String explainWhatIsBlocked(BankAccount other) {
        // BigDecimal theirs = other.balance;
        //   javac: error: balance has protected access in BankAccount
        //   -> THE EXAM FAVORITE: `other` is typed BankAccount, not
        //      SubclassInOtherPackage, so this subclass is just "an unrelated
        //      class in another package" as far as that reference goes.

        // String note = other.auditNote;
        //   javac: error: auditNote is not public in BankAccount; cannot be accessed from outside package
        //   -> subclasses do NOT unlock package-private. That level is about
        //      LOCATION (the package), not lineage.

        // String p = other.pin;
        //   javac: error: pin has private access in BankAccount
        //   -> private is not inherited-by-name at all: no subclass, anywhere,
        //      can write `pin`. (The FIELD still exists in this object's memory -
        //      ReflectionEscapeHatchDemo proves it.)

        return "from a SUBCLASS in another package:\n"
                + "  this.balance     = " + this.balance.toPlainString() + "  <- ALLOWED (my inherited copy)\n"
                + "  other.balance    -> error: balance has protected access in BankAccount\n"
                + "  other.auditNote  -> error: auditNote is not public in BankAccount; cannot be accessed from outside package\n"
                + "  other.pin        -> error: pin has private access in BankAccount";
    }

    /** A subclass feature that uses the inherited field the intended way. */
    public void applyInterest(BigDecimal rate) {
        BigDecimal interest = this.balance.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
        this.balance = this.balance.add(interest); // direct write to the inherited protected field
    }
}
