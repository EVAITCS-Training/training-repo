package com.evaitcs.advanced.generics;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Demo 3 — BRIDGE METHODS: erasure's fingerprint, visible with reflection.
 * (README section "Walkthrough: how a bridge method is born".)
 *
 * The puzzle: Comparable<T> declares compareTo(T). After erasure that becomes
 * compareTo(Object). But Transaction declares compareTo(Transaction) — a DIFFERENT
 * signature. So how does Transaction satisfy the erased interface?
 *
 * Answer: javac silently generates a second method — compareTo(Object), marked
 * synthetic+bridge — that casts and delegates. Sorting machinery calls the bridge;
 * the bridge calls your method. That cast is why core-java 10's "sort a raw list of
 * non-Comparables" died with ClassCastException: it was this bridge doing the casting.
 *
 * Evidence below: list ALL declared methods of Transaction via reflection and print
 * which ones are bridges (Method.isBridge()).
 */
public class BridgeMethodsDemo {

    public static void main(String[] args) {
        banner("1. What the SOURCE says vs what the CLASS FILE contains");

        System.out.println("source   : class Transaction implements Comparable<Transaction>");
        System.out.println("           int compareTo(Transaction other)              <- ONE method written");
        System.out.println("erased   : interface Comparable { int compareTo(Object); }");
        System.out.println("problem  : compareTo(Transaction) does not override compareTo(Object)!");
        System.out.println("solution : javac GENERATES the missing override — the bridge.");

        banner("2. Reflection lists both methods");

        for (Method m : Transaction.class.getDeclaredMethods()) {
            if (!m.getName().equals("compareTo")) {
                continue;
            }
            System.out.println("found: " + m);
            System.out.println("       synthetic = " + m.isSynthetic() + "   bridge = " + m.isBridge());
        }
        System.out.println("verdict: the method you wrote (compareTo(Transaction)) and the one the JVM");
        System.out.println("         uses for the interface (compareTo(Object), bridge=true) BOTH exist.");

        banner("3. Walking the bridge, step by step");

        Transaction payroll = new Transaction("T-1040", "Payroll", new BigDecimal("2450.00"));
        Transaction rent = new Transaction("T-1011", "Rent", new BigDecimal("1200.00"));
        List<Transaction> statement = new ArrayList<>(List.of(payroll, rent));

        // Collections.sort erases to compareTo(Object) calls — it walks the bridge:
        //   bridge compareTo(Object o)  {  return compareTo((Transaction) o);  }
        // The cast inside the bridge is exactly the ClassCastException core-java 10
        // produced when a non-Comparable rode a raw list into the sort.
        Collections.sort(statement);
        System.out.println("step 1: Collections.sort called the BRIDGE compareTo(Object) on each pair");
        System.out.println("step 2: each bridge cast its argument and delegated to compareTo(Transaction)");
        System.out.println("step 3: sorted result = " + statement);

        banner("4. Bonus: bridge methods are also how COVARIANT RETURN TYPES work");

        System.out.println("class Child extends Parent { @Override Child copy() {...} }  // Parent.copy returns Parent");
        System.out.println("The JVM has no covariant returns — javac bridges Parent copy() -> Child copy() the same way.");
        System.out.println("Rule to remember: if the compiler needed a signature it cannot write, a bridge exists.");

        banner("5. In one sentence");
        System.out.println("A bridge method is the compiler-generated adapter that keeps erased");
        System.out.println("interfaces and covariant overrides honest at the JVM level — proof erasure is real.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
