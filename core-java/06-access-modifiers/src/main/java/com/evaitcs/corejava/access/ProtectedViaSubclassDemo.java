package com.evaitcs.corejava.access;

import com.evaitcs.corejava.access.bank.BankAccount;
import com.evaitcs.corejava.access.other.SubclassInOtherPackage;

import java.math.BigDecimal;

/**
 * Vantage point 4 of 4 - and the exam favorite: protected from a SUBCLASS IN
 * ANOTHER PACKAGE. The subclass reaches this.balance (its inherited copy) but
 * NOT someOtherAccount.balance. Same field, same object type, opposite verdict -
 * the difference is WHO you access it THROUGH.
 */
public class ProtectedViaSubclassDemo {

    public static void main(String[] args) {

        banner("1. this.balance - the inherited copy - IS reachable");
        SubclassInOtherPackage sub = new SubclassInOtherPackage("SB-2002", new BigDecimal("100.00"));
        System.out.println("sub.readMyOwnBalanceDirectly() = " + sub.readMyOwnBalanceDirectly().toPlainString());
        sub.applyInterest(new BigDecimal("0.05")); // writes this.balance directly inside the subclass
        System.out.println("after applyInterest(0.05)      = " + sub.readMyOwnBalanceDirectly().toPlainString()
                + "   <- subclass WROTE the protected field");

        banner("2. other.balance - through a plain BankAccount reference - is NOT");
        BankAccount other = BankAccount.sampleAccount();
        // BigDecimal theirs = other.balance;
        //   javac: error: balance has protected access in BankAccount
        //   ...and it fails the SAME way inside SubclassInOtherPackage itself:
        //   the moment the receiver is typed BankAccount, the subclass is just
        //   "some class in another package". See explainWhatIsBlocked().
        System.out.println(sub.explainWhatIsBlocked(other));
        System.out.println("the public door still works: other.getBalance() = "
                + other.getBalance().toPlainString());

        banner("3. The nuance that proves the rule is about TYPES");
        SubclassInOtherPackage sibling = new SubclassInOtherPackage("SB-2003", new BigDecimal("55.00"));
        // Receiver typed SubclassInOtherPackage => JLS 6.6.2 satisfied => compiles:
        System.out.println("sub.peekAtSiblingBalance(sibling) = "
                + sub.peekAtSiblingBalance(sibling).toPlainString()
                + "   <- ALLOWED: accessed through our OWN subclass type");
        System.out.println("Same member, different receiver TYPE, different verdict.");

        banner("4. WHY - say this out loud in class");
        System.out.println("protected is about YOUR inherited copy. A subclass owns the protected");
        System.out.println("state it inherits and may manage it; it does NOT get a skeleton key to");
        System.out.println("every other object's protected state. Without this rule, any class in");
        System.out.println("any package could extend BankAccount and read every balance in the bank.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
