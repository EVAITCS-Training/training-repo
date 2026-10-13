package com.evaitcs.collections.threadsafety;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demo 1 — FAILURE MODE #1: CORRUPTED STATE. Two runs of the SAME race, one verdict each.
 *
 * java.util collections are NOT synchronized — by design, not by oversight. The race below
 * shows what that design decision costs when you ignore it: 4 threads fire 25,000 adds each
 * at one plain ArrayList, and afterwards the list is short. ArrayList.add is really
 *
 *     ensureCapacity(size + 1);      // capacity check
 *     elementData[size++] = e;       // read size, bump size, store element — THREE steps
 *
 * and two threads can interleave inside those three steps: both read size=5, both write
 * slot 5 (one add silently evaporates), both store size=6. The README's first diagram is
 * this exact interleaving. Depending on where the threads land, the aftermath is lost
 * updates, null slots, or an ArrayIndexOutOfBoundsException thrown from INSIDE add() when
 * two threads pass the capacity check together.
 *
 * DETERMINISM — how this demo keeps its verdict stable run after run:
 *   - a CountDownLatch "start gun" releases all threads at the same instant, maximizing
 *     overlap (the race is a WINDOW; the gun makes the window as wide as possible);
 *   - every thread is join()ed before anything is counted — never measure a moving target;
 *   - the PLAIN run asserts only the SAFE direction (actual <= expected): how much is lost
 *     depends on the machine, so the number is PRINTED as evidence, never promised;
 *   - the WRAPPED run's exactness IS promised: Collections.synchronizedList turns add()
 *     into a lock-protected call, so 100,000 is the outcome on every machine, every run.
 *
 * Element type is String, not Transaction: the race is in the LIST machinery, not in the
 * elements — 100,000 objects would only slow the show without teaching anything new.
 */
public class RaceCorruptionDemo {

    private static final int THREADS = 4;
    private static final int ADDS_PER_THREAD = 25_000;   // underscores: Java 7+, fine on release 8

    /** The aftermath of a race: what we expected, what survived, and the two corruption marks. */
    static class RaceReport {
        final int expected;
        final int actualSize;
        final int nullSlots;
        final int failedAdds;

        RaceReport(int expected, int actualSize, int nullSlots, int failedAdds) {
            this.expected = expected;
            this.actualSize = actualSize;
            this.nullSlots = nullSlots;
            this.failedAdds = failedAdds;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        banner("1. The design decision — why java.util is NOT synchronized");

        System.out.println("ArrayList.add takes no lock. HashMap.put takes no lock. On purpose:");
        System.out.println("  1. SPEED — a lock on every add() would tax the overwhelming majority of");
        System.out.println("     collections, which are never shared between threads at all;");
        System.out.println("  2. THE CLIENT KNOWS THE LOCK — collections rarely live alone: a bank");
        System.out.println("     transfer touches TWO accounts and needs ONE lock around the whole");
        System.out.println("     sequence. Only the caller knows that scope, so the framework leaves");
        System.out.println("     the choice to you (the javadocs say exactly this: 'If multiple threads");
        System.out.println("     access a collection concurrently, and at least one thread modifies it,");
        System.out.println("     they MUST synchronize externally');");
        System.out.println("  3. THE OPT-IN EXISTS — Collections.synchronizedList/Map/... wrap any");
        System.out.println("     collection in a global mutex (banner 3), and module 10 rebuilds the");
        System.out.println("     engines with concurrency inside. This demo is the 'why' behind both.");

        banner("2. The race — 4 threads x 25,000 adds into a PLAIN ArrayList (start gun + join)");

        RaceReport plain = runRace(new ArrayList<String>());
        System.out.println("expected : " + plain.expected);
        System.out.println("actual   : " + plain.actualSize);
        System.out.println("lost     : " + (plain.expected - plain.actualSize)
                + " updates evaporated inside add() — both threads wrote the SAME slot");
        System.out.println("nulls    : " + plain.nullSlots + " slots counted up by size++ but never written");
        System.out.println("throws   : " + plain.failedAdds
                + " adds died with an exception INSIDE add() (capacity-check window)");
        System.out.println("verdict  : actual <= expected -> " + (plain.actualSize <= plain.expected)
                + " — the SAFE direction holds every run; the loss itself is machine-dependent.");
        if (plain.actualSize < plain.expected) {
            System.out.println("           This run LOST " + (plain.expected - plain.actualSize)
                    + " of " + plain.expected + " adds. No exception, no warning — silent corruption.");
        } else {
            System.out.println("           No loss this run: the race is a WINDOW, not a guarantee — which is");
            System.out.println("           WORSE than always breaking. The wrapped run below has no window.");
        }

        banner("3. The SAME race through Collections.synchronizedList — exact, every run");

        // The wrapper: every inherited method body becomes synchronized(mutex) { delegate }.
        // One thread inside add() at a time -> the three steps of add() can no longer
        // interleave -> nothing to lose. This equality is a PROMISE, not a measurement.
        List<String> wrapped = Collections.synchronizedList(new ArrayList<String>());
        RaceReport fixed = runRace(wrapped);
        System.out.println("expected : " + fixed.expected);
        System.out.println("actual   : " + fixed.actualSize);
        System.out.println("verdict  : exact every run -> " + (fixed.actualSize == fixed.expected));
        System.out.println("The price, printed for later: that mutex is GLOBAL — every reader and every");
        System.out.println("writer of the list waits in one line. Module 10 replaces the single lock with");
        System.out.println("engines that lock a bucket (ConcurrentHashMap) or nobody at all (copy-on-write).");
    }

    /**
     * Runs the start-gun race against ANY List<String> — that is the whole experiment:
     * identical threads, identical adds, only the list's locking policy differs.
     */
    static RaceReport runRace(final List<String> list) throws InterruptedException {
        final CountDownLatch startGun = new CountDownLatch(1);
        final AtomicInteger failedAdds = new AtomicInteger();   // shared counter, itself atomic (Java 5+)
        Thread[] workers = new Thread[THREADS];

        for (int t = 0; t < THREADS; t++) {
            final int threadNo = t;
            // Classic idiom on purpose: anonymous Runnable, no lambdas (java8-features 01
            // turns this into () -> {...} — same object, less ceremony).
            workers[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        startGun.await();          // everyone waits at the line...
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < ADDS_PER_THREAD; i++) {
                        try {
                            list.add("T-" + threadNo + "-" + i);
                        } catch (RuntimeException e) {
                            // On the PLAIN list the race can throw from INSIDE add():
                            // size++ happens before the slot is written, so two threads
                            // slipping through the capacity check together can push size
                            // past the array end -> ArrayIndexOutOfBoundsException.
                            // Counted, not fatal: the report stays comparable.
                            failedAdds.incrementAndGet();
                        }
                    }
                }
            }, "adder-" + t);
            workers[t].start();
        }

        startGun.countDown();                      // ...and the gun sends 4 threads at ONE list.
        for (int t = 0; t < THREADS; t++) {
            workers[t].join();                     // DETERMINISM: count only when every thread is DONE.
        }

        // Scan for null slots — size++ promised an element that the overwritten store never
        // delivered. On a badly corrupted plain list even THIS read-back can throw (size may
        // disagree with the array length); that is itself evidence, so it is caught and shown.
        int nulls = 0;
        try {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) == null) {
                    nulls++;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("           (even reading the list back threw " + e.getClass().getSimpleName()
                    + " — its own size field disagrees with its array. That IS the corruption.)");
        }
        return new RaceReport(THREADS * ADDS_PER_THREAD, list.size(), nulls, failedAdds.get());
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
