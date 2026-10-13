package com.evaitcs.advanced.executors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Demo 1 — WHY POOLS EXIST, AND WHAT IS INSIDE ONE.
 * (README sections "Why pools exist" and "Under the hood: the anatomy of a
 * ThreadPoolExecutor".)
 *
 * The claim: creating a raw thread per task is expensive and unbounded; a pool is
 * reuse + a work queue + an admission policy. All four dials of ThreadPoolExecutor
 * (core size, max size, queue, RejectedExecutionHandler) are printed from a LIVE pool,
 * and rejection is PROVOKED deterministically with latches — never with sleep-and-hope.
 *
 * Interrupt cooperation in banner 4 ties back to module 04: shutdownNow() only
 * INTERRUPTS; a task that ignores the flag keeps running.
 */
public class PoolAnatomyDemo {

    public static void main(String[] args) throws Exception {
        banner("1. The cost a pool saves you: raw threads vs pool reuse");

        // Creating a platform thread = a native OS call + ~1 MB of reserved stack.
        // We measure it, we PRINT it, we never assert it (timings are machine-dependent).
        long rawStart = System.nanoTime();
        List<Thread> rawThreads = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            Thread t = new Thread(() -> { });
            rawThreads.add(t);
            t.start();
        }
        for (Thread t : rawThreads) {
            t.join();
        }
        long rawMillis = (System.nanoTime() - rawStart) / 1_000_000;

        ExecutorService pool = Executors.newFixedThreadPool(8);
        long poolStart = System.nanoTime();
        List<Future<?>> pooled = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            pooled.add(pool.submit(() -> { }));
        }
        for (Future<?> f : pooled) {
            f.get();
        }
        long poolMillis = (System.nanoTime() - poolStart) / 1_000_000;
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("step 1: 200 RAW threads, created + joined      = " + rawMillis + " ms (printed, never asserted)");
        System.out.println("step 2: 200 tasks on a POOL of 8, all joined   = " + poolMillis + " ms");
        System.out.println("verdict: the pool rented out 8 threads 200 times; the raw version paid the OS 200 times.");
        System.out.println("         Unbounded threads would also mean unbounded stack memory + context-switch death.");

        banner("2. The four dials, read from a LIVE ThreadPoolExecutor");

        // newFixedThreadPool returns a ThreadPoolExecutor wearing the ExecutorService
        // interface. Casting it back exposes the anatomy from the README diagram.
        ThreadPoolExecutor fixed = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        System.out.println("step 1: corePoolSize        = " + fixed.getCorePoolSize() + "   (threads kept ready even when idle)");
        System.out.println("step 2: maximumPoolSize     = " + fixed.getMaximumPoolSize() + "   (hard ceiling on threads)");
        System.out.println("step 3: workQueue           = " + fixed.getQueue().getClass().getSimpleName() + " (unbounded: max is never reached)");
        System.out.println("step 4: rejectedHandler     = " + fixed.getRejectedExecutionHandler().getClass().getSimpleName() + " (throws RejectedExecutionException)");
        fixed.shutdown();
        fixed.awaitTermination(5, TimeUnit.SECONDS);

        banner("3. Reuse PROVEN: six tasks, a 2-thread pool, thread NAMES collected");

        ExecutorService twoPool = Executors.newFixedThreadPool(2);
        Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());
        List<Future<?>> six = new ArrayList<>();
        for (int task = 1; task <= 6; task++) {
            final int id = task;
            six.add(twoPool.submit(() -> {
                threadNames.add(Thread.currentThread().getName());
                // No step numbers here ON PURPOSE: these six lines interleave in whatever
                // order two threads manage — the shuffled output IS the concurrency proof.
                System.out.println("task " + id + " ran on " + Thread.currentThread().getName());
                try {
                    // Generous floor (>= 150 ms) so BOTH pool threads are provably busy at
                    // once; the <= 2 distinct-names verdict below does NOT depend on timing —
                    // a fixed pool of 2 can never create a third thread, races or not.
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();   // module 04: restore the flag, never swallow it
                }
            }));
        }
        for (Future<?> f : six) {
            f.get();   // join every task BEFORE counting: determinism by waiting, not by hoping
        }
        twoPool.shutdown();
        boolean terminated = twoPool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("step 1: all six futures joined — every task provably finished before we count");
        System.out.println("step 2: distinct thread names across the 6 tasks = " + threadNames.size() + " " + threadNames);
        System.out.println("step 3: pool terminated cleanly = " + terminated);
        System.out.println("verdict: 6 tasks, " + threadNames.size() + " threads — at most 2, guaranteed by pool size, not by timing.");

        banner("4. Rejection PROVOKED deterministically: tiny pool + bounded queue + AbortPolicy");

        // README walkthrough step 4: core busy -> queue full -> cannot grow to max -> REJECT.
        // To force all three conditions WITHOUT racing, the single worker parks on a latch
        // we control: while it waits, the queue (capacity 1) fills, and the next submit dies.
        CountDownLatch occupantStarted = new CountDownLatch(1);
        CountDownLatch releaseOccupant = new CountDownLatch(1);
        ThreadPoolExecutor tiny = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1));   // BOUNDED queue: capacity exactly 1
        try {
            tiny.execute(() -> {
                occupantStarted.countDown();
                try {
                    releaseOccupant.await();   // holds the ONLY worker hostage, on cue
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            occupantStarted.await();           // start gun: the core thread is now provably busy
            tiny.execute(() -> { });           // walks step 2: the queue has room for exactly this one
            System.out.println("step 1: core thread busy (latch-held), queue holds 1 task — pool is SATURATED");
            try {
                tiny.execute(() -> { });       // step 3 impossible (max == core), so step 4 fires
                System.out.println("step 2: NOT rejected — impossible with this wiring");
            } catch (RejectedExecutionException e) {
                System.out.println("step 2: CAUGHT -> " + e.getClass().getSimpleName() + ": " + abbreviate(e.getMessage()));
                System.out.println("WHY: AbortPolicy is the pool's admission control — busy core, full queue,");
                System.out.println("     max reached. Refusing loudly beats queueing without limit (OOM later).");
            }
        } finally {
            releaseOccupant.countDown();       // let the hostage go so shutdown can finish
            tiny.shutdown();
            System.out.println("step 3: occupant released; terminated cleanly = " + tiny.awaitTermination(5, TimeUnit.SECONDS));
        }

        banner("5. Graceful shutdown: shutdown() vs shutdownNow() (interrupt cooperation, module 04)");

        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch cooperativeStarted = new CountDownLatch(1);
        workers.submit(() -> {
            cooperativeStarted.countDown();
            try {
                Thread.sleep(10_000);          // "long task": only ends early if it COOPERATES with interrupt
                System.out.println("sleeper finished naturally (never happens here)");
            } catch (InterruptedException e) {
                System.out.println("step 2: sleeper felt the interrupt and stopped — cooperation works (module 04)");
            }
        });
        cooperativeStarted.await();
        workers.shutdown();                    // stop ACCEPTING; queued/running tasks finish naturally
        System.out.println("step 1: shutdown() called — isShutdown = " + workers.isShutdown()
                + ", but the sleeper still runs (shutdown never interrupts)");
        workers.shutdownNow();                 // now INTERRUPT the running tasks (a hint, not a kill)
        System.out.println("step 3: shutdownNow() interrupted it; terminated = "
                + workers.awaitTermination(5, TimeUnit.SECONDS));
        System.out.println("verdict: shutdown() drains the queue politely; shutdownNow() interrupts — and a task");
        System.out.println("         that ignores the interrupt flag keeps running. awaitTermination() is the ONLY proof.");
    }

    /** Rejection messages embed a task's toString(); keep the demo line readable. */
    private static String abbreviate(String message) {
        return message != null && message.length() > 90 ? message.substring(0, 90) + "..." : message;
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
