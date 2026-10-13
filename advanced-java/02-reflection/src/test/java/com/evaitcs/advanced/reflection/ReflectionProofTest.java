package com.evaitcs.advanced.reflection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 02-reflection test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC. Class identity, the forName initialize side
 * effect (via the Probe tripwire in a separate class), signatures, the module-system
 * refusal, and proxy behavior are all facts about the class file and the JDK — no timing,
 * no GC, no threads.
 */
class ReflectionProofTest {

    @Test
    @DisplayName("the three doors — literal, getClass(), forName — all return the SAME Class object")
    void classObjectIsSharedAcrossAllThreeRoutes() throws Exception {
        Account account = new Account("A-1", "Dana", new BigDecimal("250.00"));

        Class<Account> byLiteral = Account.class;
        Class<?> byInstance = account.getClass();
        Class<?> byName = Class.forName("com.evaitcs.advanced.reflection.Account");

        assertSame(byLiteral, byInstance, "literal and getClass() are the one object");
        assertSame(byInstance, byName, "forName returns the very same object");
        System.out.println("Account.class == getClass() == forName(...)  ->  " + byLiteral.getName());
    }

    @Test
    @DisplayName("Class.forName LOADS AND INITIALIZES: it runs the target's static initializer")
    void forNameRunsStaticInitializer() throws Exception {
        // The counter lives in ProbeLog, NOT Probe, so this read does not initialize Probe.
        int before = ProbeLog.INITIALIZATIONS;

        Class<?> loaded = Class.forName("com.evaitcs.advanced.reflection.Probe");

        int after = ProbeLog.INITIALIZATIONS;
        assertEquals(0, before, "Probe was untouched until forName");
        assertEquals(1, after, "forName ran Probe's static block exactly once");
        assertSame(Probe.class, loaded, "and returned Probe's one Class object");
        System.out.println("Probe initializations: before=" + before + ", after=" + after
                + " -> forName fired the static initializer");
    }

    @Test
    @DisplayName("Constructor.newInstance builds a real, working Account through reflection")
    void newInstanceBuildsWorkingAccount() throws Exception {
        Constructor<Account> ctor =
                Account.class.getDeclaredConstructor(String.class, String.class, BigDecimal.class);

        Account account = ctor.newInstance("A-9", "Dana", new BigDecimal("100.00"));

        assertEquals("A-9", account.getId());
        assertEquals("Dana", account.getOwner());
        assertEquals(0, new BigDecimal("100.00").compareTo(account.getBalance()));
        // and it is fully functional, not a hollow shell:
        assertEquals(0, new BigDecimal("150.00").compareTo(account.deposit(new BigDecimal("50.00"))));
        System.out.println("reflection-built account works: " + account);
    }

    @Test
    @DisplayName("setAccessible(true) lets reflection read AND write a private field of classpath code")
    void setAccessibleOpensPrivateField() throws Exception {
        Account account = new Account("A-1", "Dana", new BigDecimal("250.00"));
        Field owner = Account.class.getDeclaredField("owner");   // private String owner

        owner.setAccessible(true);
        assertEquals("Dana", owner.get(account), "read straight through the private field");

        owner.set(account, "Ana");
        assertEquals("Ana", account.getOwner(), "the write is visible through the normal getter");
        System.out.println("private field 'owner' read and written after setAccessible(true): " + account.getOwner());
    }

    @Test
    @DisplayName("the module system refuses setAccessible on a java.base internal (ArrayList.size)")
    void javaBaseInternalsRefuseSetAccessible() throws Exception {
        Field size = java.util.ArrayList.class.getDeclaredField("size");   // lookup is allowed...

        InaccessibleObjectException e = assertThrows(InaccessibleObjectException.class,
                () -> size.setAccessible(true));                            // ...opening it is not
        System.out.println("CAUGHT -> " + e.getClass().getSimpleName()
                + " : java.base does not open java.util — this is what --add-opens exists for");
    }

    @Test
    @DisplayName("getMethod matches EXACT parameter types: long.class finds it, Long.class does not")
    void getMethodRequiresExactParameterTypes() throws Exception {
        Method byPrimitive = Account.class.getMethod("transferCents", long.class);
        assertEquals(long.class, byPrimitive.getParameterTypes()[0], "the primitive long is found");

        assertThrows(NoSuchMethodException.class,
                () -> Account.class.getMethod("transferCents", Long.class),
                "the boxed Long is a different erased type — no such method");
        System.out.println("getMethod(\"transferCents\", long.class) -> found; (..., Long.class) -> NoSuchMethodException");
    }

    @Test
    @DisplayName("a field's generic signature survives erasure and reflection reads it back (module 01 tie-in)")
    void genericSignatureSurvivesErasure() throws Exception {
        Field statement = Account.class.getDeclaredField("statement");

        assertEquals(List.class, statement.getType(), "getType() is the RAW class...");
        assertTrue(statement.getGenericType() instanceof ParameterizedType,
                "...but getGenericType() reconstructs the declaration");

        ParameterizedType parameterized = (ParameterizedType) statement.getGenericType();
        assertEquals(Transaction.class, parameterized.getActualTypeArguments()[0]);
        System.out.println("field 'statement' generic signature recovered: " + statement.getGenericType());
    }

    @Test
    @DisplayName("a JDK dynamic proxy IS-A the interface but is NOT the concrete target class")
    void proxyImplementsInterfaceButIsNotTheTargetClass() {
        TransferService proxy =
                DynamicProxyDemo.auditProxy(new DynamicProxyDemo.SimpleLedger(), new ArrayList<>());

        assertTrue(proxy instanceof TransferService, "the proxy implements the interface");
        assertFalse(proxy instanceof DynamicProxyDemo.SimpleLedger, "but it is NOT the real class");
        assertTrue(Proxy.isProxyClass(proxy.getClass()), "it is a runtime-fabricated proxy class");
        assertTrue(proxy.getClass().getName().contains("$Proxy"), "its name looks like jdk.proxyN.$ProxyM");
        System.out.println("proxy class = " + proxy.getClass().getName()
                + " ; instanceof TransferService = true, instanceof SimpleLedger = false");
    }

    @Test
    @DisplayName("the audit proxy logs every call in order, then delegates and returns the real value")
    void proxyLogsEveryCallInOrderAndDelegates() {
        List<String> auditLog = new ArrayList<>();
        TransferService proxy =
                DynamicProxyDemo.auditProxy(new DynamicProxyDemo.SimpleLedger(), auditLog);

        String receipt1 = proxy.transfer("A", "B", new BigDecimal("30.00"));
        BigDecimal balanceA = proxy.balanceOf("A");
        String receipt2 = proxy.transfer("B", "A", new BigDecimal("10.00"));

        assertEquals(List.of("transfer", "balanceOf", "transfer"), auditLog, "calls logged in order");
        assertEquals("OK A->B 30.00", receipt1, "delegated return value from the real service");
        assertEquals(0, new BigDecimal("70.00").compareTo(balanceA), "100.00 - 30.00 = 70.00, delegated state");
        assertEquals("OK B->A 10.00", receipt2);
        System.out.println("audit log = " + auditLog + " ; delegated results = " + receipt1 + ", " + balanceA);
    }

    @Test
    @DisplayName("a cached MethodHandle and a reflective Method.invoke return the SAME result")
    void methodHandleMatchesReflectiveInvoke() throws Throwable {
        Account viaReflection = new Account("A-1", "Dana", new BigDecimal("100.00"));
        Account viaHandle = new Account("A-2", "Ana", new BigDecimal("100.00"));

        Method method = Account.class.getMethod("deposit", BigDecimal.class);
        BigDecimal reflectedResult = (BigDecimal) method.invoke(viaReflection, new BigDecimal("50.00"));

        MethodHandle handle = MethodHandles.lookup()
                .findVirtual(Account.class, "deposit", MethodType.methodType(BigDecimal.class, BigDecimal.class));
        BigDecimal handleResult = (BigDecimal) handle.invoke(viaHandle, new BigDecimal("50.00"));

        assertEquals(0, reflectedResult.compareTo(handleResult), "same method, same input, same result");
        assertEquals(0, new BigDecimal("150.00").compareTo(handleResult));
        System.out.println("Method.invoke = " + reflectedResult + " ; MethodHandle = " + handleResult + " -> equal");
    }
}
