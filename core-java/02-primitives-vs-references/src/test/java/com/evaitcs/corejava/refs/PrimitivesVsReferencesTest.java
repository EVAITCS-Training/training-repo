package com.evaitcs.corejava.refs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 02-primitives-vs-references test
 * Each test proves ONE claim and prints the evidence it asserts, so the run doubles as a demo.
 */
class PrimitivesVsReferencesTest {

    @Test
    @DisplayName("int overflow wraps silently: MAX_VALUE + 1 == MIN_VALUE; addExact fails loud")
    void intOverflowWraps() {
        int wrapped = Integer.MAX_VALUE + 1;
        System.out.println("Integer.MAX_VALUE + 1 = " + wrapped + " (MIN_VALUE = " + Integer.MIN_VALUE + ")");
        assertEquals(Integer.MIN_VALUE, wrapped, "adding 1 past the top rolls over to the bottom");
        assertThrows(ArithmeticException.class, () -> Math.addExact(Integer.MAX_VALUE, 1),
                "Math.addExact turns the silent wrap into a catchable failure");
    }

    @Test
    @DisplayName("char is a number: 'A' + 1 == 'B'")
    void charIsANumber() {
        char next = (char) ('A' + 1);
        System.out.println("(char)('A' + 1) = '" + next + "' ; (int)'A' = " + (int) 'A');
        assertEquals('B', next);
        assertEquals(65, (int) 'A');
    }

    @Test
    @DisplayName("double loses money: 0.1 + 0.2 != 0.3, but BigDecimal is exact")
    void doubleLosesMoneyBigDecimalDoesNot() {
        double d = 0.1 + 0.2;
        System.out.println("double 0.1 + 0.2 = " + d + " ; (== 0.3) " + (d == 0.3));
        assertFalse(d == 0.3, "double 0.1 + 0.2 must NOT equal 0.3");

        BigDecimal exact = new BigDecimal("0.1").add(new BigDecimal("0.2"));
        System.out.println("BigDecimal 0.1 + 0.2 = " + exact.toPlainString());
        assertEquals(0, exact.compareTo(new BigDecimal("0.3")), "BigDecimal from strings is exact");
    }

    @Test
    @DisplayName("BigDecimal: equals checks SCALE, compareTo checks VALUE")
    void bigDecimalEqualsVsCompareTo() {
        BigDecimal twoOh = new BigDecimal("2.0");
        BigDecimal twoOhOh = new BigDecimal("2.00");
        System.out.println("2.0.equals(2.00) = " + twoOh.equals(twoOhOh)
                + " ; compareTo == 0 : " + (twoOh.compareTo(twoOhOh) == 0));
        assertFalse(twoOh.equals(twoOhOh), "different scale -> equals is false");
        assertEquals(0, twoOh.compareTo(twoOhOh), "same value -> compareTo is 0");
    }

    @Test
    @DisplayName("two references to ONE object: mutating through an alias is visible")
    void referencesShareOneObject() {
        Account a1 = new Account("A-1", "Ada", new BigDecimal("100.00"));
        Account a2 = a1; // copy the POINTER, not the object
        a2.deposit(new BigDecimal("50.00"));
        System.out.println("a1 = " + a1 + " ; a2 = " + a2 + " ; (a1 == a2) " + (a1 == a2));
        assertSame(a1, a2, "a2 = a1 copies the pointer, so they are the same object");
        assertEquals(0, a1.getBalance().compareTo(new BigDecimal("150.00")),
                "mutating through a2 changed a1 — there is only one object");
    }

    @Test
    @DisplayName("== asks 'same object?'; default equals agrees until module 08 overrides it")
    void equalsVsSameObject() {
        Account x = new Account("A-9", "Grace", new BigDecimal("250.00"));
        Account y = new Account("A-9", "Grace", new BigDecimal("250.00"));
        System.out.println("x = " + x + " ; y = " + y + " ; (x == y) " + (x == y) + " ; x.equals(y) " + x.equals(y));
        assertNotSame(x, y, "two separate new objects are never the same reference");
        assertFalse(x == y, "== is reference identity");
        assertFalse(x.equals(y), "Account does not override equals yet, so equals falls back to ==");
    }

    @Test
    @DisplayName("dereferencing null throws NullPointerException")
    void nullThrowsNpe() {
        Account nothing = null;
        NullPointerException e = assertThrows(NullPointerException.class, () -> nothing.getBalance());
        System.out.println("caught " + e.getClass().getSimpleName() + " dereferencing a null reference");
    }

    @Test
    @DisplayName("Java is pass-by-value: swap fails, reassign fails, mutate succeeds")
    void javaIsPassByValue() {
        // (a) primitives are copied — a swap cannot reach the caller's variables
        int a = 1, b = 2;
        PassByValueDemo.swap(a, b);
        System.out.println("after swap(a, b): a=" + a + " b=" + b);
        assertEquals(1, a, "caller's a unchanged");
        assertEquals(2, b, "caller's b unchanged");

        // (b) reassigning the parameter to a new object does NOT touch the caller's reference
        Account acc = new Account("A-1", "Ada", new BigDecimal("100.00"));
        PassByValueDemo.reassignToNew(acc);
        System.out.println("after reassignToNew(acc): " + acc);
        assertEquals("Ada", acc.getOwner(), "caller's object was NOT replaced");

        // (c) mutating through the copied pointer IS visible to the caller
        PassByValueDemo.mutate(acc);
        System.out.println("after mutate(acc): " + acc);
        assertEquals(0, acc.getBalance().compareTo(new BigDecimal("150.00")), "caller sees the mutation");
    }

    @Test
    @DisplayName("Integer cache boundary: 127 == 127 true, 128 == 128 false, equals always true")
    void integerCacheBoundary() {
        Integer lo1 = 127, lo2 = 127;
        Integer hi1 = 128, hi2 = 128;
        System.out.println("127 == 127 : " + (lo1 == lo2) + " ; 128 == 128 : " + (hi1 == hi2)
                + " ; 127.equals : " + lo1.equals(lo2) + " ; 128.equals : " + hi1.equals(hi2));
        assertTrue(lo1 == lo2, "127 is inside the cache -128..127, so valueOf returns the SAME object");
        assertFalse(hi1 == hi2, "128 is outside the cache, so valueOf returns DIFFERENT objects");
        assertEquals(lo1, lo2);
        assertEquals(hi1, hi2, ".equals compares value and is ALWAYS the right choice");
    }

    @Test
    @DisplayName("unboxing a null Integer throws NullPointerException")
    void unboxNullThrows() {
        Integer maybeNull = null;
        NullPointerException e = assertThrows(NullPointerException.class, () -> {
            int x = maybeNull; // auto-unbox -> maybeNull.intValue() -> NPE
            System.out.println("never runs: " + x);
        });
        System.out.println("caught " + e.getClass().getSimpleName() + " unboxing a null Integer");
    }

    @Test
    @DisplayName("collections need objects: List<int> is illegal, List<Integer> autoboxes")
    void collectionsNeedObjects() {
        List<Integer> numbers = new ArrayList<Integer>();
        numbers.add(7);            // autoboxing
        int back = numbers.get(0); // unboxing
        System.out.println("List<Integer> round-trip: added 7, read back " + back);
        assertEquals(7, back);
    }
}
