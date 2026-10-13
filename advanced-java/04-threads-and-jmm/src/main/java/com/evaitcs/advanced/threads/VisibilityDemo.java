package com.evaitcs.advanced.threads;

/**
 * Demo 3 — VISIBILITY IS NOT AUTOMATIC: the spin-flag, both endings.
 * (README sections "The Java Memory Model: cached copies" and "volatile is an edge".)
 *
 * Two workers spin on a boolean flag. Main sets the flag false and waits one second.
 *   - The PLAIN (non-volatile) worker MAY never see the change and spin forever. There
 *     is no happens-before edge between main's write and the worker's read, so the JIT
 *     is free to hoist the read out of the loop — compiling while(flag.running) into
 *     while(true). Whether it reproduces depends on the JVM and the machine, so we PRINT
 *     the observed outcome and assert NOTHING (house rule for unguaranteed visibility).
 *   - The VOLATILE worker ALWAYS stops. A volatile write happens-before every subsequent
 *     volatile read of the same field, so the loop must re-read the flag each iteration
 *     and sees false.
 *
 * BOTH workers are DAEMON threads (setDaemon(true)). That is not cosmetic: the plain
 * worker might spin forever, and a daemon thread never blocks JVM exit — so the demo,
 * the launcher and the build all terminate even if the stale read reproduces.
 */
public class VisibilityDemo {

    /** A plain boolean — no visibility guarantee across threads. */
    static final class PlainFlag {
        boolean running = true;
    }

    /** A volatile boolean — every write is seen by every subsequent read. */
    static final class VolatileFlag {
        volatile boolean running = true;
    }

    public static void main(String[] args) throws Exception {
        banner("1. PLAIN boolean — the worker MAY spin forever (observed reality, not asserted)");
        PlainFlag plain = new PlainFlag();
        Thread plainWorker = new Thread(() -> {
            // The JIT may hoist plain.running out of this loop => while(true) => never sees false.
            while (plain.running) {
                Thread.onSpinWait();
            }
            System.out.println("  (plain worker DID see the write and stopped — legal, but NOT guaranteed)");
        }, "plain-spinner");
        plainWorker.setDaemon(true);                 // CRITICAL: it may never stop
        plainWorker.start();
        Thread.sleep(200);                           // let the JIT compile the hot loop (raises the odds)
        System.out.println("step 1: main sets plain.running = false — with NO happens-before edge to the worker");
        plain.running = false;
        plainWorker.join(1000);                       // give it one second to notice
        boolean plainStillSpinning = plainWorker.isAlive();
        System.out.println("step 2: after join(1000), plain worker still spinning? " + plainStillSpinning);
        System.out.println("verdict: " + (plainStillSpinning
                ? "REPRODUCED — the cached/hoisted read never refreshed. This is the JMM, not a bug in the JVM."
                : "did not reproduce this run — the write happened to be seen. Absence of a hang is NOT a guarantee."));

        banner("2. VOLATILE boolean — the worker ALWAYS stops");
        VolatileFlag vol = new VolatileFlag();
        Thread volatileWorker = new Thread(() -> {
            // A volatile read cannot be hoisted: the loop re-reads the flag every iteration.
            while (vol.running) {
                Thread.onSpinWait();
            }
            System.out.println("  (volatile worker saw the write and stopped — GUARANTEED)");
        }, "volatile-spinner");
        volatileWorker.setDaemon(true);
        volatileWorker.start();
        Thread.sleep(200);
        System.out.println("step 1: main sets vol.running = false — a VOLATILE write");
        vol.running = false;
        volatileWorker.join(1000);
        boolean volatileStopped = !volatileWorker.isAlive();
        System.out.println("step 2: after join(1000), volatile worker stopped? " + volatileStopped);
        System.out.println("verdict: a volatile write happens-before every later volatile read, so the");
        System.out.println("         worker is REQUIRED to see false and exit. This one you can assert.");

        banner("3. In one sentence");
        System.out.println("A plain field is a private rumor each thread may cache forever; a volatile field");
        System.out.println("is a written contract — write it once, and every later read is guaranteed to see it.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
