package com.evaitcs.collections.threadsafety;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 09-thread-safety-and-fail-fast test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * DETERMINISM is the design rule of this module, and the tests obey it strictly:
 *   - races are choreographed with CountDownLatch start guns and join()ed before ANY
 *     assertion — never measure a moving target;
 *   - exact counts are asserted ONLY where a lock guarantees them (synchronizedList,
 *     synchronizedMap, Vector); the PLAIN-list race asserts the safe direction
 *     (size <= expected) and PRINTS the observed loss — how much evaporates is the
 *     machine's business, that something CANNOT be added is the JVM's;
 *   - every CME and every lost update here is produced SINGLE-THREADED, by calling the
 *     steps manually in the doomed order. A test that races threads and hopes for a
 *     bug is a flaky test; a test that SCRIPTS the interleaving is a proof.
 */
class ThreadSafetyTest {

    /**
     * The start-gun race, shared by three tests: `threads` threads each perform
     * `addsPerThread` adds against ONE list, released simultaneously and joined before
     * returning. Returns the number of adds that threw inside add() (possible only on
     * an unsynchronized list — the capacity-check window, demo 1).
     */
    private static int raceAdds(final List<String> target, final int threads, final int addsPerThread)
            throws InterruptedException {
        final CountDownLatch startGun = new CountDownLatch(1);
        final AtomicInteger failures = new AtomicInteger();
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            final int threadNo = t;
            workers[t] = new Thread(new Runnable() {            // anonymous Runnable: classic idiom
                @Override
                public void run() {
                    try {
                        startGun.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < addsPerThread; i++) {
                        try {
                            target.add("T-" + threadNo + "-" + i);
                        } catch (RuntimeException e) {
                            failures.incrementAndGet();         // counted, not fatal — the thread finishes
                        }
                    }
                }
            }, "test-adder-" + t);
            workers[t].start();
        }
        startGun.countDown();
        for (int t = 0; t < threads; t++) {
            workers[t].join();                                  // determinism: return only when all are done
        }
        return failures.get();
    }

    private static boolean containsId(List<Transaction> transactions, String id) {
        return transactions.contains(new Transaction(id, "probe", BigDecimal.ZERO, 0L));
    }

    private static List<String> idsOf(List<Transaction> transactions) {
        List<String> ids = new ArrayList<String>();
        for (Transaction tx : transactions) {
            ids.add(tx.getId());
        }
        return ids;
    }

    @Test
    @DisplayName("plain ArrayList race: never MORE than expected; the loss is printed, never asserted")
    void plainArrayListRaceOnlyPromisesTheSafeDirection() throws InterruptedException {
        final List<String> list = new ArrayList<String>();
        final int expected = 4 * 25_000;
        int failures = raceAdds(list, 4, 25_000);

        assertTrue(list.size() <= expected,
                "size can never EXCEED the number of add calls — the safe direction holds every run");
        // Deliberately NO assertion that something was lost: whether the window is hit, and
        // how often, depends on the machine and the scheduler. Asserting "loss > 0" would be
        // a flaky test; the printed number is the evidence, the inequality is the proof.
        System.out.println("plain ArrayList: expected " + expected + ", actual " + list.size()
                + " -> lost " + (expected - list.size()) + " adds, threw inside add(): " + failures);
    }

    @Test
    @DisplayName("the SAME race through Collections.synchronizedList: EXACT count, every run")
    void synchronizedListRaceIsExact() throws InterruptedException {
        final List<String> list = Collections.synchronizedList(new ArrayList<String>());
        final int expected = 4 * 25_000;
        int failures = raceAdds(list, 4, 25_000);

        assertEquals(expected, list.size(),
                "the wrapper's mutex closes every window add() has — this equality is a PROMISE, not a measurement");
        assertEquals(0, failures, "no add can throw: the capacity check runs under the same lock");
        System.out.println("synchronizedList: expected " + expected + ", actual " + list.size()
                + " -> exact (start gun + join)");
    }

    @Test
    @DisplayName("single-thread CME: list.remove() inside a for-each throws — no second thread involved")
    void removeDuringForEachThrowsCme() {
        final List<Transaction> transactions = Transaction.sampleTransactions();  // 4 items, target at index 1

        ConcurrentModificationException e = assertThrows(ConcurrentModificationException.class, () -> {
            for (Transaction tx : transactions) {
                if (tx.getDescription().startsWith("Reversed")) {
                    transactions.remove(tx);   // modCount++ behind the snapshot -> the NEXT next() throws
                }
            }
        });

        StackTraceElement top = e.getStackTrace()[0];
        System.out.println("CAUGHT -> " + e.getClass().getName()
                + " thrown from " + top.getClassName() + "." + top.getMethodName()
                + " — the modCount comparison itself, on ONE thread");
    }

    @Test
    @DisplayName("the escape: removing the second-to-last element throws NOTHING — fail-fast is best-effort")
    void secondToLastRemovalEscapesTheDetector() {
        List<Transaction> transactions = new ArrayList<Transaction>();
        transactions.add(new Transaction("T-1001", "Salary", new BigDecimal("3100.00"), 1700000000000L));
        transactions.add(new Transaction("T-1002", "Reversed transfer", new BigDecimal("45.50"), 1700003600000L));
        transactions.add(new Transaction("T-1004", "Interest", new BigDecimal("12.34"), 1700010800000L));

        List<String> visited = new ArrayList<String>();
        for (Transaction tx : transactions) {        // NO try/catch — the whole point is that nothing throws
            visited.add(tx.getId());
            if (tx.getDescription().startsWith("Reversed")) {
                transactions.remove(tx);
            }
        }

        // WHY it escapes: hasNext() is `cursor != size` and NEVER checks modCount. Removing the
        // second-to-last element leaves cursor == size, hasNext() says "done", and next() — the
        // only place the modCount check lives — never runs. (Removing the ACTUAL last element
        // leaves cursor PAST size: hasNext() stays true, next() runs the check, CME after all.)
        assertEquals(Arrays.asList("T-1001", "T-1002"), visited, "the loop ended early — T-1004 was silently SKIPPED");
        assertEquals(2, transactions.size(), "the removal DID happen");
        assertFalse(containsId(transactions, "T-1002"), "the removed element is gone");
        assertTrue(containsId(transactions, "T-1004"), "...and the survivor was never visited");
        System.out.println("no exception, visited " + visited + ", list now " + idsOf(transactions)
                + " — the detector never fired: 'should be used only to detect bugs' (javadoc)");
    }

    @Test
    @DisplayName("iterator.remove(): the ONLY sanctioned removal — no CME, element gone, walk completes")
    void iteratorRemoveIsTheSanctionedWay() {
        List<Transaction> transactions = Transaction.sampleTransactions();

        Iterator<Transaction> it = transactions.iterator();
        while (it.hasNext()) {
            Transaction tx = it.next();
            if (tx.getDescription().startsWith("Reversed")) {
                it.remove();                         // removes, THEN expectedModCount = modCount
            }
        }

        assertEquals(3, transactions.size());
        assertFalse(containsId(transactions, "T-1002"), "the reversed transfer is gone");
        assertTrue(containsId(transactions, "T-1004"), "and the walk reached the end — nothing skipped");
        System.out.println("iterator.remove(): " + idsOf(transactions) + " — no CME, every run (module 03's rule, module 09's why)");
    }

    @Test
    @DisplayName("set() mid-iteration is NOT structural: modCount untouched, no CME")
    void setDuringIterationTripsNothing() {
        List<Transaction> transactions = Transaction.sampleTransactions();

        ListIterator<Transaction> it = transactions.listIterator();
        int seen = 0;
        while (it.hasNext()) {
            Transaction tx = it.next();
            seen++;
            it.set(new Transaction(tx.getId(), tx.getDescription() + " (audited)",
                    tx.getAmount(), tx.getTimestampMillis()));
        }

        assertEquals(4, seen, "the walk completed — replacing a slot never invalidates an iterator");
        assertEquals(4, transactions.size(), "same slots, same shape: set() is not structural");
        assertTrue(transactions.get(0).getDescription().endsWith("(audited)"));
        System.out.println("every element replaced mid-walk, e.g. " + transactions.get(0) + " — no CME");
    }

    @Test
    @DisplayName("the wrapper trap: put THROUGH a synchronizedMap mid-iteration -> CME anyway")
    void wrapperDoesNotProtectIteration() {
        final Map<String, Account> accounts = Collections.synchronizedMap(new HashMap<String, Account>());
        for (Account a : Account.sampleAccounts()) {
            accounts.put(a.getId(), a);
        }

        assertThrows(ConcurrentModificationException.class, () -> {
            for (String id : accounts.keySet()) {
                accounts.put("ACC-9999", new Account("ACC-9999", "Zoe", new BigDecimal("1.00")));
                // Unconditional is fine: the FIRST put of a new key is structural, and the
                // second next() throws before the body can run again. The wrapper locked the
                // put() perfectly — the ITERATION is a sequence of calls with the lock
                // released in between, and the keySet iterator is raw and fail-fast.
            }
        });

        assertEquals(4, accounts.size(), "the put itself SUCCEEDED — that is the trap");
        System.out.println("CME through a synchronizedMap — the wrapper protects CALLS, not SEQUENCES");
    }

    @Test
    @DisplayName("iteration INSIDE synchronized(map) with a concurrent writer: no CME, exact view (joined)")
    void lockedIterationSurvivesAConcurrentWriter() throws InterruptedException {
        final Map<String, String> live = Collections.synchronizedMap(new HashMap<String, String>());
        live.put("T-1001", "seeded");
        live.put("T-1002", "seeded");
        live.put("T-1003", "seeded");

        final CountDownLatch mainIsInside = new CountDownLatch(1);
        Thread writer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mainIsInside.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < 3; i++) {
                    live.put("T-LATE-" + i, "late");   // blocks on the wrapper's mutex until main lets go
                }
            }
        }, "test-late-writer");
        writer.start();

        final List<String> keysSeen = new ArrayList<String>();   // written only by the test thread
        synchronized (live) {                                    // the wrapper's OWN mutex — the documented way
            mainIsInside.countDown();                            // the writer may approach only now...
            for (String key : live.keySet()) {                   // ...and every put it attempts parks
                keysSeen.add(key);                               // on the monitor until this block ends
            }
        }
        writer.join();                                           // DETERMINISM: judge only after join

        assertEquals(3, keysSeen.size(), "the locked walk saw exactly the seeded keys — no interleaving possible");
        assertEquals(6, live.size(), "all 3 late puts landed — the instant the walk released the mutex");
        System.out.println("locked walk saw " + keysSeen.size() + " keys; map holds " + live.size()
                + " after join — consistent, every run");
    }

    @Test
    @DisplayName("check-then-act under a wrapper: the scripted interleaving loses A's write — by construction")
    void checkThenActLosesUpdateUnderWrapper() {
        Map<String, Account> accounts = Collections.synchronizedMap(new HashMap<String, Account>());
        String id = "ACC-1027";

        boolean aSawKey = accounts.containsKey(id);    // A checks — the lock is taken and RELEASED around the call
        boolean bSawKey = accounts.containsKey(id);    // B checks in the GAP — sees the same empty map
        assertFalse(aSawKey);
        assertFalse(bSawKey);
        accounts.put(id, new Account(id, "Dana", new BigDecimal("100.00")));   // A acts on a stale answer
        accounts.put(id, new Account(id, "Dana", new BigDecimal("250.00")));   // B acts on a stale answer too

        assertEquals(new BigDecimal("250.00"), accounts.get(id).getBalance(),
                "B's write landed last — A's 100.00 is GONE; the account should hold 350.00");
        System.out.println("two atomic calls, one non-atomic SEQUENCE: final balance "
                + accounts.get(id).getBalance() + " instead of 350.00 — module 10's putIfAbsent deletes the gap");
    }

    @Test
    @DisplayName("Vector's method-level synchronization: concurrent adds are EXACT (joined)")
    void vectorConcurrentAddsAreExact() throws InterruptedException {
        Vector<String> vector = new Vector<String>();
        int failures = raceAdds(vector, 4, 10_000);    // Vector IS a List<String> — the same helper, the same race

        assertEquals(40_000, vector.size(), "add() is synchronized — 1997's wrapper granularity");
        assertEquals(0, failures, "and nothing can throw inside it");
        System.out.println("Vector: 4 x 10,000 adds -> " + vector.size()
                + " — exact, exactly like synchronizedList: same granularity, same limits");
    }

    @Test
    @DisplayName("Vector does NOT fix compound actions: get-then-set loses an update (scripted)")
    void vectorCompoundActionStillLosesUpdate() {
        Vector<Account> accounts = new Vector<Account>();
        accounts.add(new Account("ACC-1027", "Dana", new BigDecimal("100.00")));

        Account aRead = accounts.get(0);               // A reads — lock taken, RELEASED on return
        Account bRead = accounts.get(0);               // B reads in the gap — stale by construction
        accounts.set(0, new Account("ACC-1027", "Dana", aRead.getBalance().add(new BigDecimal("50.00"))));
        accounts.set(0, new Account("ACC-1027", "Dana", bRead.getBalance().add(new BigDecimal("30.00"))));

        assertEquals(new BigDecimal("130.00"), accounts.get(0).getBalance(),
                "100.00 + 50.00 + 30.00 should be 180.00 — B's stale read wiped out A's deposit");
        System.out.println("Vector get()/set() are each synchronized; get-compute-set is NOT: final "
                + accounts.get(0).getBalance() + " — the legacy lock reaches exactly as far as the wrapper's");
    }
}
