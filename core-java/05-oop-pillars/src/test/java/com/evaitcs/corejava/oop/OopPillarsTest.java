package com.evaitcs.corejava.oop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims the demos make. Run with:
 *   .\mvnw.cmd -pl 05-oop-pillars test
 * Each test proves ONE claim, prints the evidence it asserts, and stays deterministic —
 * the test run doubles as a show you can put on the projector.
 */
class OopPillarsTest {

    private static SavingsAccount savings() {
        return new SavingsAccount("ACC-1001", "Ada Lovelace", new BigDecimal("500.00"), new BigDecimal("0.04"));
    }

    private static CurrentAccount current() {
        return new CurrentAccount("ACC-2002", "Grace Hopper", new BigDecimal("300.00"), new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("ENCAPSULATION: withdraw refuses to push the balance below zero, and the balance survives untouched")
    void encapsulationRejectsNegativeBalance() {
        SavingsAccount acc = savings();

        IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class,
                () -> acc.withdraw(new BigDecimal("500.01")),
                "one cent more than the balance must be refused");
        assertEquals(new BigDecimal("500.00"), acc.getBalance(),
                "a refused withdraw must leave NO trace on the state");

        assertThrows(IllegalArgumentException.class, () -> acc.deposit(new BigDecimal("-50.00")),
                "a negative deposit is a withdrawal in disguise — also refused");
        assertThrows(IllegalArgumentException.class, () -> acc.withdraw(BigDecimal.ZERO),
                "zero is not a positive amount either");

        acc.withdraw(new BigDecimal("500.00"));   // exactly the balance IS allowed: floor is >= 0
        assertEquals(new BigDecimal("0.00"), acc.getBalance());

        System.out.println("refused withdraw said : " + rejected.getMessage());
        System.out.println("balance after refusal : 500.00 (untouched); after withdraw(500.00) exactly: " + acc.getBalance());
    }

    @Test
    @DisplayName("ENCAPSULATION's counter-example: a public field cannot say no")
    void publicFieldsCannotSayNo() {
        EncapsulationDemo.LeakyAccount leaky = new EncapsulationDemo.LeakyAccount("ACC-6666");
        leaky.balance = new BigDecimal("-999999");   // no method ran, no check fired, it just... landed

        assertEquals(new BigDecimal("-999999"), leaky.balance,
                "PROOF of the danger: with a public field, nonsense is one assignment away");
        System.out.println("leaky.balance = " + leaky.balance
                + "  <- assigned directly; nothing objected. This is what private + withdraw() prevents.");
    }

    @Test
    @DisplayName("OVERRIDING: dynamic dispatch picks the RUNTIME type's method, not the reference type's")
    void overrideDispatchPicksRuntimeType() {
        Account a = savings();     // compile-time type Account...
        Account b = current();     // ...runtime types Savings / Current

        assertEquals("SavingsAccount", a.getClass().getSimpleName());
        assertEquals("CurrentAccount", b.getClass().getSimpleName());
        assertTrue(a.describe().startsWith("Savings"), "SavingsAccount.describe() must run through an Account reference");
        assertTrue(b.describe().startsWith("Current"), "CurrentAccount.describe() must run through an Account reference");

        BigDecimal sum = new BigDecimal("1000.00");
        assertEquals(new BigDecimal("40.00"), a.interestFor(sum), "4% of 1000.00");
        assertEquals(BigDecimal.ZERO, b.interestFor(sum), "current accounts pay no interest");

        System.out.println("through Account references: a.describe() = " + a.describe());
        System.out.println("                            b.describe() = " + b.describe());
        System.out.println("interestFor(1000.00):       a = " + a.interestFor(sum) + ", b = " + b.interestFor(sum));
    }

    @Test
    @DisplayName("OVERRIDING via inheritance: the overdraft override changes the FLOOR, not the discipline")
    void overdraftChangesTheFloorNotTheDiscipline() {
        CurrentAccount acc = current();

        acc.withdraw(new BigDecimal("500.00"));               // 300 - 500 = -200: exactly the floor
        assertEquals(new BigDecimal("-200.00"), acc.getBalance());
        assertThrows(IllegalArgumentException.class,
                () -> acc.withdraw(new BigDecimal("0.01")),   // one cent past the limit: still refused
                "even an overdraft account has an invariant — it is just a different floor");

        System.out.println("CurrentAccount went to " + acc.getBalance() + " (limit -200.00), and the next cent was refused.");
        System.out.println("Same method name, different floor — the override changed the RULE, and the guard still guards.");
    }

    @Test
    @DisplayName("OVERLOADING: the compiler picks by the argument's COMPILE-TIME type, never the runtime object")
    void overloadPicksCompileTimeType() {
        RecordingTeller teller = new RecordingTeller();

        Object mystery = "250.00";        // runtime: a String. compile-time: an Object.
        // teller.deposit(mystery);       // COMPILE ERROR: no deposit(Object) exists — proof the
                                          // runtime String inside is never consulted for overload choice.
        teller.deposit((String) mystery); // the cast re-types the expression -> String overload selected
        assertEquals("deposit(String)", teller.lastOverload);

        teller.deposit(new BigDecimal("250.00"));
        assertEquals("deposit(BigDecimal)", teller.lastOverload);

        System.out.println("deposit((String) mystery)           ran -> " + "deposit(String)");
        System.out.println("deposit(new BigDecimal(\"250.00\"))  ran -> " + teller.lastOverload);
        System.out.println("Both choices were made by the COMPILER, before a single instruction ran.");
    }

    /** Small recorder proving WHEN overload selection happens: compile time. */
    static class RecordingTeller {
        String lastOverload = "(none yet)";

        void deposit(BigDecimal cash) {
            lastOverload = "deposit(BigDecimal)";
        }

        void deposit(String amountText) {
            lastOverload = "deposit(String)";
        }
    }

    @Test
    @DisplayName("INHERITANCE: construction order is super-ctor THEN child-ctor, every single object")
    void superConstructorOrder() {
        List<String> log = ConstructionOrderDemo.LOG;
        log.clear();

        new ConstructionOrderDemo.Branch("Testside");

        List<String> ctorSteps = new ArrayList<String>();
        for (String step : log) {
            if (step.startsWith("constructor")) {
                ctorSteps.add(step);
            }
        }
        assertEquals(2, ctorSteps.size(), "exactly one ctor per level of the hierarchy");
        assertTrue(ctorSteps.get(0).contains("HeadOffice"), "parent ctor runs FIRST — super() is the first line");
        assertTrue(ctorSteps.get(1).contains("Branch"), "child ctor body runs SECOND");

        System.out.println("full construction log for one new Branch(...):");
        for (int i = 0; i < log.size(); i++) {
            System.out.println("  step " + (i + 1) + ": " + log.get(i));
        }
        System.out.println("(static-init lines appear only if this was the JVM's first Branch —");
        System.out.println(" statics run once per CLASS, constructors once per OBJECT; both orders asserted here.)");
    }

    @Test
    @DisplayName("ABSTRACTION: Account cannot be instantiated, yet its constructor runs inside every subclass")
    void abstractClassIsConstructibleButNotInstantiable() {
        // `new Account(...)` is a COMPILE ERROR — asserted at runtime via the class's own flag:
        assertTrue(Modifier.isAbstract(Account.class.getModifiers()), "Account must carry the abstract flag");

        Account acc = savings();   // ...but Account's CONSTRUCTOR just ran, via super(id, owner, balance):
        assertEquals("ACC-1001", acc.getId());          // id lives in a PRIVATE Account field...
        assertEquals(new BigDecimal("500.00"), acc.getBalance());  // ...and so does balance — inherited, not copied
        assertTrue(acc instanceof SavingsAccount, "every SavingsAccount IS-A Account (and IS-A SavingsAccount)");

        FeePolicy flat = new FeePolicy() {
            @Override
            public BigDecimal feeFor(BigDecimal amount) {
                return new BigDecimal("2.50");
            }
        };
        assertEquals(new BigDecimal("2.50"), flat.feeFor(new BigDecimal("9999.99")),
                "the anonymous class fulfils the interface's single promise");

        System.out.println("Account abstract? " + Modifier.isAbstract(Account.class.getModifiers())
                + " ; instantiable? no — but its ctor built the private id/balance fields of " + acc);
        System.out.println("anonymous FeePolicy.feeFor(anything) = " + flat.feeFor(BigDecimal.ONE)
                + "  (java8-features/01-lambdas writes this as one arrow)");
    }
}
