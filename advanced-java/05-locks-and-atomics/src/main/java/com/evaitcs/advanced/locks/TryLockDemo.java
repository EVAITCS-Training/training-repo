package com.evaitcs.advanced.locks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demo 2 — ReentrantLock: the monitor's pieces, sold a la carte.
 * (README section "ReentrantLock: tryLock, timeouts, fairness".)
 *
 * synchronized gives you ONE behavior: block until you get the monitor, no way to ask
 * "is it free?", no way to give up after a while, no way to be interrupted while waiting.
 * ReentrantLock sells those as separate methods:
 *   lock()/unlock()          — the same acquire/release, but EXPLICIT (you must unlock in finally)
 *   tryLock()                — non-blocking: "got it?" true/false, never waits
 *   tryLock(timeout, unit)   — "got it within the deadline?" — waits, then gives up
 *   lockInterruptibly()      — wait, but let an interrupt break you out (DeadlockDemo needs this)
 *   new ReentrantLock(fair)  — a FIFO fairness policy synchronized cannot express
 */
public class TryLockDemo {

    public static void main(String[] args) throws Exception {
        banner("1. The idiom — lock() OUTSIDE, unlock() in FINALLY");
        ReentrantLock vault = new ReentrantLock();
        vault.lock();
        try {
            System.out.println("step 1: acquired; isHeldByCurrentThread() = " + vault.isHeldByCurrentThread());
            System.out.println("step 2: reentrant getHoldCount() = " + vault.getHoldCount());
            // If this block throws, finally STILL unlocks — that is the whole discipline.
            // synchronized does this for you; ReentrantLock makes you do it, and forgetting
            // it leaks the lock forever. This try/finally is not optional.
        } finally {
            vault.unlock();
        }
        System.out.println("step 3: released in finally; isLocked() = " + vault.isLocked());
        System.out.println("verdict: lock() outside try, unlock() inside finally — every single time.");

        banner("2. tryLock() — non-blocking: fails instantly while another thread holds it");
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch holderHasLock = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                holderHasLock.countDown();
                releaseHolder.await();          // hold it until main says release
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }, "holder");
        holder.setDaemon(true);
        holder.start();
        holderHasLock.await();                   // now the holder definitely owns the lock

        boolean gotWhileHeld = lock.tryLock();   // non-blocking ask -> false, returns instantly
        System.out.println("step 1: holder owns the lock; main tryLock() -> " + gotWhileHeld + " (no waiting)");

        long start = System.nanoTime();
        boolean gotTimed = lock.tryLock(200, TimeUnit.MILLISECONDS);   // waits up to 200 ms, then gives up
        long waitedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        System.out.println("step 2: main tryLock(200 ms) -> " + gotTimed + " after waiting ~" + waitedMillis + " ms");

        releaseHolder.countDown();               // let the holder release
        holder.join();
        boolean gotAfterRelease = lock.tryLock();   // now it is free -> true
        System.out.println("step 3: holder released; main tryLock() -> " + gotAfterRelease);
        if (gotAfterRelease) {
            lock.unlock();
        }
        System.out.println("verdict: tryLock() asks without blocking; tryLock(t) waits then gives up. synchronized");
        System.out.println("         can do NEITHER — that is the piece j.u.c. sells you.");

        banner("3. Fair vs non-fair — who gets the lock next?");
        System.out.println("non-fair (the default): a newly-arriving thread may BARGE ahead of the queue.");
        System.out.println("                        Higher throughput, but a queued thread can starve briefly.");
        System.out.println("fair (new ReentrantLock(true)): grants in FIFO order — first queued, first in.");
        System.out.println("                        Predictable, slightly slower. Reach for it when starvation hurts.");
        ReentrantLock fair = new ReentrantLock(true);
        ReentrantLock nonFair = new ReentrantLock(false);
        System.out.println("fair.isFair()   = " + fair.isFair());
        System.out.println("nonFair.isFair() = " + nonFair.isFair());
        System.out.println("verdict: fairness is a constructor choice — the monitor has no such switch.");

        banner("4. In one sentence");
        System.out.println("ReentrantLock is the monitor taken apart: tryLock to ask, tryLock(t) to ask with a");
        System.out.println("deadline, lockInterruptibly to be rescuable, fairness to order the queue — your choice.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
