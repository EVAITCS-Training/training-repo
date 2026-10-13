package com.evaitcs.advanced.threads;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demo 5 — INTERRUPTION IS COOPERATION, not a kill switch.
 * (README section "Interruption is a request, not a command".)
 *
 * Thread.interrupt() does NOT stop a thread. It sets a boolean FLAG on the target. The
 * target decides what to do:
 *   - Code that POLLS the flag (while (!isInterrupted())) can wind down cleanly.
 *   - Code that BLOCKS (sleep, wait, join, await) throws InterruptedException the moment
 *     the flag is set — AND THAT THROW CLEARS THE FLAG. Inside the catch, isInterrupted()
 *     is already false. This is the part everyone gets wrong.
 *
 * So the golden rule: NEVER swallow an InterruptedException. Either rethrow it, or call
 * Thread.currentThread().interrupt() to RESTORE the flag so callers up the stack can still
 * see the interruption. This demo proves all three facts deterministically (each on its
 * own thread, read back after join() — a happens-before edge, see HappensBeforeDemo).
 */
public class InterruptDemo {

    /** Plain fields, written by a worker and read after join() — the join edge publishes them. */
    static final class Facts {
        boolean flagAfterInterrupt;       // (a) is the flag set right after interrupt()?
        boolean flagAfterSecondRead;      // (a) does isInterrupted() leave it set (not clear it)?
        boolean flagInsideCatch;          // (b) did the THROW clear the flag?
        boolean flagAfterReinterrupt;     // (c) did re-interrupting restore it?
    }

    public static void main(String[] args) throws Exception {
        banner("1. Cooperative polling — the worker stops because it CHOOSES to");
        CountDownLatch started = new CountDownLatch(1);
        AtomicLong work = new AtomicLong();
        Thread cooperative = new Thread(() -> {
            started.countDown();
            // Poll the flag each turn: interrupt() is a REQUEST, and this loop honors it.
            while (!Thread.currentThread().isInterrupted()) {
                work.incrementAndGet();
                Thread.onSpinWait();
            }
            System.out.println("  (worker saw the interrupt flag and wound down after " + work.get() + " turns)");
        }, "cooperative");
        cooperative.start();
        started.await();
        cooperative.interrupt();               // just sets the flag — cooperation does the rest
        cooperative.join();
        System.out.println("step 1: worker looped while (!isInterrupted())");
        System.out.println("step 2: main called interrupt() — set the flag, forced nothing");
        System.out.println("step 3: worker exited on its own; isAlive() = " + cooperative.isAlive());
        System.out.println("verdict: interrupt is a polite request. The target must cooperate by checking.");

        banner("2. interrupt() sets a flag; isInterrupted() reads it WITHOUT clearing");
        Facts a = new Facts();
        Thread selfInterrupter = new Thread(() -> {
            Thread.currentThread().interrupt();                       // set my own flag; no blocking call
            a.flagAfterInterrupt = Thread.currentThread().isInterrupted();     // true — it is set
            a.flagAfterSecondRead = Thread.currentThread().isInterrupted();    // true — reading did NOT clear it
        }, "self-interrupter");
        selfInterrupter.start();
        selfInterrupter.join();
        System.out.println("step 1: worker called interrupt() on itself, then isInterrupted() twice");
        System.out.println("step 2: flag set after interrupt()      = " + a.flagAfterInterrupt);
        System.out.println("step 3: flag STILL set after 2nd read    = " + a.flagAfterSecondRead);
        System.out.println("verdict: isInterrupted() is a pure read. (Thread.interrupted() is the one that clears.)");

        banner("3. A blocking method THROWS on interrupt — and the throw CLEARS the flag");
        Facts b = new Facts();
        CountDownLatch blocking = new CountDownLatch(1);
        Thread sleeper = new Thread(() -> {
            blocking.countDown();
            try {
                Thread.sleep(60_000);                                  // block; interrupt will break it
            } catch (InterruptedException e) {
                b.flagInsideCatch = Thread.currentThread().isInterrupted();   // false — the throw cleared it
                Thread.currentThread().interrupt();                     // RE-INTERRUPT: restore the flag
                b.flagAfterReinterrupt = Thread.currentThread().isInterrupted(); // true — restored
            }
        }, "sleeper");
        sleeper.start();
        blocking.await();
        sleeper.interrupt();                                           // makes sleep() throw
        sleeper.join();
        System.out.println("step 1: worker blocked in Thread.sleep(60_000); main interrupted it");
        System.out.println("step 2: sleep threw InterruptedException; inside the catch isInterrupted() = "
                + b.flagInsideCatch + "  (the throw CLEARED the flag)");
        System.out.println("step 3: worker called interrupt() again; now isInterrupted() = "
                + b.flagAfterReinterrupt + "  (restored)");
        System.out.println("verdict: catching the exception does NOT leave the flag set. If you swallow it,");
        System.out.println("         the interruption is LOST for everyone above you.");

        banner("4. The idiom — never swallow it");
        System.out.println("WRONG : catch (InterruptedException e) { /* ignore */ }   // flag stays cleared, signal lost");
        System.out.println("RIGHT : catch (InterruptedException e) { Thread.currentThread().interrupt(); ... }");
        System.out.println("        — restore the flag so the caller/pool can react; OR rethrow the exception.");
        System.out.println("This is why every catch in this module re-interrupts. It is a habit, not a flourish.");

        banner("5. In one sentence");
        System.out.println("interrupt() sets a flag or breaks a block; a broken block CLEARS the flag, so");
        System.out.println("always re-interrupt (or rethrow) — swallowing an interruption loses it for good.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
