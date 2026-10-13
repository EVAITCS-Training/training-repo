package com.evaitcs.advanced.threads;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Demo 1 — THE SIX THREAD STATES, caught deterministically.
 * (README section "The lifecycle: six states, one enum".)
 *
 * Every Java thread is always in exactly one of six Thread.State values: NEW, RUNNABLE,
 * BLOCKED, WAITING, TIMED_WAITING, TERMINATED. Reading a live thread's state is usually
 * a race — but this demo CHOREOGRAPHS each state with a latch or a monitor so getState()
 * returns the state we are teaching, every run.
 *
 * The distinction that trips everyone up, made visible here:
 *   BLOCKED       = waiting for a MONITOR LOCK specifically (a synchronized block held by another)
 *   WAITING       = parked on a latch/queue with no deadline (await(), wait())
 *   TIMED_WAITING = the same park, but with a deadline (sleep(ms), await(ms))
 * They look alike in prose and are three different mechanisms. This demo parks a thread
 * in each and prints the difference.
 *
 * WHERE THE POLLING IS: reaching a target state depends on the scheduler, so awaitState()
 * uses a bounded retry with a deadline (house rule for scheduler-dependent waits). The
 * TARGET is guaranteed by the choreography — main holds the monitor, so BLOCKED is certain;
 * the worker parks on a latch, so WAITING is certain — only the instant it lands varies.
 */
public class LifecycleDemo {

    public static void main(String[] args) throws Exception {
        banner("1. NEW — allocated, not yet started");
        Thread newborn = new Thread(() -> System.out.println("  (this line runs in section 6)"), "newborn");
        System.out.println("step 1: constructed a Thread but did NOT call start()");
        System.out.println("step 2: newborn.getState() = " + newborn.getState());
        System.out.println("verdict: NEW means 'the object exists, the execution stream does not yet'.");

        banner("2. RUNNABLE — running now or ready to run");
        CountDownLatch spinning = new CountDownLatch(1);
        AtomicBoolean stopSpin = new AtomicBoolean(false);   // the visibility edge main->worker (JMM)
        Thread runner = new Thread(() -> {
            spinning.countDown();                            // signal: I am about to spin
            while (!stopSpin.get()) {
                Thread.onSpinWait();                         // burn CPU => genuinely RUNNABLE
            }
        }, "runner");
        runner.start();
        spinning.await();                                    // wait until the worker is in its loop
        awaitState(runner, Thread.State.RUNNABLE, 5_000);
        System.out.println("step 1: started a worker that busy-spins until told to stop");
        System.out.println("step 2: runner.getState() = " + runner.getState());
        System.out.println("verdict: RUNNABLE covers 'on a CPU' AND 'waiting for the OS to schedule it'.");
        stopSpin.set(true);
        runner.join();

        banner("3. BLOCKED — waiting for a MONITOR another thread holds");
        Object monitor = new Object();
        Thread blocked = new Thread(() -> {
            synchronized (monitor) {                         // cannot enter: main owns the monitor
                System.out.println("  (worker got the monitor after main released it)");
            }
        }, "blocked");
        synchronized (monitor) {                             // MAIN takes and HOLDS the monitor
            blocked.start();
            awaitState(blocked, Thread.State.BLOCKED, 5_000); // guaranteed: the lock is ours
            System.out.println("step 1: main entered synchronized(monitor) and holds it");
            System.out.println("step 2: started a worker that tries to enter the SAME synchronized(monitor)");
            System.out.println("step 3: blocked.getState() = " + blocked.getState());
            System.out.println("verdict: BLOCKED means ONE thing — queued for a monitor lock. Not a latch, a LOCK.");
        }                                                    // main releases the monitor here
        blocked.join();

        banner("4. WAITING — parked on a latch, no deadline");
        CountDownLatch gate = new CountDownLatch(1);
        Thread waiter = new Thread(() -> {
            try {
                gate.await();                                // parks indefinitely => WAITING
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "waiter");
        waiter.start();
        awaitState(waiter, Thread.State.WAITING, 5_000);      // guaranteed: await() parks
        System.out.println("step 1: started a worker parked on latch.await() with no timeout");
        System.out.println("step 2: waiter.getState() = " + waiter.getState());
        System.out.println("verdict: WAITING means 'I will not proceed until someone signals me'. No clock.");
        gate.countDown();                                    // release the waiter
        waiter.join();

        banner("5. TIMED_WAITING — parked, but with a deadline");
        Thread sleeper = new Thread(() -> {
            try {
                Thread.sleep(60_000);                        // long nap => TIMED_WAITING
            } catch (InterruptedException e) {
                System.out.println("  (sleep cut short by interrupt — that is the plan)");
            }
        }, "sleeper");
        sleeper.start();
        awaitState(sleeper, Thread.State.TIMED_WAITING, 5_000); // guaranteed: sleep(ms) parks with a deadline
        System.out.println("step 1: started a worker in Thread.sleep(60_000)");
        System.out.println("step 2: sleeper.getState() = " + sleeper.getState());
        System.out.println("verdict: TIMED_WAITING = WAITING plus a clock. It wakes on the deadline OR a signal.");
        sleeper.interrupt();                                 // end the 60s nap now (cooperation — demo 5)
        sleeper.join();

        banner("6. TERMINATED — the run() method has finished");
        newborn.start();                                     // reuse the NEW thread from section 1
        newborn.join();                                        // join() returns only after run() completes
        System.out.println("step 1: started the thread from section 1 and join()ed it");
        System.out.println("step 2: newborn.getState() = " + newborn.getState());
        System.out.println("verdict: TERMINATED is final — a dead thread cannot be restarted.");

        banner("7. In one sentence");
        System.out.println("A thread is always in exactly one of six states; BLOCKED is a monitor queue,");
        System.out.println("WAITING/TIMED_WAITING are latch/queue parks, and getState() tells them apart.");
    }

    /**
     * Poll until the thread reaches the target state, or give up after the deadline.
     * The TARGET is guaranteed by the surrounding choreography (a held monitor, a latch);
     * only the exact instant the scheduler lands the thread there varies — hence bounded
     * retry with a deadline instead of a fixed sleep (house rule for scheduler waits).
     */
    static void awaitState(Thread thread, Thread.State target, long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (thread.getState() != target) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError(
                        "thread '" + thread.getName() + "' never reached " + target + "; last seen " + thread.getState());
            }
            Thread.sleep(1);   // yield the CPU so the target thread can transition
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
