package com.evaitcs.collections.threadsafety;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Demo 3 — FIX LEVELS (b) AND (c): manual locks, the synchronized wrappers, and the TWO
 * traps the wrappers leave open.
 *
 * Collections.synchronizedMap(new HashMap(...)) wraps EVERY single method in the wrapper's
 * own mutex: put, get, remove, containsKey — one call, one lock, released on return. That
 * genuinely fixes failure mode #1 (banner 1: a start-gun race of 20,000 concurrent puts
 * lands EXACTLY, joined — the same promise synchronizedList made in demo 1).
 *
 * Trap #1 (banner 2): an ITERATION is not a single method call. for-each over keySet() is
 * a SEQUENCE — hasNext(), next(), hasNext(), next() — and the wrapper releases its lock
 * between the calls. The iterator handed out is the raw HashMap iterator, fail-fast and
 * mutex-blind, so a put THROUGH THE WRAPPER mid-walk still detonates a CME. The wrapper's
 * own javadoc: "It is imperative that the user manually synchronize on the returned map
 * when iterating over any of its collection views." Banners 3-4 do exactly that, live,
 * with a second thread hammering puts that block on the monitor until the walk is over.
 *
 * Trap #2: even a perfectly locked iteration protects only the ITERATION. Check-then-act
 * across two calls stays broken under every wrapper ever shipped — that is CheckThenActDemo.
 *
 * Why HashMap under the hood matters: module 05 showed the bucket machinery a concurrent
 * resize corrupts — banner 1's exactness is the wrapper buying that machinery a lock.
 */
public class WrapperIsNotEnoughDemo {

    private static final int THREADS = 4;
    private static final int PUTS_PER_THREAD = 5_000;

    public static void main(String[] args) throws InterruptedException {
        banner("1. What the wrapper DOES fix — 20,000 concurrent puts, exact, every run");

        final Map<String, String> audit = Collections.synchronizedMap(new HashMap<String, String>());
        final CountDownLatch startGun = new CountDownLatch(1);
        Thread[] workers = new Thread[THREADS];
        for (int t = 0; t < THREADS; t++) {
            final int threadNo = t;
            workers[t] = new Thread(new Runnable() {          // classic idiom: anonymous Runnable
                @Override
                public void run() {
                    try {
                        startGun.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < PUTS_PER_THREAD; i++) {
                        audit.put("T-" + threadNo + "-" + i, "audit entry");
                    }
                }
            }, "putter-" + t);
            workers[t].start();
        }
        startGun.countDown();
        for (int t = 0; t < THREADS; t++) {
            workers[t].join();                                 // count only after join — determinism
        }
        System.out.println("expected : " + (THREADS * PUTS_PER_THREAD));
        System.out.println("actual   : " + audit.size() + "  -> exact every run: "
                + (audit.size() == THREADS * PUTS_PER_THREAD));
        System.out.println("Each put() ran under the wrapper's mutex — the HashMap bucket surgery");
        System.out.println("from module 05 can no longer interleave. (A PLAIN HashMap loses entries");
        System.out.println("under this race.) But note what the lock covers: ONE CALL. Nothing more.");

        banner("2. Trap #1 — iteration THROUGH the wrapper still throws CME (single-threaded!)");

        final Map<String, Account> accounts = Collections.synchronizedMap(new HashMap<String, Account>());
        for (Account a : Account.sampleAccounts()) {
            accounts.put(a.getId(), a);
        }
        boolean injected = false;
        try {
            for (String id : accounts.keySet()) {              // iterator = raw HashMap iterator
                System.out.println("  visiting " + id);
                if (!injected) {
                    injected = true;
                    accounts.put("ACC-9999", new Account("ACC-9999", "Zoe", new BigDecimal("1.00")));
                    System.out.println("  (mid-iteration put THROUGH THE WRAPPER — the call itself succeeded!)");
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " — the NEXT next() threw,");
            System.out.println("          even though every single call went through the synchronized wrapper.");
        }
        System.out.println("WHY: the wrapper locks each CALL; the for-each is a SEQUENCE of calls with the");
        System.out.println("lock RELEASED in between. The put bumped the HashMap's modCount; the keySet");
        System.out.println("iterator's snapshot went stale; the machinery of demo 2 fired as designed.");
        System.out.println("The wrapper's javadoc, verbatim: 'It is imperative that the user manually");
        System.out.println("synchronize on the returned map when iterating over any of its collection views.'");

        banner("3. The fix the javadoc demands — iterate INSIDE synchronized(wrapper)");

        // synchronized(accounts) grabs the WRAPPER ITSELF — which is exactly the mutex the
        // wrapper synchronizes its own methods on. Any writer that respects the wrapper is
        // therefore locked out for the WHOLE walk, not just per call.
        synchronized (accounts) {
            System.out.println("  locked walk over " + accounts.size() + " keys:");
            for (String id : accounts.keySet()) {
                System.out.println("    visiting " + id + " — no writer can interleave, we hold ITS mutex");
            }
        }
        System.out.println("No CME — but this walk was alone in the JVM. Banner 4 puts it under fire.");

        banner("4. ...under fire — a second thread puts while the locked walk runs");

        final Map<String, String> live = Collections.synchronizedMap(new HashMap<String, String>());
        live.put("T-1001", "seeded");
        live.put("T-1002", "seeded");
        live.put("T-1003", "seeded");

        final CountDownLatch mainIsInside = new CountDownLatch(1);
        Thread writer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mainIsInside.await();                      // wait until main holds the lock...
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < 3; i++) {
                    live.put("T-LATE-" + i, "late entry");     // ...then BLOCK on the wrapper's mutex
                }                                              // until main's walk lets go
            }
        }, "late-writer");
        writer.start();

        final List<String> keysSeen = new ArrayList<String>(); // written only by main
        synchronized (live) {
            mainIsInside.countDown();                          // fire the writer's start gun...
            for (String key : live.keySet()) {                 // ...but WE HOLD THE LOCK: not one put
                keysSeen.add(key);                             // can land until this block ends
            }
        }
        writer.join();                                         // DETERMINISM: judge only after join

        System.out.println("keys seen during the locked walk : " + keysSeen.size()
                + " — exactly the 3 seeded, every run (the writer was parked on the monitor)");
        System.out.println("map size after the writer finished: " + live.size()
                + " — its 3 late puts landed the instant we let go");
        System.out.println("Same wrapper, same put() calls — the ONLY difference is who holds the mutex");
        System.out.println("during the walk. The module in one line: the wrapper protects CALLS;");
        System.out.println("YOU must protect SEQUENCES. And sequences of TWO DIFFERENT calls —");
        System.out.println("check-then-act — are the next demo.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
