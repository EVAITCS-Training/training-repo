package com.evaitcs.collections.concurrent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Demo 1 — CopyOnWriteArrayList: the FAIL-SAFE iterator (module 09's CME, extinct).
 *
 * The deal, printed below: every WRITE (add/set/remove) takes the list's one lock, copies
 * the ENTIRE backing array, and swaps the reference; every READ (get/iterator) takes NO
 * lock at all and walks whatever array the reference pointed to when the read started. An
 * iterator therefore walks a FROZEN SNAPSHOT:
 *
 *   - it can NEVER throw ConcurrentModificationException — the array under it cannot
 *     change, so there is nothing to fail fast about (the module 03 explosion and the
 *     module 09 machinery simply do not exist here);
 *   - it can never SEE elements added after the iterator was created — you are iterating
 *     over history, and that is the deal you accepted for lock-free reads;
 *   - iterator.remove() throws UnsupportedOperationException — removing "from the
 *     snapshot" would be a lie, so the COW iterator refuses to pretend.
 *
 * Right tool: listener/observer/alert lists — written rarely (registration), read
 * constantly (every event walks the list). Wrong tool: anything write-heavy — EVERY add
 * pays a full array copy.
 *
 * DETERMINISM: banners 1–2 are single-threaded (the snapshot is structural, not lucky);
 * banner 3 choreographs reader and writer with TWO latches so both counts are exact every
 * run — the reader's snapshot is provably frozen before the writer's first add, while the
 * reader's WALK still genuinely overlaps the writer's 50 array copies.
 */
public class CopyOnWriteDemo {

    public static void main(String[] args) throws InterruptedException {
        banner("1. The snapshot proof — single-threaded, so you can SEE the freeze");

        CopyOnWriteArrayList<Transaction> alerts =
                new CopyOnWriteArrayList<Transaction>(Transaction.sampleTransactions());   // 3 seeded
        Iterator<Transaction> it = alerts.iterator();   // <- FREEZE: grabs TODAY's array, no lock
        System.out.println("iterator created over " + alerts.size() + " alerts");

        alerts.add(new Transaction("T-1004", "New-account alert", new BigDecimal("10.00"), 1700010800000L));
        System.out.println("added a 4th alert WHILE the iterator was open — on an ArrayList this is");
        System.out.println("module 03's explosion / module 09's CME. Here the write COPIED the array");
        System.out.println("and swapped the reference; the iterator is still walking the OLD one.");

        List<String> seen = new ArrayList<String>();
        while (it.hasNext()) {
            seen.add(it.next().getId());
        }
        System.out.println("iterator saw : " + seen + "   <- THREE: T-1004 is invisible to it, and NO exception");
        System.out.println("the list     : " + alerts.size() + " alerts — the add DID happen, for every FUTURE iterator");

        banner("2. The price of the snapshot — iterator.remove() is unsupported");

        Iterator<Transaction> it2 = alerts.iterator();
        it2.next();
        try {
            it2.remove();
        } catch (UnsupportedOperationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName()
                    + " — the iterator walks a copy that ISN'T the list;");
            System.out.println("           'removing' from it would change nothing real, so it refuses to lie.");
        }
        System.out.println("To remove, call alerts.remove(...) on the LIST itself — a write, so it copies");
        System.out.println("like every other write. (java8-features 01: alerts.removeIf(...) is the lambda-");
        System.out.println("era one-liner over that same copying machinery.)");

        banner("3. Under fire — a reader and a writer, truly overlapping, joined, EXACT counts");

        final CopyOnWriteArrayList<Transaction> live =
                new CopyOnWriteArrayList<Transaction>(Transaction.sampleTransactions());   // 3 seeded
        final CountDownLatch startGun = new CountDownLatch(1);
        final CountDownLatch snapshotTaken = new CountDownLatch(1);
        final List<String> readerSaw = new ArrayList<String>();   // written ONLY by the reader; main reads after join

        Thread reader = new Thread(new Runnable() {               // classic idiom: anonymous Runnable
            @Override
            public void run() {
                try {
                    startGun.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                Iterator<Transaction> snapshot = live.iterator(); // freeze: the array of 3, taken under no lock
                snapshotTaken.countDown();                        // tell the writer: the snapshot is cast
                while (snapshot.hasNext()) {                      // ...and keep walking it while the writer
                    readerSaw.add(snapshot.next().getId());       // copies the array 50 times around us
                }
            }
        }, "alert-reader");

        Thread writer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startGun.await();
                    snapshotTaken.await();                        // never write before the snapshot exists
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < 50; i++) {                    // 50 writes = 50 full array copies
                    live.add(new Transaction("T-9" + String.format("%03d", i), "Live alert " + i,
                            new BigDecimal("10.00"), System.currentTimeMillis()));
                }
            }
        }, "alert-writer");

        reader.start();
        writer.start();
        startGun.countDown();
        reader.join();                                            // DETERMINISM: judge only after join
        writer.join();

        System.out.println("reader saw            : " + readerSaw.size() + " — EXACTLY the 3 seeded, every run:");
        System.out.println("                        its snapshot froze before the writer's first add (latch-choreographed)");
        System.out.println("list after the race   : " + live.size() + " — EXACTLY 3 + 50: every write took the COW lock, nothing lost");
        System.out.println("and the two threads genuinely OVERLAPPED: while the writer copied the array 50");
        System.out.println("times, the reader walked the original. No lock on the read side, no CME, no loss.");

        banner("4. What it costs — the write-path arithmetic");

        System.out.println("n adds into a list of size s copy s+1, s+2, ... elements: O(n*s) total work.");
        System.out.println("For a 10-element listener list: irrelevant. For a 100,000-row ledger: ruinous.");
        System.out.println("Reads, meanwhile, never lock, never wait, never see a torn state — that trade is");
        System.out.println("EXACTLY right for observer lists: written at registration, read on every event.");
        System.out.println("CopyOnWriteArraySet (demo 6) is this same engine behind the Set interface —");
        System.out.println("addIfAbsent + equals dedupe (core-java 08's contract doing the work).");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
