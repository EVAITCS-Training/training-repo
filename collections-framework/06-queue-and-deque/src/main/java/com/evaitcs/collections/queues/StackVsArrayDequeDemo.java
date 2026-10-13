package com.evaitcs.collections.queues;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;
import java.util.Vector;

/**
 * Demo 5 — THE LEGACY STACK: it works, it even passes every interview — and it is
 * still the wrong tool. Use ArrayDeque.
 *
 * java.util.Stack extends java.util.Vector, the Java 1.0 "growable array" from before
 * the Collections Framework existed (1.2). Inheritance hands Stack two problems it can
 * never fix:
 *
 *   1. EVERY Vector operation pays for a monitor lock — its core methods are declared
 *      synchronized and the few that are not simply delegate to ones that are. Proven
 *      below by reflecting on PUBLIC method modifiers (Modifier.isSynchronized) — no
 *      peeking into internals needed.
 *   2. A Stack IS a Vector, so callers may legally get(2) or insert in the MIDDLE —
 *      breaking the LIFO guarantee the class exists to provide. Bad inheritance:
 *      a stack is not a "list with extra rules".
 *
 * The javadoc of Deque itself says it: "Deque implementations can be used as LIFO
 * stacks... in preference to the legacy Stack class." ArrayDeque.push/pop are the same
 * names with the same meaning, minus the lock and minus the illegal side doors.
 *
 * Thread-safety is NOT what Stack's synchronization gives you anyway — compound acts
 * like "pop if non-empty" still race. Real shared-stack work belongs to module 09/10.
 */
public class StackVsArrayDequeDemo {

    public static void main(String[] args) {
        banner("1. Stack still works — push/pop/peek, exactly as expected");

        Stack<String> legacy = new Stack<String>();
        legacy.push("audit step 1");
        legacy.push("audit step 2");
        System.out.println("peek() = " + legacy.peek() + ", pop() = " + legacy.pop() + ", size = " + legacy.size());
        System.out.println("Functionally fine. So why does the JDK itself tell you not to use it?");

        banner("2. Because a Stack IS a Vector — instanceof proof");

        System.out.println("new Stack<String>() instanceof Stack  ? " + (legacy instanceof Stack));
        System.out.println("                        instanceof Vector ? " + (legacy instanceof Vector) + "   <- the family secret");
        System.out.println("                        instanceof Deque  ? " + (legacy instanceof Deque) + "  <- NOT part of the framework's Deque line");

        banner("3. ...and the inheritance leaks: callers can break LIFO legally");

        legacy.push("bottom");
        legacy.push("top");
        // Because Stack extends Vector, every List operation is public on it. Nothing
        // stops a caller from inserting in the middle — the LIFO promise is unenforceable.
        legacy.add(0, "INJECTED at index 0 — a real stack must never allow this");
        System.out.println("after legacy.add(0, ...): " + legacy);
        System.out.println("A stack you can burp into the middle of is not a stack. ArrayDeque exposes no get(i).");

        banner("4. The lock tax — reflection on PUBLIC method modifiers (no internals touched)");

        try {
            Method stackPop = Stack.class.getMethod("pop");
            Method vectorGet = Vector.class.getMethod("get", int.class);
            Method arrayDequePop = ArrayDeque.class.getMethod("pop");
            System.out.println("Stack.pop         synchronized? " + Modifier.isSynchronized(stackPop.getModifiers()) + "   <- every pop takes a lock");
            System.out.println("Vector.get(int)   synchronized? " + Modifier.isSynchronized(vectorGet.getModifiers()) + "   <- so does every Vector read");
            System.out.println("ArrayDeque.pop    synchronized? " + Modifier.isSynchronized(arrayDequePop.getModifiers()) + "  <- no lock, no tax");
        } catch (NoSuchMethodException e) {
            System.out.println("reflection failed: " + e);
        }
        System.out.println("(A few Vector methods like add(int,E) are not marked 'synchronized' themselves —");
        System.out.println("they delegate to ones that are, so the lock is unavoidable either way. Stack.push");
        System.out.println("is the same story: it calls Vector.addElement, which IS synchronized.)");
        System.out.println("Modifier.isSynchronized only READS the method's flags — public API, no setAccessible,");
        System.out.println("no java.util internals. (Module 08 runs the same trick on Vector vs ArrayList.)");

        banner("5. The recommended replacement — ArrayDeque as a stack, same verbs");

        Deque<String> modern = new ArrayDeque<String>();
        modern.push("audit step 1");
        modern.push("audit step 2");
        System.out.println("peek() = " + modern.peek() + ", pop() = " + modern.pop() + ", size = " + modern.size());
        System.out.println("Identical push/pop vocabulary, minus the per-call lock, minus the Vector side doors.");
        System.out.println("Rule: 'Deque implementations should be used as LIFO stacks in preference to the");
        System.out.println("legacy Stack class' — the JDK's own javadoc. If you truly need a SHARED stack,");
        System.out.println("that is a module 09/10 conversation (explicit synchronization or ConcurrentLinkedDeque).");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
