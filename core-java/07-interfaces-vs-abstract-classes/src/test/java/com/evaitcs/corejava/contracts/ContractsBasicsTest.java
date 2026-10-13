package com.evaitcs.corejava.contracts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 07-interfaces-vs-abstract-classes test
 * Each test proves ONE claim and prints its evidence, so the test run doubles
 * as a demo. Everything asserted here is deterministic: reflection facts about
 * the compiled types, and exact BigDecimal arithmetic. Constructor teaching
 * prints will show up in the output — that is on purpose, they are evidence too.
 */
class ContractsBasicsTest {

    @Test
    @DisplayName("CLAIM 1: interface members are implicitly public static final / public abstract")
    void interfaceMembersGetTheirModifiersWhetherYouTypedThemOrNot() throws Exception {
        // Transferable.java literally says `int MAX_TRANSFERS_PER_DAY = 3;` — no modifiers.
        Field constant = Transferable.class.getField("MAX_TRANSFERS_PER_DAY");
        int mods = constant.getModifiers();

        System.out.println("source says `int MAX_TRANSFERS_PER_DAY = 3;`, class file says: "
                + Modifier.toString(mods) + " int, value = " + constant.getInt(null));
        System.out.println("Auditable.AUDIT_PREFIX = \"" + Auditable.AUDIT_PREFIX + "\" (inherited bare by implementors)");

        assertTrue(Modifier.isPublic(mods), "interface fields are always public");
        assertTrue(Modifier.isStatic(mods), "interface fields are always static — one shared copy");
        assertTrue(Modifier.isFinal(mods), "interface fields are always final — constants, not state");
        assertEquals(3, constant.getInt(null));

        for (Method m : PaymentMethod.class.getDeclaredMethods()) {
            System.out.println("PaymentMethod." + m.getName() + " is: " + Modifier.toString(m.getModifiers()));
            assertTrue(Modifier.isPublic(m.getModifiers()) && Modifier.isAbstract(m.getModifiers()),
                    "pre-Java-8 interface methods are always public abstract");
        }
    }

    @Test
    @DisplayName("CLAIM 2: abstract class holds PER-OBJECT state; interface constants are shared")
    void abstractClassHoldsStateAndEveryObjectGetsItsOwnCopy() {
        SavingsAccount s1 = new SavingsAccount("S-1001", new BigDecimal("1000.00"));
        SavingsAccount s2 = new SavingsAccount("S-1002", new BigDecimal("1000.00"));

        s1.deposit(new BigDecimal("100.00"));      // touches ONLY s1's fields
        s1.recordAudit("manual note");

        System.out.println("s1.balance = " + s1.getBalance() + " (auditCount " + s1.getAuditCount() + ")");
        System.out.println("s2.balance = " + s2.getBalance() + " (auditCount " + s2.getAuditCount() + ")");
        System.out.println("both read the SAME interface constant: MAX_TRANSFERS_PER_DAY = "
                + s1.MAX_TRANSFERS_PER_DAY + " / " + s2.MAX_TRANSFERS_PER_DAY);

        assertNotEquals(0, s1.getBalance().compareTo(s2.getBalance()), "state is per object");
        assertEquals(0, s2.getBalance().compareTo(new BigDecimal("1000.00")), "s2 must be untouched");
        assertEquals(s1.getAuditCount(), s2.getAuditCount() + 1, "audit log is per object too");
        assertEquals(s1.MAX_TRANSFERS_PER_DAY, s2.MAX_TRANSFERS_PER_DAY, "constants: one shared copy");
    }

    @Test
    @DisplayName("CLAIM 3: a class extends ONE class but may implement MANY interfaces")
    void oneClassSlotManyInterfaceSlots() {
        CardPayment card = new CardPayment("CARD-77", new BigDecimal("50.00"));

        System.out.println("CardPayment extends : " + card.getClass().getSuperclass().getSimpleName());
        System.out.print("CardPayment implements:");
        for (Class<?> i : card.getClass().getInterfaces()) {
            System.out.print(" " + i.getSimpleName());
        }
        System.out.println();
        System.out.println("SavingsAccount extends : " + SavingsAccount.class.getSuperclass().getSimpleName()
                + "  and implements: Transferable");

        assertEquals(Object.class, card.getClass().getSuperclass(), "one class slot, spent on Object");
        assertEquals(4, card.getClass().getInterfaces().length, "interface slots are unlimited in practice");
        assertTrue(card instanceof Transferable);
        assertTrue(card instanceof PaymentMethod);
        assertTrue(card instanceof Auditable);
        assertTrue(card instanceof Comparable);
        assertEquals(Account.class, SavingsAccount.class.getSuperclass(),
                "the extends slot was already spent — interfaces are the only way to add types");
    }

    @Test
    @DisplayName("CLAIM 4: template method is a FINAL skeleton over ABSTRACT blanks, with exact results")
    void templateMethodRunsTheFixedSkeletonAndSubclassesOnlyFillBlanks() throws Exception {
        Method skeleton = Account.class.getDeclaredMethod("processMonthEnd");
        Method blankOne = Account.class.getDeclaredMethod("interestRate");

        System.out.println("processMonthEnd : " + Modifier.toString(skeleton.getModifiers()));
        System.out.println("interestRate    : " + Modifier.toString(blankOne.getModifiers()));

        assertTrue(Modifier.isFinal(skeleton.getModifiers()), "subclasses may NOT break the skeleton");
        assertTrue(Modifier.isAbstract(blankOne.getModifiers()), "subclasses MUST fill the blank");

        // Exact arithmetic — deterministic to the cent:
        //   savings: 1000.00 * 0.03 = 30.00 interest, fee 0.00 -> 1030.00
        //   current:  500.00 * 0.00 =  0.00 interest, fee 2.00 ->  498.00
        SavingsAccount savings = new SavingsAccount("S-1001", new BigDecimal("1000.00"));
        CurrentAccount current = new CurrentAccount("C-2001", new BigDecimal("500.00"), new BigDecimal("200.00"));

        String savingsTrail = savings.processMonthEnd();
        String currentTrail = current.processMonthEnd();

        System.out.println("savings after month-end: " + savings.getBalance() + ", trail:");
        System.out.print(savingsTrail);
        System.out.println("current after month-end: " + current.getBalance() + ", trail:");
        System.out.print(currentTrail);

        assertEquals(0, savings.getBalance().compareTo(new BigDecimal("1030.00")));
        assertEquals(0, current.getBalance().compareTo(new BigDecimal("498.00")));
        assertTrue(savingsTrail.contains("month-end: interest +30.00, fee -0.00"),
                "the skeleton always ends with the audit step");
        assertTrue(currentTrail.contains("month-end: interest +0.00, fee -2.00"));
    }

    @Test
    @DisplayName("CLAIM 5: callback interface — two anonymous policies, one UNCHANGED callee")
    void anonymousCallbacksAreInterchangeableBehindTheInterface() {
        // The classic (pre-lambda) way to pass behavior — an anonymous inner class.
        // java8-features/01-lambdas: each becomes one arrow:  a -> new BigDecimal("1.50")
        FeeCalculator flat = new FeeCalculator() {
            @Override
            public BigDecimal feeFor(BigDecimal amount) {
                return new BigDecimal("1.50");
            }
        };
        FeeCalculator twoPercent = new FeeCalculator() {
            @Override
            public BigDecimal feeFor(BigDecimal amount) {
                return amount.multiply(new BigDecimal("0.02")).setScale(2, java.math.RoundingMode.HALF_UP);
            }
        };

        BigDecimal amount = new BigDecimal("250.00");
        BigDecimal flatTotal = AnonymousCallbackDemo.totalOf(amount, flat);
        BigDecimal percentTotal = AnonymousCallbackDemo.totalOf(amount, twoPercent);

        System.out.println("250.00 + flat 1.50 -> " + flatTotal);
        System.out.println("250.00 + 2% (5.00) -> " + percentTotal);
        System.out.println("same totalOf() method both times — the callee never changed");

        assertEquals(0, flatTotal.compareTo(new BigDecimal("251.50")));
        assertEquals(0, percentTotal.compareTo(new BigDecimal("255.00")));
        assertTrue(flat.getClass().isAnonymousClass() && twoPercent.getClass().isAnonymousClass(),
                "both policies are objects of nameless classes");
    }
}
