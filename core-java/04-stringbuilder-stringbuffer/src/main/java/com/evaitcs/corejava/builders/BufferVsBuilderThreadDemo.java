package com.evaitcs.corejava.builders;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The synchronized sibling — and the HONEST thread demo.
 *
 * History first: StringBuffer came FIRST (JDK 1.0, 1996) — every mutator is
 * `synchronized`. StringBuilder was added in Java 5 (2004) with the identical
 * API minus the locks, because in the overwhelming majority of real code the
 * builder is a LOCAL variable no other thread can see, and locking it is pure
 * overhead.
 *
 * This demo uses plain Threads and one CountDownLatch "start gate" — the gate is
 * a prop so all threads charge at the SAME instant; concurrency as a subject
 * starts later in the course, here it only serves the buffer-vs-builder point.
 */
public class BufferVsBuilderThreadDemo {

    private static final int THREADS = 8;
    private static final int APPENDS_PER_THREAD = 5_000;
    private static final int EXPECTED = THREADS * APPENDS_PER_THREAD;

    public static void main(String[] args) throws Exception {

        banner("1. Identical API — the only difference is the `synchronized` flag");
        Method bufferAppend = StringBuffer.class.getMethod("append", String.class);
        Method builderAppend = StringBuilder.class.getMethod("append", String.class);
        System.out.println("StringBuffer.append(String)  is synchronized? "
                + Modifier.isSynchronized(bufferAppend.getModifiers()));
        System.out.println("StringBuilder.append(String) is synchronized? "
                + Modifier.isSynchronized(builderAppend.getModifiers()));
        System.out.println("Both inherit the real work from AbstractStringBuilder (char[] value + int count);");
        System.out.println("Buffer re-declares every mutator `synchronized`, Builder does not. That's the diff.");

        banner("2. " + THREADS + " threads x " + APPENDS_PER_THREAD + " appends into a SHARED StringBuffer");
        StringBuffer buffer = new StringBuffer();
        int bufferFailures = hammer(buffer);
        System.out.println("expected length : " + EXPECTED);
        System.out.println("actual length   : " + buffer.length());
        System.out.println("thread failures : " + bufferFailures);
        System.out.println("=> correct EVERY run: the lock makes each append atomic.");

        banner("3. The exact same race with a SHARED StringBuilder");
        StringBuilder builder = new StringBuilder();
        int builderFailures = hammer(builder);
        System.out.println("expected length : " + EXPECTED);
        System.out.println("actual length   : " + builder.length());
        System.out.println("lost appends    : " + (EXPECTED - builder.length()));
        System.out.println("thread failures : " + builderFailures + "  (ArrayIndexOutOfBoundsException, caught below)");
        System.out.println("=> WHY: append is check-then-act — read count, ensure room, copy, write count.");
        System.out.println("   Two threads read count=5, both write into slots 5.., both set count=6:");
        System.out.println("   one append vanishes (lost update). If one thread swaps the array mid-append,");
        System.out.println("   the other copies into the stale array -> ArrayIndexOutOfBoundsException.");
        System.out.println("   Honest framing: a race may get LUCKY on some runs — 'it worked' proves nothing.");

        banner("4. The RULE (say this out loud)");
        System.out.println("builder = the default. 99% of real builders are LOCAL variables, and a local");
        System.out.println("          cannot be reached by other threads — locking would buy nothing.");
        System.out.println("buffer  = only when ONE instance is genuinely shared across threads (rare).");
        System.out.println("          Often better: give each thread its OWN builder and merge the results.");
    }

    /**
     * Runs the race: every thread waits on the start gate, then appends as fast as it can.
     * `shared` is typed as Appendable — the interface BOTH classes implement — so this
     * classic-idiom loop is written once and used for Buffer and Builder alike.
     * Returns the number of threads that blew up (only possible for the Builder).
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
                        startGate.await();                  // everyone waits at the line...
                        for (int i = 0; i < APPENDS_PER_THREAD; i++) {
                            shared.append("x");             // ...then everyone pushes at once
                        }
                    } catch (Throwable e) {
                        // A shared StringBuilder can throw mid-append (two threads growing the
                        // same array). Catch and COUNT it — a demo must never die on stage.
                        failures.incrementAndGet();
                    }
                }
            });
            threads[t].start();
        }
        startGate.countDown();                              // the starting gun
        for (Thread thread : threads) {
            thread.join();
        }
        return failures.get();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
