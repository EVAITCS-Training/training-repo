package com.evaitcs.corejava.builders;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The thread tests — the honest ones.
 *
 * What we ASSERT:   the StringBuffer result is EXACT, every run (synchronized append).
 * What we DON'T:    that a shared StringBuilder breaks. A race can get lucky; asserting
 *                   "must corrupt" would be a flaky test. We assert only the invariant
 *                   that can never be violated (never MORE than expected) and print the
 *                   damage as evidence. Deterministic assertions, no timing assertions.
 */
class ThreadSafetyTest {

    private static final int THREADS = 8;
    private static final int APPENDS_PER_THREAD = 5_000;
    private static final int EXPECTED = THREADS * APPENDS_PER_THREAD;

    @Test
    @DisplayName("shared StringBuffer under 8 racing threads: length is EXACT, every run")
    void bufferIsCorrectUnderThreads() throws InterruptedException {
        StringBuffer buffer = new StringBuffer();
        int failures = hammer(buffer);

        System.out.println("StringBuffer  : expected=" + EXPECTED
                + ", actual=" + buffer.length()
                + ", thread exceptions=" + failures);

        assertEquals(0, failures, "a synchronized append can never throw mid-race");
        assertEquals(EXPECTED, buffer.length(), "every single append must survive the race");
    }

    @Test
    @DisplayName("shared StringBuilder under the same race: never invents data; damage is evidence, not law")
    void builderDamageIsPrintedButNotAssertedAsGuaranteed() throws InterruptedException {
        int damagedRuns = 0;
        for (int attempt = 1; attempt <= 3; attempt++) {
            StringBuilder builder = new StringBuilder();
            int failures = hammer(builder);
            int lost = EXPECTED - builder.length();

            System.out.println("StringBuilder : attempt " + attempt
                    + ", expected=" + EXPECTED
                    + ", actual=" + builder.length()
                    + ", lost=" + lost
                    + ", thread exceptions=" + failures);

            // The one invariant that ALWAYS holds: racy appends can lose data or throw,
            // but they can never produce MORE than the number of append calls.
            assertTrue(builder.length() <= EXPECTED, "a race may lose appends, never invent them");
            if (lost > 0 || failures > 0) {
                damagedRuns++;
            }
        }
        System.out.println("runs showing damage (lost appends or exceptions): " + damagedRuns + "/3");
        System.out.println("(deliberately NOT asserted: whether a race manifests depends on the machine,");
        System.out.println(" and a flaky test teaches nothing. The demo + these numbers are the evidence.)");
    }

    /**
     * The race harness, identical to BufferVsBuilderThreadDemo's: all threads wait on a
     * CountDownLatch start gate, then append as fast as they can. Typed as Appendable —
     * the interface BOTH classes implement — so the harness is written once.
     */
    private static int hammer(final Appendable shared) throws InterruptedException {
        final CountDownLatch startGate = new CountDownLatch(1);
        final AtomicInteger failures = new AtomicInteger();
        Thread[] threads = new Thread[THREADS];
        for (int t = 0; t < THREADS; t++) {
            threads[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        startGate.await();
                        for (int i = 0; i < APPENDS_PER_THREAD; i++) {
                            shared.append("x");
                        }
                    } catch (Throwable e) {
                        // shared StringBuilder: check-then-act on count+array without a lock
                        // => lost updates, or ArrayIndexOutOfBoundsException when the array
                        // is swapped mid-append. Count it, never let it fail the run.
                        failures.incrementAndGet();
                    }
                }
            });
            threads[t].start();
        }
        startGate.countDown();
        for (Thread thread : threads) {
            thread.join();
        }
        return failures.get();
    }
}
