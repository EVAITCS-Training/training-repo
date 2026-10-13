package com.evaitcs.advanced.threads;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Demo 2 — PLATFORM THREADS vs VIRTUAL THREADS (Java 21).
 * (README section "Platform threads, virtual threads, and the carrier".)
 *
 * A PLATFORM thread is 1:1 with an OS thread — precious, ~1 MB of reserved stack each,
 * so a few thousand is already a lot. A VIRTUAL thread is scheduled by the JVM onto a
 * HANDFUL of carrier threads (platform threads, ~one per CPU). When a virtual thread
 * blocks (sleeps, waits on IO), the JVM UNMOUNTS it from its carrier and runs another —
 * so blocking costs almost nothing and you can have millions.
 *
 * The proof below: 10,000 virtual threads each sleep 100 ms. Serially that is ~16.7
 * minutes of sleep; concurrently on a few carriers it is a fraction of a second of wall
 * time. The SAME on platform threads would need 10,000 OS threads (~10 GB of stacks) —
 * we describe that, we do not run it (it would thrash or fail the machine).
 *
 * Timing here is PRINTED, never asserted (house rule) — the exact number depends on the
 * machine. The test only asserts that all 10,000 completed within a generous deadline.
 */
public class VirtualThreadsDemo {

    private static final int COUNT = 10_000;
    private static final long NAP_MILLIS = 100;

    public static void main(String[] args) throws Exception {
        banner("1. isVirtual(): which kind of thread am I on?");
        System.out.println("step 1: main thread isVirtual() = " + Thread.currentThread().isVirtual()
                + "  (main is a PLATFORM thread)");

        // Thread.ofVirtual() builds a virtual thread; isVirtual() is true inside it.
        boolean[] virtualInside = new boolean[1];
        CountDownLatch oneDone = new CountDownLatch(1);
        Thread.ofVirtual().name("scout-virtual").start(() -> {
            virtualInside[0] = Thread.currentThread().isVirtual();   // published by join() below (JMM edge)
            oneDone.countDown();
        });
        oneDone.await();                                             // edge so main can read virtualInside[0]
        System.out.println("step 2: a Thread.ofVirtual() worker reports isVirtual() = " + virtualInside[0]);
        System.out.println("step 3: carrier threads available ~= " + Runtime.getRuntime().availableProcessors()
                + " CPUs — the JVM multiplexes ALL virtual threads onto that few.");
        System.out.println("verdict: virtual threads are JVM-scheduled, not 1:1 with the OS.");

        banner("2. 10,000 virtual threads, each sleeping " + NAP_MILLIS + " ms");
        CountDownLatch allDone = new CountDownLatch(COUNT);
        long start = System.nanoTime();
        for (int i = 0; i < COUNT; i++) {
            final int id = i;
            Thread.ofVirtual().name("worker-", id).start(() -> {
                try {
                    Thread.sleep(NAP_MILLIS);          // blocking here UNMOUNTS the carrier — it is free
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    allDone.countDown();
                }
            });
        }
        boolean finished = allDone.await(30, TimeUnit.SECONDS);   // generous ceiling, not a timing assertion
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000L;

        long serialMinutes = TimeUnit.MILLISECONDS.toMinutes(COUNT * NAP_MILLIS);
        System.out.println("step 1: started " + COUNT + " virtual threads, each sleeping " + NAP_MILLIS + " ms");
        System.out.println("step 2: all finished within the deadline = " + finished);
        System.out.println("step 3: total sleep if SERIAL would be ~" + serialMinutes + " minutes");
        System.out.println("step 4: actual WALL time = " + elapsedMillis + " ms");
        System.out.println("verdict: blocking is nearly free on virtual threads — " + COUNT
                + " naps overlapped on a few carriers.");
        System.out.println("         The same on PLATFORM threads = " + COUNT
                + " OS threads (~10 GB of stacks). We describe that; we do not run it.");

        banner("3. When each is right");
        System.out.println("VIRTUAL  -> one thread per task that BLOCKS (IO, DB, HTTP). Cheap; use millions.");
        System.out.println("PLATFORM -> CPU-bound work (crunching, hashing) where a thread stays ON the core.");
        System.out.println("PINNING caveat: a virtual thread inside a synchronized block or native call is");
        System.out.println("                PINNED to its carrier and cannot unmount — module 05 is why we");
        System.out.println("                prefer ReentrantLock over synchronized in virtual-thread code.");

        banner("4. In one sentence");
        System.out.println("A virtual thread is a cheap, JVM-scheduled task that frees its carrier when it");
        System.out.println("blocks — so 'one thread per request' stops being insane and becomes the default.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
