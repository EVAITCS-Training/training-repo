package com.evaitcs.advanced.locks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demo 1 — THE MONITOR: reentrancy, ownership, and wait-releases-the-lock.
 * (README sections "Every object has a monitor" and "wait/notify need ownership".)
 *
 * Three facts about the monitor (the lock behind every `synchronized`), each shown live:
 *   1. REENTRANT — a thread that already holds a monitor can re-acquire it without
 *      blocking. The monitor keeps a HOLD COUNT. Without this, a synchronized method
 *      calling another synchronized method on the same object would deadlock itself.
 *   2. OWNERSHIP — wait()/notify() only work while you HOLD the monitor. Call them
 *      without it and you get IllegalMonitorStateException, deterministically.
 *   3. wait() RELEASES the monitor (so others can enter) while sleep() KEEPS it. This is
 *      the module-04 sleep-vs-wait table, now proven with two real threads.
 */
public class MonitorReentrancyDemo {

    public static void main(String[] args) throws Exception {
        banner("1. Reentrancy — the same thread re-acquires its OWN monitor (hold count)");
        Object monitor = new Object();
        synchronized (monitor) {                       // hold count 1
            System.out.println("step 1: entered synchronized(monitor) — hold count conceptually 1");
            synchronized (monitor) {                   // hold count 2 — SAME thread, does NOT block
                System.out.println("step 2: re-entered the SAME monitor — hold count conceptually 2");
                synchronized (monitor) {               // hold count 3
                    System.out.println("step 3: re-entered again — hold count conceptually 3");
                }
                System.out.println("step 4: exited innermost — hold count back to 2");
            }
            System.out.println("step 5: exited — hold count back to 1");
        }
        System.out.println("step 6: fully released — hold count 0");
        System.out.println("verdict: a synchronized method calling another on 'this' cannot self-deadlock.");

        // ReentrantLock makes the invisible hold count VISIBLE — same model, explicit API.
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        System.out.println("ReentrantLock.getHoldCount() after 1 lock()   = " + lock.getHoldCount());
        lock.lock();
        System.out.println("ReentrantLock.getHoldCount() after 2 lock()   = " + lock.getHoldCount()
                + "  (reentrant, just like the monitor)");
        lock.unlock();
        lock.unlock();
        System.out.println("ReentrantLock.getHoldCount() after 2 unlock() = " + lock.getHoldCount());

        banner("2. wait()/notify() REQUIRE monitor ownership — IllegalMonitorStateException");
        Object unowned = new Object();
        try {
            unowned.wait(1);   // no synchronized(unowned) around it -> illegal
        } catch (IllegalMonitorStateException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("WHY: wait() means 'release THIS monitor and park'. You cannot release a lock");
            System.out.println("     you never held, so the JVM refuses before it can corrupt the wait set.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // unreachable here, but never swallow it
        }
        try {
            unowned.notify();  // same rule for notify
        } catch (IllegalMonitorStateException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from notify() too — same ownership rule.");
        }
        System.out.println("verdict: wait/notify are monitor operations; ownership is mandatory (module 05's Condition");
        System.out.println("         queues enforce the same rule but with a clearer API).");

        banner("3a. sleep() KEEPS the monitor — a second thread CANNOT enter");
        Object sleepLock = new Object();
        CountDownLatch aHoldsDuringSleep = new CountDownLatch(1);
        long[] bBlockedMillis = { -1 };
        Thread sleeper = new Thread(() -> {
            synchronized (sleepLock) {
                aHoldsDuringSleep.countDown();
                try {
                    Thread.sleep(300);            // holds the monitor the WHOLE 300 ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "sleeper");
        Thread prober = new Thread(() -> {
            try {
                aHoldsDuringSleep.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            long start = System.nanoTime();
            synchronized (sleepLock) {            // blocked until sleeper exits (sleep kept the lock)
                bBlockedMillis[0] = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            }
        }, "prober");
        sleeper.start();
        prober.start();
        sleeper.join();
        prober.join();
        System.out.println("step 1: sleeper held the monitor and slept 300 ms");
        System.out.println("step 2: prober blocked on the SAME monitor for ~" + bBlockedMillis[0] + " ms");
        System.out.println("verdict: sleep() does NOT release the monitor — prober waited out the whole nap.");

        banner("3b. wait() RELEASES the monitor — a second thread CAN enter");
        Object waitLock = new Object();
        CountDownLatch aWaiting = new CountDownLatch(1);
        CountDownLatch bEntered = new CountDownLatch(1);
        Thread waiter = new Thread(() -> {
            synchronized (waitLock) {
                aWaiting.countDown();             // A holds the monitor...
                try {
                    waitLock.wait(5_000);         // ...then wait() RELEASES it and parks
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // A was notified and re-acquired the monitor to leave wait()
            }
        }, "waiter");
        Thread enterer = new Thread(() -> {
            try {
                aWaiting.await();                 // wait until A is inside (about to call wait)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            synchronized (waitLock) {             // enters ONLY because wait() released the monitor
                bEntered.countDown();             // proof B got in while A was parked
                waitLock.notify();                // wake A
            }
        }, "enterer");
        waiter.start();
        enterer.start();
        boolean bGotIn = bEntered.await(5, TimeUnit.SECONDS);   // succeeds precisely because wait released
        System.out.println("step 1: waiter held the monitor, then called wait()");
        System.out.println("step 2: enterer acquired the SAME monitor while waiter was parked? " + bGotIn);
        System.out.println("verdict: wait() released the monitor so enterer could proceed — the opposite of sleep().");
        waiter.join();
        enterer.join();

        banner("4. In one sentence");
        System.out.println("A monitor is reentrant (its owner may re-enter), it guards wait/notify (ownership");
        System.out.println("required), and wait() trades the lock for a wait-set slot so others can proceed.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
