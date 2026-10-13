package com.evaitcs.advanced.locks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for module 05's claims. Run with:
 *   .\mvnw.cmd -pl 05-locks-and-atomics test
 * Each test proves ONE claim and prints its evidence, so the run doubles as a demo.
 *
 * DETERMINISM POLICY (the whole game in a locks module): every race gets a start-gun
 * latch and is joined before asserting; exact counts are asserted ONLY where CAS/atomics
 * guarantee them. The PLAIN int race asserts only the safe direction (<= expected) and
 * PRINTS the observed loss, because the scheduler decides the exact number. The deadlock
 * test uses DAEMON threads (a deadlock is forever) and breaks it with interrupt() at the
 * end, because ReentrantLock.lockInterruptibly is interruptible (synchronized is not).
 * No timing is asserted except generous blocking floors, clearly commented.
 */
class LocksAtomicsTest {

    private static final int TELLERS = 4;
    private static final int DEPOSITS_EACH = 25_000;
    private static final int EXPECTED = TELLERS * DEPOSITS_EACH;   // 100 000

    @Test
    @DisplayName("tryLock: fails instantly while another thread holds the lock, times out politely, and succeeds once it is released")
    void tryLockReflectsOwnership() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                held.countDown();
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }, "holder");
        holder.setDaemon(true);
        holder.start();
        assertTrue(held.await(5, TimeUnit.SECONDS), "the holder acquired the lock");

        assertFalse(lock.tryLock(), "non-blocking tryLock() must fail while the holder owns the lock");

        long start = System.nanoTime();
        assertFalse(lock.tryLock(200, TimeUnit.MILLISECONDS), "timed tryLock() must give up while the lock is held");
        long waitedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        // Generous blocking floor only (house rule): a 200 ms tryLock waits ~200 ms, never less in practice.
        assertTrue(waitedMillis >= 150, "the timed ask actually waited (observed " + waitedMillis + " ms)");

        release.countDown();
        holder.join(5_000);
        assertFalse(holder.isAlive(), "the holder released and exited");
        assertTrue(lock.tryLock(), "after release, tryLock() must succeed");
        lock.unlock();

        System.out.println("tryLock while held = false; timed tryLock waited " + waitedMillis
                + " ms then false; after release = true");
    }

    @Test
    @DisplayName("bounded buffer: one producer's eight transactions reach one consumer in exact FIFO order, capacity forcing awaits")
    void boundedBufferTransfersAllInFifoOrder() throws Exception {
        int capacity = 4;
        int count = 8;
        BoundedBufferDemo.BoundedBuffer<Transaction> buffer = new BoundedBufferDemo.BoundedBuffer<>(capacity);

        List<Transaction> batch = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            batch.add(Transaction.of("T-" + i, "transfer " + i, (i * 10) + ".00"));
        }
        List<Transaction> received = new ArrayList<>();   // only the consumer writes it; join publishes it

        Thread producer = new Thread(() -> {
            try {
                for (Transaction t : batch) {
                    buffer.put(t);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer");
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    received.add(buffer.take());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join(10_000);
        consumer.join(10_000);
        assertFalse(producer.isAlive(), "producer finished");
        assertFalse(consumer.isAlive(), "consumer finished");

        assertEquals(batch, received, "every transaction crossed, and record equality checks all fields");
        List<String> receivedIds = received.stream().map(Transaction::id).toList();
        assertEquals(List.of("T-1", "T-2", "T-3", "T-4", "T-5", "T-6", "T-7", "T-8"), receivedIds);
        System.out.println("capacity " + capacity + " < count " + count + " forced notFull awaits; FIFO order = " + receivedIds);
    }

    @Test
    @DisplayName("ReadWriteLock: three readers hold the read lock SIMULTANEOUSLY, proven by a CyclicBarrier(3) that only trips together")
    void threeReadersHoldReadLockSimultaneously() throws Exception {
        ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
        CyclicBarrier threeTogether = new CyclicBarrier(3);   // trips ONLY if all three arrive at once
        CountDownLatch done = new CountDownLatch(3);
        AtomicBoolean barrierFailed = new AtomicBoolean(false);

        for (int i = 1; i <= 3; i++) {
            Thread reader = new Thread(() -> {
                rw.readLock().lock();
                try {
                    threeTogether.await(5, TimeUnit.SECONDS);   // an exclusive read lock would time out here
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    barrierFailed.set(true);
                } catch (BrokenBarrierException | TimeoutException e) {
                    barrierFailed.set(true);   // THE PROOF OF FAILURE: readers did not coexist
                } finally {
                    rw.readLock().unlock();
                    done.countDown();
                }
            }, "reader-" + i);
            reader.start();
        }

        assertTrue(done.await(10, TimeUnit.SECONDS), "all three readers finished");
        // If the read lock were exclusive, reader 2 would wait for reader 1, the barrier would
        // time out, and barrierFailed would be true. It is false => three read locks coexisted.
        assertFalse(barrierFailed.get(), "the barrier tripped => all three read locks were held at the same time");
        System.out.println("CyclicBarrier(3) tripped under the read lock: three readers ran concurrently");
    }

    @Test
    @DisplayName("atomics: 4 threads x 25 000 AtomicInteger.incrementAndGet() total EXACTLY 100 000 — CAS loses nothing")
    void atomicIntegerCountsExactly() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        List<Thread> tellers = startRace(counter::incrementAndGet);
        for (Thread t : tellers) {
            t.join(10_000);
            assertFalse(t.isAlive(), "every teller finished");
        }
        assertEquals(EXPECTED, counter.get(), "AtomicInteger is exact under any contention");
        System.out.println("4 tellers x 25 000 atomic increments -> " + counter.get() + " (exact, every run)");
    }

    @Test
    @DisplayName("the lost update: 4 threads x 25 000 plain int++ can only come out AT MOST 100 000 — the exact loss is printed, not asserted")
    void plainIntRaceOnlyLosesUpdates() throws Exception {
        int[] plain = { 0 };                       // one-slot box so the lambda can mutate it; plain[0]++ is NOT atomic
        List<Thread> tellers = startRace(() -> plain[0]++);
        for (Thread t : tellers) {
            t.join(10_000);
            assertFalse(t.isAlive(), "every teller finished");
        }
        // The safe direction is guaranteed (increments never invent updates); the exact loss is
        // scheduler-dependent, so we assert <= and PRINT what was actually lost (house rule).
        assertTrue(plain[0] <= EXPECTED, "plain increments can lose updates but never create them");
        int lost = EXPECTED - plain[0];
        System.out.println("expected " + EXPECTED + ", actual " + plain[0]
                + (lost > 0 ? " -> " + lost + " updates LOST" : " -> no loss this run (legal, but not guaranteed)"));
    }

    @Test
    @DisplayName("ownership: calling wait() or notify() without holding the monitor throws IllegalMonitorStateException")
    void waitWithoutMonitorThrows() {
        Object guard = new Object();
        IllegalMonitorStateException waitThrow =
                assertThrows(IllegalMonitorStateException.class, () -> guard.wait(),
                        "wait() without the monitor must throw");
        IllegalMonitorStateException notifyThrow =
                assertThrows(IllegalMonitorStateException.class, guard::notify,
                        "notify() without the monitor must throw");
        System.out.println("CAUGHT -> " + waitThrow.getClass().getSimpleName() + " from wait(), and "
                + notifyThrow.getClass().getSimpleName() + " from notify(): you cannot use a wait set you do not own");
    }

    @Test
    @DisplayName("ABA: AtomicStampedReference REFUSES a stale stamp even when the value returned, and ACCEPTS a fresh stamp")
    void stampedReferenceRefusesStaleAndAcceptsFresh() {
        AtomicStampedReference<String> status = new AtomicStampedReference<>("PENDING", 0);

        int[] seenBox = new int[1];
        String seen = status.get(seenBox);          // (PENDING, stamp 0)
        int seenStamp = seenBox[0];

        status.set("REJECTED", 1);                   // A -> B
        status.set("PENDING", 2);                    // B -> A (value back to PENDING, stamp advanced)

        // Value matches (PENDING) but the stamp is stale (0 vs current 2) -> the CAS must FAIL.
        assertFalse(status.compareAndSet(seen, "APPROVED", seenStamp, 3),
                "a stale stamp must be refused even though the value looks unchanged (the ABA scar)");

        // Re-read for a FRESH (value, stamp) pair -> the CAS must SUCCEED.
        int[] freshBox = new int[1];
        String fresh = status.get(freshBox);         // (PENDING, stamp 2)
        assertTrue(status.compareAndSet(fresh, "APPROVED", freshBox[0], freshBox[0] + 1),
                "a fresh (value, stamp) pair must be accepted");
        assertEquals("APPROVED", status.getReference());

        System.out.println("stale stamp " + seenStamp + " refused (value had gone PENDING->REJECTED->PENDING); "
                + "fresh stamp " + freshBox[0] + " accepted -> now " + status.getReference());
    }

    @Test
    @DisplayName("deadlock: findDeadlockedThreads() names BOTH choreographed tellers within a bounded poll, and interrupt() breaks it")
    void deadlockIsDetectedByName() throws Exception {
        ReentrantLock lockA = new ReentrantLock();   // guards account ACC-A
        ReentrantLock lockB = new ReentrantLock();   // guards account ACC-B — a SECOND, distinct lock
        CountDownLatch danaHoldsA = new CountDownLatch(1);
        CountDownLatch boHoldsB = new CountDownLatch(1);

        Thread dana = deadlocker("teller-Dana", lockA, danaHoldsA, boHoldsB, lockB);
        Thread bo = deadlocker("teller-Bo", lockB, boHoldsB, danaHoldsA, lockA);
        dana.setDaemon(true);                          // MANDATORY: a deadlock is forever
        bo.setDaemon(true);
        dana.start();
        bo.start();
        assertTrue(danaHoldsA.await(5, TimeUnit.SECONDS), "Dana took lock A first");
        assertTrue(boHoldsB.await(5, TimeUnit.SECONDS), "Bo took lock B first");

        // Bounded poll with a deadline (house rule for scheduler-dependent waits).
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] ids = null;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            ids = threadBean.findDeadlockedThreads();
            if (ids != null) {
                break;
            }
            Thread.sleep(50);
        }
        assertNotNull(ids, "the choreographed deadlock was detected within the bounded poll");

        ThreadInfo[] infos = threadBean.getThreadInfo(ids);
        Set<String> names = new HashSet<>();
        for (ThreadInfo info : infos) {
            if (info != null) {
                names.add(info.getThreadName());
                System.out.println("deadlocked: " + info.getThreadName() + " blocked on " + info.getLockName()
                        + " held by " + info.getLockOwnerName());
            }
        }
        assertTrue(names.contains("teller-Dana") && names.contains("teller-Bo"),
                "both tellers were named as deadlocked: " + names);

        // Cleanup: interrupt breaks it BECAUSE we used lockInterruptibly (a synchronized monitor cannot be broken this way).
        dana.interrupt();
        bo.interrupt();
        dana.join(5_000);
        bo.join(5_000);
        assertFalse(dana.isAlive(), "Dana exited after the interrupt broke the deadlock");
        assertFalse(bo.isAlive(), "Bo exited after the interrupt broke the deadlock");
        System.out.println("deadlock detected, both tellers named, then broken cleanly via lockInterruptibly");
    }

    // ---- helpers ----

    /** A thread that takes `mine`, signals, waits for the other side, then wants `theirs` -> a cycle. */
    private static Thread deadlocker(String name, ReentrantLock mine, CountDownLatch iHold,
                                     CountDownLatch theyHold, ReentrantLock theirs) {
        Thread t = new Thread(() -> {
            mine.lock();
            try {
                iHold.countDown();
                theyHold.await();
                theirs.lockInterruptibly();   // blocks forever in the deadlock; interruptible so we can break it
                theirs.unlock();
            } catch (InterruptedException e) {
                // the rescue path used by the test cleanup
            } finally {
                mine.unlock();
            }
        }, name);
        return t;
    }

    /** Start TELLERS threads that each increment DEPOSITS_EACH times, released together by a start gun. */
    private static List<Thread> startRace(Runnable deposit) {
        CountDownLatch startGun = new CountDownLatch(1);
        List<Thread> tellers = new ArrayList<>();
        for (int i = 0; i < TELLERS; i++) {
            Thread t = new Thread(() -> {
                try {
                    startGun.await();          // every teller leaves the gate at the same instant
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int j = 0; j < DEPOSITS_EACH; j++) {
                    deposit.run();
                }
            }, "teller-" + i);
            tellers.add(t);
            t.start();
        }
        startGun.countDown();                  // fire the gun -> maximum contention
        return tellers;
    }
}
