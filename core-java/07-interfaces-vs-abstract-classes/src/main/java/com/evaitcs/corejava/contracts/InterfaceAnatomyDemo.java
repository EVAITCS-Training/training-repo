package com.evaitcs.corejava.contracts;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;

/**
 * Claim #1 of the module: PRE-JAVA-8, an interface contains exactly TWO kinds
 * of member — public abstract methods and public static final constants. The
 * source code often HIDES those modifiers; reflection drags them back out.
 *
 * Point at Transferable.java first: it says `int MAX_TRANSFERS_PER_DAY = 3;`
 * and `void deposit(BigDecimal);` — not one modifier in sight. Then run this.
 */
public class InterfaceAnatomyDemo {

    public static void main(String[] args) {

        banner("1. The constant you wrote WITHOUT modifiers");

        // The compiler rewrote `int MAX_TRANSFERS_PER_DAY = 3;` into its full form.
        // Reflection reads the COMPILED truth, not the source shorthand.
        Field constant = firstField(Transferable.class);
        System.out.println("source code says : int " + constant.getName() + " = 3;   // no modifiers typed");
        System.out.println("class file says  : " + Modifier.toString(constant.getModifiers())
                + " int " + constant.getName());
        System.out.println("=> writing `int MAX = 5;` in an interface SILENTLY makes it public static final.");
        System.out.println("   (There is no such thing as a private or instance field in an interface —");
        System.out.println("    `private int x = 1;` is, word for word: error: modifier private not allowed here)");

        banner("2. Every method is public abstract, whether you typed it or not");

        for (Method m : Transferable.class.getDeclaredMethods()) {
            System.out.println("class file says  : " + Modifier.toString(m.getModifiers())
                    + "  " + m.getReturnType().getSimpleName() + " " + m.getName() + "(...)");
        }
        System.out.println("=> no bodies allowed. javac, word for word, if you try:");
        System.out.println("   error: interface abstract methods cannot have body");

        banner("3. Constants are SHARED, not per-object — so they are NOT state");

        SavingsAccount savings = new SavingsAccount("S-1001", new BigDecimal("1000.00"));
        CurrentAccount current = new CurrentAccount("C-2001", new BigDecimal("500.00"), new BigDecimal("200.00"));
        // Both read the SAME one constant — and note it works even through an
        // unrelated family: CardPayment never saw Transferable's source either.
        CardPayment card = new CardPayment("CARD-77", new BigDecimal("50.00"));
        System.out.println("savings reads MAX_TRANSFERS_PER_DAY = " + savings.MAX_TRANSFERS_PER_DAY);
        System.out.println("current reads MAX_TRANSFERS_PER_DAY = " + current.MAX_TRANSFERS_PER_DAY);
        System.out.println("card    reads MAX_TRANSFERS_PER_DAY = " + card.MAX_TRANSFERS_PER_DAY);
        System.out.println("=> ONE copy on the interface, inherited bare by every implementor.");
        System.out.println("   State would mean one copy PER OBJECT (next demo). Constants don't count.");

        banner("4. Implement it ALL — or be abstract yourself");

        // The rule, proven by our own types:
        //  - Account is ABSTRACT, so it may implement Auditable/PaymentMethod and leave
        //    Transferable alone entirely — the debt simply passes down the family.
        //  - CardPayment is CONCRETE, so it had to write every body of all FOUR interfaces.
        System.out.println("Account    is abstract? " + Modifier.isAbstract(Account.class.getModifiers())
                + "  -> may leave blanks; subclasses inherit the debt");
        System.out.println("CardPayment is abstract? " + Modifier.isAbstract(CardPayment.class.getModifiers())
                + "  -> concrete: EVERY method of EVERY interface has a body");
        System.out.println("A concrete class that skips one gets, word for word:");
        System.out.println("   error: CardPayment is not abstract and does not override abstract method auditTrail() in Auditable");
    }

    private static Field firstField(Class<?> type) {
        return type.getDeclaredFields()[0];   // Transferable declares exactly one field: the constant
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
