package com.evaitcs.advanced.jvm;

import java.util.function.BooleanSupplier;

/**
 * The module's ONE bounded-retry helper for everything GC-dependent — demos and tests
 * share it, so no code path in this module ever sleeps-and-hopes.
 *
 * WHY a helper at all: {@code System.gc()} is a HINT, not a command. The Java spec
 * guarantees nothing; on default HotSpot configurations (no -XX:+DisableExplicitGC) the
 * JVM honors the hint with a full collection — which is why "request, observe, retry
 * until a deadline" is deterministic in practice WITHOUT asserting anything about the
 * collector itself. Even a honored full GC occasionally needs a second pass to process
 * references lazily, so we loop instead of trusting one call.
 *
 * The rule this encodes (README "The traps", trap 1): NEVER assert GC counts, heap
 * sizes, or timings. Ask instead: "did the OBSERVABLE state arrive before the deadline?"
 * That is a yes/no the default JVM answers reliably, on every run.
 */
public final class GcProbe {

    /** The module-wide deadline for GC-dependent waiting: 5 seconds (task spec). */
    public static final long DEADLINE_MS = 5_000;

    private GcProbe() { }

    /**
     * Requests {@code System.gc()} repeatedly until {@code condition} holds or the
     * deadline passes.
     *
     * @param condition  the observable we wait for — a cleared weak reference, a shrunk
     *                   map, an enqueued reference; re-evaluated after every attempt
     * @param deadlineMs maximum wall time to keep retrying, in milliseconds
     * @return true if (and only if) the condition holds when we stop retrying
     */
    public static boolean collectUntil(BooleanSupplier condition, long deadlineMs) {
        long deadlineNanos = System.nanoTime() + deadlineMs * 1_000_000L;
        int attempts = 0;
        while (true) {
            attempts++;
            System.gc();                       // the HINT — default HotSpot runs a full GC
            if (condition.getAsBoolean()) {
                System.out.println("  (GcProbe: condition met after " + attempts + " System.gc() attempt(s))");
                return true;
            }
            if (System.nanoTime() >= deadlineNanos) {
                return false;                  // deadline passed with the condition still false
            }
            try {
                Thread.sleep(50);              // a beat for lazy reference processing, then retry
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return condition.getAsBoolean();
            }
        }
    }
}
