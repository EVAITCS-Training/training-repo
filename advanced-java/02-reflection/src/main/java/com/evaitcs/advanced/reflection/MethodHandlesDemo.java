package com.evaitcs.advanced.reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.math.BigDecimal;

/**
 * Demo 5 — METHODHANDLES: the fast modern path. (README section "MethodHandles vs
 * Method.invoke".)
 *
 * Method.invoke is the classic reflective call: it re-checks access, boxes arguments, and
 * goes through a native trampoline every time. A MethodHandle is a typed, pre-resolved
 * "function pointer" to a method. Resolve it ONCE with a Lookup, then invoke it as often
 * as you like — the JIT can inline a hot handle the way it inlines a direct call.
 *
 * Both routes run the SAME method and return the SAME result. Frameworks that call your
 * code millions of times (serializers, DI containers) resolve a handle once and CACHE it,
 * which is why they prefer MethodHandles over raw Method.invoke in the hot path.
 */
public class MethodHandlesDemo {

    public static void main(String[] args) throws Throwable {
        banner("1. Classic reflective call — Method.invoke");

        Account viaReflection = new Account("A-1", "Dana", new BigDecimal("100.00"));
        Method deposit = Account.class.getMethod("deposit", BigDecimal.class);
        BigDecimal reflectedResult = (BigDecimal) deposit.invoke(viaReflection, new BigDecimal("50.00"));
        System.out.println("step 1: resolved Method 'deposit' by name + parameter types");
        System.out.println("step 2: deposit.invoke(account, 50.00) -> " + reflectedResult.toPlainString());

        banner("2. Modern path — MethodHandles.lookup().findVirtual");

        Account viaHandle = new Account("A-2", "Ana", new BigDecimal("100.00"));
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        // MethodType reads: RETURN type first, then the PARAMETER types — same erased types
        // Method.invoke would match. long.class vs Long.class exactness applies here too.
        MethodType depositType = MethodType.methodType(BigDecimal.class, BigDecimal.class);
        MethodHandle depositHandle = lookup.findVirtual(Account.class, "deposit", depositType);
        BigDecimal handleResult = (BigDecimal) depositHandle.invoke(viaHandle, new BigDecimal("50.00"));
        System.out.println("step 1: lookup.findVirtual(Account.class, \"deposit\", (BigDecimal)BigDecimal) -> a MethodHandle");
        System.out.println("step 2: handle.invoke(account, 50.00) -> " + handleResult.toPlainString());

        banner("3. Same method, same result");

        System.out.println("Method.invoke result     = " + reflectedResult.toPlainString());
        System.out.println("MethodHandle result      = " + handleResult.toPlainString());
        System.out.println("equal?                   = " + (reflectedResult.compareTo(handleResult) == 0));
        System.out.println("verdict: two doors to one method. The handle is the one a hot loop should cache.");

        banner("4. Why frameworks cache the handle");

        System.out.println("Method.invoke re-checks access and boxes on EVERY call. A MethodHandle is resolved");
        System.out.println("once; invokeExact (with exact static types) lets the JIT inline it like a direct call.");
        System.out.println("So a serializer resolves a handle per field at startup, then reuses it for every object.");

        banner("5. In one sentence");
        System.out.println("MethodHandles are the typed, pre-resolved, JIT-friendly version of Method.invoke —");
        System.out.println("resolve once, invoke often, cache it — which is exactly what fast frameworks do.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
