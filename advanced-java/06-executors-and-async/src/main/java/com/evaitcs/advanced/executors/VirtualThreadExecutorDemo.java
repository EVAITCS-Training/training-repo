package com.evaitcs.advanced.executors;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Demo 4 — THREAD-PER-TASK IS CHEAP AGAIN: 10,000 virtual threads.
 * (README section "Virtual threads: thread-per-task is cheap again".)
 *
 * The claim: a virtual thread costs a few hundred bytes of HEAP stack, not ~1 MB of
 * reserved native stack, and BLOCKING one is almost free (it unmounts from its carrier).
 * So 10,000 concurrent "IO" tasks finish concurrently, and the completion count is
 * EXACT — guaranteed by ExecutorService.close() (Java 19+: shutdown + awaitTermination),
 * not by timing. Elapsed time is printed, never asserted.
 */
public class VirtualThreadExecutorDemo {

    static final int TASKS = 10_000;

    public static void main(String[] args) throws Exception {
        banner("1. " + TASKS + " tasks, each 'blocking' 1 ms — on virtual threads");

        AtomicInteger completed = new AtomicInteger();
        AtomicInteger sawVirtual = new AtomicInteger();
        CountDownLatch allDone = new CountDownLatch(TASKS);
        long start = System.nanoTime();

        // ExecutorService is AutoCloseable since Java 19 (README "Java 21 corner"):
        // close() = shutdown() + awaitTermination(), so leaving the try block PROVES
        // every submitted task finished. The count below is exact by construction.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < TASKS; i++) {
                executor.submit(() -> {
                    if (Thread.currentThread().isVirtual()) {
                        sawVirtual.incrementAndGet();
                    }
                    // 1 ms of simulated IO. A virtual thread PARKS here: it unmounts from
                    // its carrier (a platform thread), which immediately runs someone else.
                    // 10,000 parks in flight cost ~nothing; 10,000 platform threads would not.
                    LockSupport.parkNanos(1_000_000);
                    completed.incrementAndGet();
                    allDone.countDown();
                });
            }
        }

        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        // Generous deadline (30 s) as a belt-and-braces check — close() already waited,
        // so this await returns immediately on every healthy run.
        boolean latchCleared = allDone.await(30, TimeUnit.SECONDS);

        System.out.println("step 1: submitted " + TASKS + " tasks; close() returned only after ALL finished (latch cleared = " + latchCleared + ")");
        System.out.println("step 2: completed = " + completed.get() + " (EXACT — no sampling, no estimate); ran on virtual threads = " + sawVirtual.get());
        System.out.println("step 3: elapsed = " + elapsedMillis + " ms for " + TASKS + " x 1 ms of 'IO' — concurrent, not sequential (" + (TASKS) + " ms if serial)");
        System.out.println("verdict: a platform thread reserves ~1 MB of native stack at birth; a virtual thread starts");
        System.out.println("         at a few hundred bytes of heap and grows on demand. Pools existed because threads were");
        System.out.println("         scarce; virtual threads end the scarcity — for BLOCKING work. CPU-bound work still wants");
        System.out.println("         a fixed pool (you cannot run more code than you have cores).");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
