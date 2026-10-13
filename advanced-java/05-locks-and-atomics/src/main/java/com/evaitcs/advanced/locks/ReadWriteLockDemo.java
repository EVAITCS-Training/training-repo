package com.evaitcs.advanced.locks;

import java.math.BigDecimal;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * Demo 4 — ReadWriteLock (many readers, one writer) and StampedLock (optimistic reads).
 * (README section "ReadWriteLock and StampedLock".)
 *
 * A plain monitor is exclusive: while one thread reads, everyone else waits — even other
 * READERS who cannot possibly conflict. ReadWriteLock splits the lock in two:
 *   readLock()  — many readers may hold it AT THE SAME TIME
 *   writeLock() — exactly one writer, and no readers while it is held
 * The proof of "readers run concurrently" is a CyclicBarrier(3): it only trips if all
 * three reader threads reach it TOGETHER, which is impossible if the read lock were
 * exclusive (reader 2 would wait for reader 1, and the barrier would time out).
 *
 * StampedLock goes further with OPTIMISTIC reads: read without locking, then validate a
 * stamp to check no write landed in between. Caveats: NOT reentrant, NO Conditions.
 */
public class ReadWriteLockDemo {

    public static void main(String[] args) throws Exception {
        banner("1. Three readers hold the read lock SIMULTANEOUSLY — proven by a barrier");
        Account ledger = new Account("ACC-1027", "Dana", new BigDecimal("2450.00"));
        ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

        // The barrier's action runs only when all 3 parties arrive — i.e. all 3 readers are
        // INSIDE the read lock at once. An exclusive lock would make this impossible.
        CyclicBarrier threeTogether = new CyclicBarrier(3,
                () -> System.out.println("  barrier action: 3 readers are inside the read lock AT THE SAME TIME"));

        CountDownLatch readersDone = new CountDownLatch(3);
        AtomicBoolean barrierTripped = new AtomicBoolean(false);
        AtomicBoolean barrierFailed = new AtomicBoolean(false);

        for (int i = 1; i <= 3; i++) {
            final int n = i;
            Thread reader = new Thread(() -> {
                rw.readLock().lock();
                try {
                    threeTogether.await(5, TimeUnit.SECONDS);   // needs the other two INSIDE, right now
                    barrierTripped.set(true);
                    System.out.println("  reader " + n + " read balance = " + ledger.getBalance().toPlainString());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    barrierFailed.set(true);
                } catch (BrokenBarrierException | TimeoutException e) {
                    barrierFailed.set(true);   // would happen if readers were exclusive
                } finally {
                    rw.readLock().unlock();
                    readersDone.countDown();
                }
            }, "reader-" + n);
            reader.start();
        }
        boolean allDone = readersDone.await(10, TimeUnit.SECONDS);
        System.out.println("step 1: three readers each took readLock() and met at a CyclicBarrier(3)");
        System.out.println("step 2: all readers finished = " + allDone + "; barrier tripped = " + barrierTripped.get()
                + "; barrier failed = " + barrierFailed.get());
        System.out.println("verdict: the barrier can ONLY trip if three read locks were held at once — read");
        System.out.println("         locks are SHARED. (Under an exclusive lock this would time out and fail.)");

        banner("2. One writer stands ALONE — readers cannot enter while it writes");
        CountDownLatch writerHolds = new CountDownLatch(1);
        CountDownLatch releaseWriter = new CountDownLatch(1);
        AtomicBoolean readerEnteredDuringWrite = new AtomicBoolean(false);

        Thread writer = new Thread(() -> {
            rw.writeLock().lock();
            try {
                writerHolds.countDown();
                releaseWriter.await();          // hold the EXCLUSIVE write lock until main says stop
                ledger.deposit(new BigDecimal("250.00"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                rw.writeLock().unlock();
            }
        }, "writer");
        writer.setDaemon(true);
        writer.start();
        writerHolds.await();

        Thread reader = new Thread(() -> {
            rw.readLock().lock();               // blocks: the writer holds the exclusive lock
            try {
                readerEnteredDuringWrite.set(true);
            } finally {
                rw.readLock().unlock();
            }
        }, "blocked-reader");
        reader.setDaemon(true);
        reader.start();
        Thread.sleep(200);                      // a window to observe; the reader CANNOT enter (guaranteed)
        System.out.println("step 1: writer holds writeLock(); a reader tried readLock()");
        System.out.println("step 2: reader entered while the writer held the lock? "
                + readerEnteredDuringWrite.get() + "  (expected false — write lock is exclusive)");
        releaseWriter.countDown();
        writer.join();
        reader.join();
        System.out.println("step 3: after the writer released, the reader entered? "
                + readerEnteredDuringWrite.get() + "  (expected true)");
        System.out.println("verdict: N readers OR 1 writer — the lock is a mode switch, not a single door.");

        banner("3. StampedLock — the optimistic read: look, then CHECK no write interrupted you");
        // Guarded state: a balance in whole cents (a plain long — StampedLock guards raw fields).
        long[] balanceCents = { 245_000 };                 // $2,450.00
        StampedLock stamped = new StampedLock();

        long stamp = stamped.tryOptimisticRead();          // takes NO lock — just a version token
        long observed = balanceCents[0];                    // read the field...
        boolean valid = stamped.validate(stamp);            // ...then ask: did any WRITE land since?
        System.out.println("step 1: tryOptimisticRead() gave stamp " + stamp + " (nothing was locked)");
        System.out.println("step 2: read balanceCents = " + observed + "; validate(stamp) = " + valid
                + " (true: no write in between, so the read is trustworthy)");

        // Now simulate a writer landing between an optimistic read and its validate.
        long staleStamp = stamped.tryOptimisticRead();
        long staleRead = balanceCents[0];
        long writeStamp = stamped.writeLock();              // a real write...
        balanceCents[0] = 270_000;                          // ...$2,700.00 (the deposit from section 2)
        stamped.unlockWrite(writeStamp);
        boolean staleValid = stamped.validate(staleStamp);  // now FALSE — the write moved the version
        System.out.println("step 3: read " + staleRead + " under stamp " + staleStamp + ", then a write landed");
        System.out.println("step 4: validate(staleStamp) = " + staleValid
                + " -> the optimistic read is STALE; fall back to a real read lock:");
        long readStamp = stamped.readLock();
        long fresh;
        try {
            fresh = balanceCents[0];
        } finally {
            stamped.unlockRead(readStamp);
        }
        System.out.println("        re-read under readLock() = " + fresh + " cents = $"
                + BigDecimal.valueOf(fresh, 2).toPlainString());
        System.out.println("CAVEATS: StampedLock is NOT reentrant (re-locking deadlocks yourself) and has NO");
        System.out.println("         Conditions. Optimistic reads suit many-readers/rare-writers; validate or re-read.");

        banner("4. In one sentence");
        System.out.println("ReadWriteLock lets readers share and writers own; StampedLock lets readers not even");
        System.out.println("lock — they just validate a stamp and re-read if a writer beat them to it.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
