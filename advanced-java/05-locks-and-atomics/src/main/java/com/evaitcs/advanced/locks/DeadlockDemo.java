package com.evaitcs.advanced.locks;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demo 7 — DEADLOCK: built on purpose, detected by name, then fixed twice.
 * (README section "Deadlock: the four conditions".)
 *
 * SAFETY FIRST: both deadlocking threads are DAEMONS. A deadlock is forever, and a thread
 * blocked on a monitor cannot be interrupted out of it — so if we used synchronized here,
 * the demo (and the Maven build) could never exit. We use ReentrantLock.lockInterruptibly()
 * instead, which (a) is still a real deadlock that findDeadlockedThreads() detects, and
 * (b) lets main INTERRUPT the threads to break it cleanly. That interruptibility is itself
 * one of the pieces j.u.c sells a la carte (see TryLockDemo).
 *
 * DETERMINISM: the circular wait is CHOREOGRAPHED with latches — each teller signals after
 * taking its first lock and only then reaches for the second — so the deadlock forms every
 * run. Detection polls findDeadlockedThreads() with a bounded deadline (house rule for
 * scheduler-dependent waits).
 */
public class DeadlockDemo {

    public static void main(String[] args) throws Exception {
        banner("1. The four conditions — ALL must hold, so breaking ANY one prevents deadlock");
        System.out.println("condition 1 — MUTUAL EXCLUSION: a lock is exclusive (only one holder at a time).");
        System.out.println("condition 2 — HOLD AND WAIT: a thread holds one lock while waiting for another.");
        System.out.println("condition 3 — NO PREEMPTION: a held lock cannot be forcibly taken away.");
        System.out.println("condition 4 — CIRCULAR WAIT: A waits for B, and B waits for A (a cycle).");
        System.out.println("The two fixes below break condition 4 (ordering) and condition 2 (tryLock timeout).");

        banner("2. A real deadlock, choreographed — Dana wants B, Bo wants A");
        ReentrantLock lockA = new ReentrantLock();     // guards account ACC-A
        ReentrantLock lockB = new ReentrantLock();     // guards account ACC-B
        CountDownLatch danaHoldsA = new CountDownLatch(1);
        CountDownLatch boHoldsB = new CountDownLatch(1);

        Thread dana = new Thread(() -> {
            lockA.lock();                              // Dana takes A...
            try {
                danaHoldsA.countDown();
                boHoldsB.await();                      // ...waits until Bo holds B (forces the cycle)...
                lockB.lockInterruptibly();             // ...then wants B. Bo holds it -> blocks forever.
                lockB.unlock();                        // (only reached once the deadlock is broken below)
            } catch (InterruptedException e) {
                // main interrupts to break the deadlock — this is the rescue path
            } finally {
                lockA.unlock();
            }
        }, "Dana");

        Thread bo = new Thread(() -> {
            lockB.lock();                              // Bo takes B...
            try {
                boHoldsB.countDown();
                danaHoldsA.await();                    // ...waits until Dana holds A...
                lockA.lockInterruptibly();             // ...then wants A. Dana holds it -> blocks forever.
                lockA.unlock();
            } catch (InterruptedException e) {
                // rescue path
            } finally {
                lockB.unlock();
            }
        }, "Bo");

        dana.setDaemon(true);                          // MANDATORY: a deadlock must never hold the JVM hostage
        bo.setDaemon(true);
        dana.start();
        bo.start();
        danaHoldsA.await();                            // both first-locks are now held...
        boHoldsB.await();                              // ...so the cycle is guaranteed to close

        banner("3. Detection — ThreadMXBean.findDeadlockedThreads() names the culprits");
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedIds = null;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {         // bounded poll with a deadline
            deadlockedIds = threadBean.findDeadlockedThreads();
            if (deadlockedIds != null) {
                break;
            }
            Thread.sleep(50);
        }
        if (deadlockedIds == null) {
            System.out.println("no deadlock detected (unexpected — the choreography guarantees one)");
        } else {
            ThreadInfo[] infos = threadBean.getThreadInfo(deadlockedIds);
            System.out.println("step 1: findDeadlockedThreads() returned " + deadlockedIds.length + " threads:");
            for (ThreadInfo info : infos) {
                if (info == null) {
                    continue;
                }
                System.out.println("        " + info.getThreadName() + " is blocked on " + info.getLockName()
                        + " held by " + info.getLockOwnerName());
            }
            System.out.println("verdict: DEADLOCK DETECTED and NAMED. In production this MXBean is your smoke detector.");
        }

        banner("4. Breaking it — interrupt works BECAUSE we used lockInterruptibly");
        dana.interrupt();
        bo.interrupt();
        dana.join(5_000);
        bo.join(5_000);
        System.out.println("step 1: main interrupted both threads");
        System.out.println("step 2: Dana alive = " + dana.isAlive() + ", Bo alive = " + bo.isAlive()
                + " (both exited — the blocked lockInterruptibly threw, and each released its lock)");
        System.out.println("verdict: a thread blocked on a SYNCHRONIZED monitor cannot be interrupted out — it would");
        System.out.println("         stay deadlocked and the JVM (daemon or not) could never clean up. j.u.c locks");
        System.out.println("         give you lockInterruptibly() and tryLock(t) precisely so you CAN escape.");

        banner("5. Fix 1 — lock ORDERING: everyone grabs accounts in the SAME global order");
        Account a = new Account("ACC-A", "Dana", new BigDecimal("100.00"));
        Account b = new Account("ACC-B", "Bo", new BigDecimal("50.00"));
        ReentrantLock orderLockA = new ReentrantLock();
        ReentrantLock orderLockB = new ReentrantLock();
        // A single global order by account id (Account is Comparable). Both threads take
        // ACC-A's lock before ACC-B's lock, regardless of transfer direction -> no cycle.
        CountDownLatch gun = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        List<Thread> ordered = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Thread t = new Thread(() -> {
                try {
                    gun.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                orderLockA.lock();                     // ALWAYS A first (ACC-A < ACC-B)...
                try {
                    orderLockB.lock();                 // ...then B. Same order on both threads.
                    try {
                        // transfer body would go here, holding both locks safely
                    } finally {
                        orderLockB.unlock();
                    }
                } finally {
                    orderLockA.unlock();
                }
                done.countDown();
            }, "ordered-" + i);
            ordered.add(t);
            t.start();
        }
        gun.countDown();
        boolean orderedOk = done.await(5, TimeUnit.SECONDS);
        for (Thread t : ordered) {
            t.join();
        }
        System.out.println("step 1: two threads, opposite transfer directions, but BOTH lock ACC-A then ACC-B");
        System.out.println("step 2: both completed = " + orderedOk + " (order preserved: " + a.getId() + " < " + b.getId() + ")");
        System.out.println("verdict: a global lock order makes condition 4 (circular wait) IMPOSSIBLE.");

        banner("6. Fix 2 — tryLock with a timeout: nobody holds-and-waits forever");
        ReentrantLock tryA = new ReentrantLock();
        ReentrantLock tryB = new ReentrantLock();
        CountDownLatch tryGun = new CountDownLatch(1);
        CountDownLatch tryDone = new CountDownLatch(2);
        // Deliberately the DEADLOCK order (thread 0: A then B; thread 1: B then A) — but with
        // tryLock(timeout) + backoff, a thread that cannot get the second lock RELEASES the
        // first and retries, so hold-and-wait never persists. Both make progress.
        Thread try0 = tryLockThread("try-0", tryA, tryB, tryGun, tryDone);
        Thread try1 = tryLockThread("try-1", tryB, tryA, tryGun, tryDone);
        try0.start();
        try1.start();
        tryGun.countDown();
        boolean tryOk = tryDone.await(5, TimeUnit.SECONDS);
        try0.join();
        try1.join();
        System.out.println("step 1: two threads took the locks in OPPOSITE order (the deadlock setup)...");
        System.out.println("step 2: ...but each used tryLock(timeout) and backed off; both completed = " + tryOk);
        System.out.println("verdict: tryLock(t) breaks condition 2 (hold and wait) — you can always let go and retry.");

        banner("7. In one sentence");
        System.out.println("Deadlock needs all four conditions; order your locks to break circular wait, or use");
        System.out.println("tryLock-with-timeout to break hold-and-wait — and detect the rest with ThreadMXBean.");
    }

    /** A thread that grabs `first`, then tryLocks `second` with a timeout, backing off on failure. */
    private static Thread tryLockThread(String name, ReentrantLock first, ReentrantLock second,
                                        CountDownLatch gun, CountDownLatch done) {
        Thread t = new Thread(() -> {
            try {
                gun.await();
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                boolean transferred = false;
                while (!transferred && System.nanoTime() < deadline) {
                    if (first.tryLock(50, TimeUnit.MILLISECONDS)) {
                        try {
                            if (second.tryLock(50, TimeUnit.MILLISECONDS)) {
                                try {
                                    transferred = true;   // got both -> the transfer would happen here
                                } finally {
                                    second.unlock();
                                }
                            }
                        } finally {
                            first.unlock();               // ALWAYS release first -> never hold-and-wait
                        }
                    }
                    if (!transferred) {
                        Thread.sleep(10);                 // back off before retrying
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }, name);
        t.setDaemon(true);
        return t;
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
