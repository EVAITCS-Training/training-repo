package com.evaitcs.corejava.contracts;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;

/**
 * Claim #2 of the module: an abstract class is a PARTIAL IMPLEMENTATION. It can
 * hold per-object STATE, have CONSTRUCTORS (reached via super), mix concrete
 * and abstract methods, and use ANY visibility — everything the interface above
 * is forbidden from doing.
 *
 * The one thing it shares with an interface: you can never instantiate it.
 *   Account a = new Account("X-1", BigDecimal.ZERO);
 *   error: Account is abstract; cannot be instantiated        <- javac, word for word
 */
public class AbstractAccountDemo {

    public static void main(String[] args) {

        banner("1. STATE: fields are one copy PER OBJECT (constants were one copy for ALL)");

        // Watch the constructor prints: Account's ctor line appears BEFORE the
        // SavingsAccount ctor line — super(...) runs first, always.
        System.out.println("new SavingsAccount(\"S-1001\", 1000.00):");
        SavingsAccount s1 = new SavingsAccount("S-1001", new BigDecimal("1000.00"));
        System.out.println("new SavingsAccount(\"S-1002\", 250.00):");
        SavingsAccount s2 = new SavingsAccount("S-1002", new BigDecimal("250.00"));

        s1.deposit(new BigDecimal("100.00"));    // touches ONLY s1's copy of `balance`
        System.out.println("after s1.deposit(100.00):  s1.balance = " + s1.getBalance()
                + "   s2.balance = " + s2.getBalance() + "  <- untouched");
        System.out.println("=> that independence IS state. An interface cannot express it:");
        System.out.println("   its only fields are static final constants — shared by everyone.");

        banner("2. What Account declares — reflection over the source");

        for (Field f : Account.class.getDeclaredFields()) {
            System.out.println("field   : " + Modifier.toString(f.getModifiers()) + " "
                    + f.getType().getSimpleName() + " " + f.getName());
        }
        System.out.println("=> private/protected fields: legal here, impossible in an interface.");
        System.out.println();
        for (Method m : Account.class.getDeclaredMethods()) {
            String kind = Modifier.isAbstract(m.getModifiers()) ? "BLANK (abstract)" : "body (concrete)";
            if (Modifier.isFinal(m.getModifiers())) {
                kind = "body (concrete, FINAL — the template method)";
            }
            System.out.println("method  : " + kind + "  "
                    + Modifier.toString(m.getModifiers()) + " " + m.getName() + "(...)");
        }
        System.out.println("=> concrete + abstract MIXED in one type — 'partial implementation'.");
        System.out.println("   Note monthlyFee is PROTECTED: an interface member can only ever be public.");

        banner("3. Constructors exist here — and only super(...) can reach them");

        System.out.println("SavingsAccount's constructor line 1 is: super(id, openingBalance);");
        System.out.println("Account's constructor is `protected` — it exists to be called BY SUBCLASSES,");
        System.out.println("never by the outside world (there IS no outside `new Account(...)`, see above).");
        CurrentAccount c = new CurrentAccount("C-2001", new BigDecimal("500.00"), new BigDecimal("200.00"));
        System.out.println("CurrentAccount added its OWN extra state on top: overdraft shows in toString:");
        System.out.println("  " + c);

        banner("4. The payoff: write the plumbing ONCE, reuse it in the whole family");

        // pay() lives in Account; both subclasses below just inherited it. Neither
        // SavingsAccount nor CurrentAccount wrote a single line of pay/audit code.
        s1.pay(new BigDecimal("25.00"));
        c.pay(new BigDecimal("10.00"));
        System.out.print("s1 trail after pay:\n" + s1.auditTrail());
        System.out.print("c  trail after pay:\n" + c.auditTrail());
        System.out.println("=> interfaces DECLARE such plumbing; abstract classes actually SHIP it.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
