package com.evaitcs.collections.threadsafety;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

/**
 * Demo 5 — "But Vector IS synchronized!" Yes. And it is STILL not enough.
 *
 * Vector (and its map cousin Hashtable) predate the Collections Framework: every method is
 * declared `synchronized` — the SAME lock granularity as demo 3's wrappers, baked into the
 * class since JDK 1.0. That earns Vector exactly what the wrappers earn, and no more:
 *
 *   banner 1 — failure mode #1 fixed: concurrent add() calls are serialized, the count is
 *              EXACT (start gun, join, 100,000 every run). Collections.synchronizedList(
 *              new ArrayList<String>()) IS the modern Vector — same mutex, same promises.
 *   banner 2 — failure mode #1's ugly twin survives: get-then-set is a COMPOUND action, and
 *              method-level locks do not span compounds. Played as a scripted interleaving
 *              (demo 4's technique): both tellers read 100.00, both write, one +50.00
 *              deposit evaporates. Deterministic, every run.
 *   banner 3 — failure mode #2 survives too: Vector's iterator snapshots modCount exactly
 *              like ArrayList's, so remove-during-for-each throws CME. Vector's own javadoc
 *              demands the same manual `synchronized(vector)` around iteration as the
 *              wrappers do. Legacy synchronization buys you nothing new.
 *
 * House verdict, printed below: prefer ArrayList + explicit synchronization (YOU choose the
 * lock scope — demo 3 banner 4) or a java.util.concurrent engine (module 10). Vector is
 * legacy — kept alive by old codebases and interview questions, which is reason enough to
 * know precisely how far its locks reach.
 */
public class VectorRevisitedDemo {

    private static final int THREADS = 4;
    private static final int ADDS_PER_THREAD = 25_000;

    public static void main(String[] args) throws InterruptedException {
        banner("1. What method-level synchronization DOES fix — concurrent adds, exact");

        final Vector<String> vector = new Vector<String>();
        final CountDownLatch startGun = new CountDownLatch(1);
        Thread[] workers = new Thread[THREADS];
        for (int t = 0; t < THREADS; t++) {
            final int threadNo = t;
            workers[t] = new Thread(new Runnable() {           // anonymous Runnable: classic idiom
                @Override
                public void run() {
                    try {
                        startGun.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < ADDS_PER_THREAD; i++) {
                        vector.add("T-" + threadNo + "-" + i); // add() is `synchronized` — one at a time
                    }
                }
            }, "vector-adder-" + t);
            workers[t].start();
        }
        startGun.countDown();
        for (int t = 0; t < THREADS; t++) {
            workers[t].join();                                 // determinism: count after join
        }
        System.out.println("expected : " + (THREADS * ADDS_PER_THREAD));
        System.out.println("actual   : " + vector.size() + "  -> exact every run: "
                + (vector.size() == THREADS * ADDS_PER_THREAD));
        System.out.println("Same promise, same mechanism, same GLOBAL mutex as Collections.synchronizedList");
        System.out.println("(demo 1 banner 3). Vector = the 1997 edition of the wrapper. Which is why");
        System.out.println("everything the wrapper could NOT fix, Vector cannot fix either — next banner.");

        banner("2. What it does NOT fix #1 — the compound action (get-then-set), scripted");

        Vector<Account> accounts = new Vector<Account>();
        accounts.add(new Account("ACC-1027", "Dana", new BigDecimal("100.00")));
        System.out.println("balance starts at 100.00. Teller A adds 50.00, teller B adds 30.00.");
        System.out.println("Both follow the read-modify-write pattern: get(0), add deposit, set(0, ...).");

        // The doomed interleaving, played by hand (demo 4's technique): every CALL below
        // takes the Vector lock — and releases it before the teller's NEXT line runs.
        Account aRead = accounts.get(0);                       // A reads: 100.00
        System.out.println("A: get(0) -> balance " + aRead.getBalance() + "   (lock taken and RELEASED around the call)");
        Account bRead = accounts.get(0);                       // B reads BEFORE A writes: 100.00
        System.out.println("B: get(0) -> balance " + bRead.getBalance() + "   (A has not written yet — B's read is doomed to be stale)");
        accounts.set(0, new Account("ACC-1027", "Dana", aRead.getBalance().add(new BigDecimal("50.00"))));
        System.out.println("A: set(0, 150.00)          (computed OUTSIDE any lock, from A's read)");
        accounts.set(0, new Account("ACC-1027", "Dana", bRead.getBalance().add(new BigDecimal("30.00"))));
        System.out.println("B: set(0, 130.00)          (computed from B's STALE read: 100.00 + 30.00)");
        System.out.println("final balance: " + accounts.get(0).getBalance() + " — should be 180.00. A's +50.00 VANISHED.");
        System.out.println("get() atomic. set() atomic. get-compute-set NOT atomic: the Vector lock covers");
        System.out.println("METHODS, and the compound action lives BETWEEN methods. External synchronized");
        System.out.println("blocks around the whole sequence fix it — at demo 3's global-lock price.");

        banner("3. What it does NOT fix #2 — Vector's iterator is fail-fast too");

        List<Transaction> transactions = new Vector<Transaction>(Transaction.sampleTransactions());
        List<String> idsBefore = new ArrayList<String>();
        for (Transaction tx : transactions) {
            idsBefore.add(tx.getId());
        }
        System.out.println("before: " + idsBefore);
        try {
            for (Transaction tx : transactions) {
                if (tx.getDescription().startsWith("Reversed")) {
                    transactions.remove(tx);                   // modCount++ behind the iterator's back
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName()
                    + " — from a Vector! Its iterator snapshots modCount exactly like ArrayList's (demo 2).");
        }
        System.out.println("Vector's own javadoc, same rule as the wrappers: 'when iterating over a vector,");
        System.out.println("the user must manually synchronize on the vector'. Method-level locks do not");
        System.out.println("make iterators immortal — they make the modCount machinery run behind a mutex.");
        System.out.println();
        System.out.println("VERDICT: Vector/Hashtable = wrapper granularity from 1997. Every trap in this");
        System.out.println("module applies to them unchanged. Module 10 stops bolting locks onto single-");
        System.out.println("threaded designs and builds the concurrency INTO the data structure.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
