package com.evaitcs.advanced.exceptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo 4 — TRY-WITH-RESOURCES and SUPPRESSION, seen from the exception side.
 * (README "try-with-resources and suppression".)
 *
 * This is the SAME mechanism module 08 (IO & NIO) introduced when streams first needed
 * guaranteed closing. Here we look at it as an EXCEPTION mechanism, because that is what
 * makes it subtle:
 *
 *  1. Resources close in REVERSE declaration order — the outer decorator (which may depend
 *     on the inner resource it wraps) must close first.
 *  2. If the try BODY throws AND a close() also throws, the body's exception is PRIMARY
 *     (it is what propagates) and each failing close() is attached to it via addSuppressed().
 *     You read them back with getSuppressed(). Nothing is silently swallowed.
 *
 * The instrument is a Ledger (an AutoCloseable) that records WHEN it closed and can be told
 * to throw on close — exactly module 08's "Traced" idea, renamed for the banking domain.
 */
public class SuppressionDemo {

    public static void main(String[] args) throws Exception {
        banner("1. Resources close in REVERSE order of declaration");

        List<String> closeOrder = new ArrayList<>();
        try (Ledger first = new Ledger("A", closeOrder, false);
             Ledger second = new Ledger("B", closeOrder, false)) {
            System.out.println("step 1: opened A, then B (declaration order).");
        }
        System.out.println("step 2: recorded close order = " + closeOrder + "  -> B, A = REVERSE of declaration.");
        System.out.println("verdict: reverse close means a decorator is always closed BEFORE the resource it wraps");
        System.out.println("        (module 08: DataInputStream -> BufferedInputStream -> FileInputStream unwinds outer-first).");

        banner("2. Body throws AND close() throws: body is PRIMARY, close() is SUPPRESSED");

        List<String> log = new ArrayList<>();
        try {
            try (Ledger ledger = new Ledger("R", log, true)) {   // close() is rigged to throw
                System.out.println("step 1: the try BODY throws first — this becomes the PRIMARY exception.");
                throw new IllegalStateException("primary body failure");
            }
        } catch (IllegalStateException primary) {
            Throwable[] suppressed = primary.getSuppressed();
            System.out.println("step 2: CAUGHT -> " + primary.getClass().getSimpleName() + ": " + primary.getMessage());
            System.out.println("step 3: primary.getSuppressed().length = " + suppressed.length);
            for (Throwable s : suppressed) {
                System.out.println("        suppressed -> " + s.getClass().getSimpleName() + ": " + s.getMessage());
            }
            System.out.println("verdict: the body's exception wins and propagates; the failing close() rides along as");
            System.out.println("        SUPPRESSED, so you still see both. A hand-written finally would have LOST one.");
        }

        banner("3. Walkthrough — how the JVM decides primary vs suppressed");
        System.out.println("step 1: the body runs. If it completes normally, close() is called and any close() error propagates alone.");
        System.out.println("step 2: if the body throws, that throwable is remembered as the PRIMARY (call it P).");
        System.out.println("step 3: close() is still called (that is the whole guarantee). If close() throws (call it S),");
        System.out.println("        the JVM calls P.addSuppressed(S) instead of letting S replace P.");
        System.out.println("step 4: P — with S attached — propagates to the nearest matching catch.");
        System.out.println("step 5: with several resources, each failing close() is suppressed onto P, in reverse close order.");
        System.out.println("verdict: suppression keeps the FIRST failure as the headline and the rest as footnotes.");

        banner("4. In one sentence");
        System.out.println("try-with-resources guarantees close in reverse order and, when both body and close() fail,");
        System.out.println("keeps the body's exception primary and attaches close()'s as suppressed — nothing is lost.");
    }

    /**
     * An AutoCloseable that records WHEN it closed and can be told to throw on close.
     * This is the instrument that makes reverse order and suppression visible.
     */
    static final class Ledger implements AutoCloseable {
        private final String name;
        private final List<String> closeOrder;
        private final boolean throwOnClose;

        Ledger(String name, List<String> closeOrder, boolean throwOnClose) {
            this.name = name;
            this.closeOrder = closeOrder;
            this.throwOnClose = throwOnClose;
        }

        @Override
        public void close() throws IOException {
            closeOrder.add(name);                       // record the close, in the order it happens
            if (throwOnClose) {
                throw new IOException("flush failed for " + name);
            }
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
