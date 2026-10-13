package com.evaitcs.advanced.generics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 01-generics-deep-dive test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC: erasure, signatures and bridge methods are
 * facts about the class file, and the heap-pollution ClassCastException happens on chosen
 * values, every single run.
 */
class GenericsProofTest {

    /** Field used as the reflection target for the "signatures survive" claim. */
    @SuppressWarnings("unused")
    private List<Transaction> statement = new ArrayList<>();

    @Test
    @DisplayName("erasure: List<String> and List<Integer> are the SAME Class object at runtime")
    void erasedTypesShareOneClass() {
        List<String> strings = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();

        assertSame(strings.getClass(), integers.getClass(), "one ArrayList.class serves every parameterization");
        assertSame(ArrayList.class, strings.getClass());
        System.out.println("List<String>.getClass() == List<Integer>.getClass() == " + strings.getClass().getName());
    }

    @Test
    @DisplayName("the type argument survives ONLY in generic signatures — reflection reads it back")
    void signaturesSurviveErasure() throws Exception {
        Field f = GenericsProofTest.class.getDeclaredField("statement");

        assertEquals(List.class, f.getType(), "getType() is the RAW class...");
        assertTrue(f.getGenericType() instanceof ParameterizedType,
                "...but getGenericType() reconstructs the declaration from the signature attribute");

        ParameterizedType pt = (ParameterizedType) f.getGenericType();
        assertEquals(Transaction.class, pt.getActualTypeArguments()[0]);
        System.out.println("field signature recovered: " + f.getGenericType());
        System.out.println("this is exactly how Spring reads List<Account> out of an @Autowired field (modules 02-03)");
    }

    @Test
    @DisplayName("the inserted cast is real: an Integer smuggled through a raw List explodes on read")
    void uncheckedSmuggleExplodesOnRead() {
        List<String> strings = new ArrayList<>();
        @SuppressWarnings("rawtypes")
        List raw = strings;

        strings.add("Dana");
        raw.add(Integer.valueOf(42));           // raw type: no protection, warning only

        ClassCastException e = assertThrows(ClassCastException.class, () -> {
            for (String s : strings) {          // javac inserted (String) on every read
                assertTrue(s.length() >= 0);
            }
        });
        System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        System.out.println("erasure moved the check from the write to the READ — the write was the crime");
    }

    @Test
    @DisplayName("bridge method: Transaction carries a synthetic compareTo(Object) that delegates")
    void bridgeMethodExists() {
        Method real = null;
        Method bridge = null;
        for (Method m : Transaction.class.getDeclaredMethods()) {
            if (!m.getName().equals("compareTo")) {
                continue;
            }
            if (m.isBridge()) {
                bridge = m;
            } else {
                real = m;
            }
        }

        assertTrue(real != null, "the method we wrote: compareTo(Transaction)");
        assertTrue(bridge != null, "the method javac generated: compareTo(Object), bridge=true");
        assertTrue(bridge.isSynthetic(), "bridges are marked synthetic — no source line exists for them");
        assertEquals(Object.class, bridge.getParameterTypes()[0]);
        assertEquals(Transaction.class, real.getParameterTypes()[0]);

        // And the bridge WORKS: sorting walks through it (core-java 10's machinery).
        List<Transaction> list = new ArrayList<>(List.of(
                new Transaction("T-2", "b", BigDecimal.ONE),
                new Transaction("T-1", "a", BigDecimal.ONE)));
        Collections.sort(list);
        assertEquals("T-1", list.get(0).getId());
        System.out.println("real   : " + real);
        System.out.println("bridge : " + bridge + "  (synthetic=" + bridge.isSynthetic() + ")");
        System.out.println("Collections.sort drove the bridge and sorted: " + list);
    }

    @Test
    @DisplayName("PECS: one sum() reads any List<? extends Number>; one fill() writes to any List<? super Integer>")
    void pecsBothDirections() {
        // PRODUCER side — ? extends Number accepts every Number subtype list:
        assertEquals(new BigDecimal("6"), WildcardsPecsDemo.sum(List.of(1, 2, 3)));
        assertEquals(new BigDecimal("10.75"), WildcardsPecsDemo.sum(List.of(new BigDecimal("10.50"), new BigDecimal("0.25"))));

        // CONSUMER side — ? super Integer accepts every Integer supertype list:
        List<Number> sink = new ArrayList<>();
        WildcardsPecsDemo.fillWithOnes(sink, 3);
        assertEquals(3, sink.size());

        List<Object> wider = new ArrayList<>();
        WildcardsPecsDemo.fillWithOnes(wider, 2);
        assertEquals(2, wider.size());

        System.out.println("sum read from List<Integer> AND List<BigDecimal>; fill wrote to List<Number> AND List<Object>");
    }

    @Test
    @DisplayName("heap pollution: storing a List<Integer> into a List<String>... varargs array throws on read")
    void heapPollutionThrows() {
        ClassCastException e = assertThrows(ClassCastException.class,
                () -> GenericTrapsDemo.dangerous(List.of("Dana"), List.of("Ana")));
        assertTrue(e.getMessage().contains("Integer"), "the message names the smuggled type");
        System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        System.out.println("write side silent, read side fatal — @SafeVarargs exists to promise 'never writes'");
    }

    @Test
    @DisplayName("type token: a Class<T> passed IN restores what erasure took from the method body")
    void typeTokenWorkaround() {
        GenericTrapsDemo.Vault<Transaction> vault = new GenericTrapsDemo.Vault<>(Transaction.class);
        assertSame(Transaction.class, vault.token());
        assertFalse(vault.describe().isEmpty());
        System.out.println("Vault built with token " + vault.token().getSimpleName() + ": " + vault.describe());
    }

    @Test
    @DisplayName("generic methods: static <T> is legal because T is chosen per CALL, not per class")
    void genericMethodPerCall() {
        Transaction t = GenericTrapsDemo.first(List.of(new Transaction("T-9", "x", BigDecimal.ONE)));
        String s = GenericTrapsDemo.first(List.of("Dana"));
        assertEquals("T-9", t.getId());
        assertEquals("Dana", s);
        System.out.println("same static method, two T's in one run: Transaction and String");
    }

    @Test
    @DisplayName("Java 21: Envelope<T> erases like any class, and the sealed switch stays exhaustive")
    void recordsAndSealedObeyTheSameRules() {
        RecordsSealedGenericsDemo.Envelope<Transaction> a =
                new RecordsSealedGenericsDemo.Envelope<>(new Transaction("T-1", "x", BigDecimal.ONE), "TRK-1");
        RecordsSealedGenericsDemo.Envelope<String> b =
                new RecordsSealedGenericsDemo.Envelope<>("memo", "TRK-2");

        assertSame(a.getClass(), b.getClass(), "records erase too — one Envelope.class");
        assertEquals(List.of("TRK-1", "TRK-2"),
                RecordsSealedGenericsDemo.trackingIds(List.of(a, b)));

        // The sealed family is closed — reflection can LIST every permitted implementation,
        // which is what makes the compiler's exhaustiveness check possible at all:
        Class<?>[] permitted = RecordsSealedGenericsDemo.Payment.class.getPermittedSubclasses();
        assertEquals(2, permitted.length);
        System.out.println("sealed Payment permits: " + permitted[0].getSimpleName() + ", " + permitted[1].getSimpleName());
        System.out.println("records erase like classes; sealed + switch = compiler-enforced exhaustiveness");
    }
}
