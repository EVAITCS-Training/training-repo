package com.evaitcs.advanced.exceptions;

import java.io.IOException;

/**
 * Demo 3 — THE CAUSE CHAIN, the swallow trap, multi-catch and precise rethrow.
 * (README "The cause chain" and "Multi-catch and precise rethrow".)
 *
 * Four claims, each printed as evidence:
 *  1. WRAPPING WITH A CAUSE keeps the origin: new RuntimeException("context", cause) links
 *     the low-level failure to the high-level one. getCause() walks back down the chain, and
 *     printStackTrace renders it as the familiar "Caused by:" lines (we render the same here).
 *  2. SWALLOWING THE CAUSE loses the origin: catch (IOException e) { throw new RuntimeException("boom"); }
 *     — the new exception has getCause() == null. The root reason is GONE. This is the trap.
 *  3. initCause() attaches a cause after construction (once only).
 *  4. MULTI-CATCH (catch (A | B e)) handles several types in one block; PRECISE RETHROW lets a
 *     method rethrow a caught Exception while declaring only the NARROW checked type it can
 *     actually propagate (Java 7's final-parameter analysis).
 */
public class CauseAndRethrowDemo {

    public static void main(String[] args) {
        banner("1. Wrap WITH the cause: the origin survives the whole way up");

        try {
            processPayment();                       // high level; wraps the low-level failure WITH the cause
        } catch (RuntimeException e) {
            System.out.println("step 1: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("step 2: walking getCause() — this is what printStackTrace shows as 'Caused by:':");
            printChain(e);
            System.out.println("step 3: e.getCause() = " + e.getCause().getClass().getSimpleName()
                    + " — the LOW-LEVEL origin is still attached.");
            System.out.println("verdict: wrapping WITH the cause gives the reader both the context AND the root.");
        }

        banner("2. The trap — rethrow WITHOUT the cause and the origin is LOST");

        try {
            processPaymentBadly();                  // high level; rethrows but DROPS the cause
        } catch (RuntimeException e) {
            System.out.println("step 1: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("step 2: walking getCause():");
            printChain(e);
            System.out.println("step 3: e.getCause() = " + e.getCause() + "  -> the IOException origin is GONE.");
            System.out.println("verdict: 'payment failed' with no cause is a dead end — you cannot debug what you threw away.");
            System.out.println("        FIX: pass the caught exception as the cause (banner 1). NEVER rethrow naked.");
        }

        banner("3. initCause(): attach a cause AFTER construction (exactly once)");

        try {
            throwLateCause();
        } catch (RuntimeException e) {
            System.out.println("step 1: built with no-arg constructor, then initCause(io) attached the origin.");
            System.out.println("step 2: CAUGHT -> " + e.getClass().getSimpleName() + "; getCause() = "
                    + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            System.out.println("        (calling initCause a SECOND time would throw IllegalStateException — it is set-once)");
            System.out.println("verdict: prefer the (message, cause) constructor; initCause is for the no-arg cases.");
        }

        banner("4. Multi-catch: ONE handler, TWO different checked types");

        // loadAndParse can fail two ways; a single `catch (IOException | ConfigException e)`
        // handles both. Inside the block, e's static type is the common supertype, but its
        // RUNTIME type is whichever was actually thrown — we print it to prove both paths ran.
        for (boolean failIo : new boolean[] { true, false }) {
            try {
                loadAndParse(failIo);
            } catch (IOException | ConfigException e) {
                System.out.println("step: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + "   (one handler, runtime type = " + e.getClass().getSimpleName() + ")");
            }
        }
        System.out.println("verdict: multi-catch removes duplicated handlers; the caught var is effectively final.");

        banner("5. Precise rethrow: catch broad, declare NARROW (Java 7 final-parameter analysis)");

        // preciseRethrow() catches Exception but declares only `throws IOException`. That compiles
        // because the try block can ONLY throw IOException, and the caught parameter is effectively
        // final — so the compiler proves `throw e` can only ever rethrow IOException.
        try {
            preciseRethrow(true);
        } catch (IOException e) {                 // the caller catches the NARROW declared type
            System.out.println("step 1: preciseRethrow() body says `catch (Exception e) { throw e; }`.");
            System.out.println("step 2: its signature declares only `throws IOException` — NOT `throws Exception`.");
            System.out.println("step 3: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + "   (caught here as the narrow IOException the compiler proved)");
            System.out.println("verdict: precise rethrow means catching Exception does not force you to declare Exception.");
        }

        banner("6. In one sentence");
        System.out.println("Always wrap WITH the cause so the origin survives; multi-catch de-duplicates handlers;");
        System.out.println("and precise rethrow lets a broad catch still declare only the narrow type it can throw.");
    }

    /** High level: catches the low-level IOException and re-throws WITH it as the cause (the right way). */
    static void processPayment() {
        try {
            chargeCard();
        } catch (IOException e) {
            throw new RuntimeException("payment could not be completed", e);   // cause attached
        }
    }

    /** High level, THE TRAP: catches the IOException but re-throws WITHOUT it — the origin is lost. */
    static void processPaymentBadly() {
        try {
            chargeCard();
        } catch (IOException e) {
            throw new RuntimeException("payment could not be completed");      // cause DROPPED
        }
    }

    /** The low-level failure at the bottom of the cause chain. */
    static void chargeCard() throws IOException {
        throw new IOException("card gateway timed out");
    }

    /** Build with the no-arg constructor, then attach the cause once via initCause(). */
    static void throwLateCause() {
        RuntimeException e = new RuntimeException("late-bound failure");
        e.initCause(new IOException("attached after construction"));
        throw e;
    }

    /** A checked exception of our own, so multi-catch has two distinct checked types to catch. */
    static class ConfigException extends Exception {
        ConfigException(String message) {
            super(message);
        }
    }

    /** Throws IOException when failIo, else ConfigException — both checked, both hit one multi-catch. */
    static void loadAndParse(boolean failIo) throws IOException, ConfigException {
        if (failIo) {
            throw new IOException("disk read failed");
        }
        throw new ConfigException("missing required key");
    }

    /** Only IOException can escape the try, so catching Exception and rethrowing still declares IOException. */
    static void preciseRethrow(boolean reallyThrow) throws IOException {
        try {
            mayThrowIo(reallyThrow);
        } catch (Exception e) {          // broad catch...
            throw e;                     // ...narrow rethrow: the compiler knows it can only be IOException
        }
    }

    static void mayThrowIo(boolean reallyThrow) throws IOException {
        if (reallyThrow) {
            throw new IOException("io failed");
        }
    }

    /** Renders the cause chain exactly as printStackTrace's "Caused by:" section would. */
    static void printChain(Throwable t) {
        int level = 0;
        for (Throwable node = t; node != null; node = node.getCause()) {
            String prefix = (level == 0) ? "thrown   : " : "Caused by: ";
            System.out.println("        " + prefix + node.getClass().getSimpleName() + ": " + node.getMessage());
            level++;
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
