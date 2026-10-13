package com.evaitcs.advanced.locks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demo 3 — TWO CONDITIONS ON ONE LOCK: the canonical bounded buffer.
 * (README section "Conditions: a lock can have several waiting queues".)
 *
 * A monitor gives you ONE wait set: every waiting thread wakes on notifyAll and re-checks,
 * which is wasteful when there are two DIFFERENT reasons to wait. A ReentrantLock can
 * create MULTIPLE Condition queues from the same lock:
 *   notFull  — producers park here when the buffer is full
 *   notEmpty — consumers park here when the buffer is empty
 * A producer that adds an item signals notEmpty (wakes a consumer, not other producers);
 * a consumer that removes an item signals notFull (wakes a producer). Targeted wakeups.
 *
 * The buffer is a public static class so LocksAtomicsTest drives the SAME code and
 * asserts FIFO order (house pattern: tests reuse demo classes).
 */
public class BoundedBufferDemo {

    /**
     * A fixed-capacity FIFO guarded by one ReentrantLock with two Condition queues.
     * put() blocks (on notFull) when full; take() blocks (on notEmpty) when empty.
     */
    public static final class BoundedBuffer<T> {
        private final ArrayDeque<T> slots = new ArrayDeque<>();
        private final int capacity;

        // The standard idiom: ONE lock, and as many Condition queues as you need from it.
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();

        private final boolean verbose;
        private final AtomicInteger step = new AtomicInteger();   // numbers the printed dance

        public BoundedBuffer(int capacity) {
            this(capacity, false);
        }

        public BoundedBuffer(int capacity, boolean verbose) {
            this.capacity = capacity;
            this.verbose = verbose;
        }

        public void put(T item) throws InterruptedException {
            lock.lock();
            try {
                // WHILE, never if: a wakeup is not a guarantee. Re-check after every await —
                // another thread may have refilled the buffer before we re-acquired the lock.
                while (slots.size() == capacity) {
                    if (verbose) {
                        System.out.println("  step " + step.incrementAndGet()
                                + ": buffer FULL (" + slots.size() + "/" + capacity + ") -> producer AWAITS notFull");
                    }
                    notFull.await();               // releases the lock AND parks on notFull
                }
                slots.addLast(item);
                if (verbose) {
                    System.out.println("  step " + step.incrementAndGet()
                            + ": producer PUT " + describe(item) + " (" + slots.size() + "/" + capacity
                            + ") -> SIGNALS notEmpty");
                }
                notEmpty.signal();                 // targeted: wake ONE consumer, not the world
            } finally {
                lock.unlock();
            }
        }

        public T take() throws InterruptedException {
            lock.lock();
            try {
                while (slots.isEmpty()) {
                    if (verbose) {
                        System.out.println("  step " + step.incrementAndGet()
                                + ": buffer EMPTY -> consumer AWAITS notEmpty");
                    }
                    notEmpty.await();              // releases the lock AND parks on notEmpty
                }
                T item = slots.pollFirst();
                if (verbose) {
                    System.out.println("  step " + step.incrementAndGet()
                            + ": consumer TOOK " + describe(item) + " (" + slots.size() + "/" + capacity
                            + ") -> SIGNALS notFull");
                }
                notFull.signal();                  // targeted: wake ONE producer
                return item;
            } finally {
                lock.unlock();
            }
        }

        private static String describe(Object item) {
            return item instanceof Transaction t ? t.id() : String.valueOf(item);   // Java 21 pattern matching
        }
    }

    public static void main(String[] args) throws Exception {
        banner("1. The dance, narrated first");
        System.out.println("step 1: producer calls put(); if the buffer is full it AWAITS on notFull, releasing the lock.");
        System.out.println("step 2: consumer calls take(); removes one item; SIGNALS notFull -> a producer wakes.");
        System.out.println("step 3: consumer calls take(); if the buffer is empty it AWAITS on notEmpty, releasing the lock.");
        System.out.println("step 4: producer calls put(); adds one item; SIGNALS notEmpty -> a consumer wakes.");
        System.out.println("step 5: every await sits in a WHILE loop — a wakeup re-checks the condition, it never assumes.");

        banner("2. The dance, live — capacity 4, eight transactions, one producer, one consumer");
        int capacity = 4;
        int count = 8;
        BoundedBuffer<Transaction> buffer = new BoundedBuffer<>(capacity, true);

        List<Transaction> produced = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            produced.add(Transaction.of("T-" + i, "transfer " + i, (i * 10) + ".00"));
        }
        List<Transaction> consumed = new ArrayList<>();   // only the consumer writes it; join() publishes it (module 04)

        Thread producer = new Thread(() -> {
            try {
                for (Transaction t : produced) {
                    buffer.put(t);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();       // never swallow (module 04)
            }
        }, "producer");
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    consumed.add(buffer.take());
                    Thread.sleep(30);        // slow consumer ON PURPOSE: forces the buffer to FILL and await
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        banner("3. The verdict — every item crossed, in order");
        List<String> producedIds = produced.stream().map(Transaction::id).toList();
        List<String> consumedIds = consumed.stream().map(Transaction::id).toList();
        System.out.println("produced order = " + producedIds);
        System.out.println("consumed order = " + consumedIds);
        System.out.println("FIFO preserved = " + producedIds.equals(consumedIds)
                + " (one producer + one consumer + a FIFO deque = exact order guaranteed)");
        System.out.println("verdict: capacity " + capacity + " forced the producer to await notFull at least once;");
        System.out.println("         the two Condition queues kept each side from waking the wrong waiters.");

        banner("4. In one sentence");
        System.out.println("One lock, two Condition queues: notFull and notEmpty let a bounded buffer wake");
        System.out.println("EXACTLY the right kind of waiter — the thing a single monitor wait set cannot do.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
