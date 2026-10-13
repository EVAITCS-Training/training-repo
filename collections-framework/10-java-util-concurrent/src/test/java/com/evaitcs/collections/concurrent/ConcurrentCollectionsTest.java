package com.evaitcs.collections.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 10-java-util-concurrent test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * DETERMINISM, the module's rule, applied three ways:
 *   - snapshot semantics are STRUCTURAL: a copy-on-write iterator walks a frozen array, so
 *     "does not see the later add, never throws CME" is a fact about the design, asserted
 *     with zero timing involved;
 *   - races are CHOREOGRAPHED: start-gun latches release the threads together, join() runs
 *     before any assertion, and exact counts are asserted only where the engine guarantees
 *     them (putIfAbsent's single winner, computeIfAbsent's 100,000, the FIFO pipeline);
 *   - the ONE timing-based claim (take() blocks) uses a guaranteed ~300ms park and a 150ms
 *     verdict floor — a 2x margin, so the verdict cannot flip on a slow machine.
 */
class ConcurrentCollectionsTest {

    private static List<String> idsOf(Iterable<Transaction> transactions) {
        List<String> ids = new ArrayList<String>();
        for (Transaction tx : transactions) {
            ids.add(tx.getId());
        }
        return ids;
    }

    @Test
    @DisplayName("COW iterator walks a frozen snapshot: a mid-iteration add is invisible, no CME")
    void cowIteratorWalksAFrozenSnapshot() {
        CopyOnWriteArrayList<Transaction> alerts =
                new CopyOnWriteArrayList<Transaction>(Transaction.sampleTransactions());   // 3 seeded

        Iterator<Transaction> it = alerts.iterator();          // FREEZE: grabs today's array
        alerts.add(new Transaction("T-1004", "New-account alert", new BigDecimal("10.00"), 1700010800000L));

        List<String> seen = new ArrayList<String>();           // no try/catch: nothing CAN throw
        while (it.hasNext()) {
            seen.add(it.next().getId());
        }

        assertEquals(Arrays.asList("T-1001", "T-1002", "T-1003"), seen,
                "the snapshot froze before the add — T-1004 is invisible to THIS iterator");
        assertEquals(4, alerts.size(), "...while the live list DID grow: a future iterator sees all 4");
        System.out.println("iterator saw " + seen + ", list now " + idsOf(alerts)
                + " — no CME: the iterator and the writer look at two different arrays (module 09's CME, extinct)");
    }

    @Test
    @DisplayName("COW iterator.remove() throws UnsupportedOperationException — the snapshot is read-only")
    void cowIteratorRemoveIsUnsupported() {
        CopyOnWriteArrayList<Transaction> alerts =
                new CopyOnWriteArrayList<Transaction>(Transaction.sampleTransactions());

        final Iterator<Transaction> it = alerts.iterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, () -> it.remove());

        assertEquals(3, alerts.size(), "the list itself is untouched — removals go through the LIST (they copy)");
        System.out.println("iterator.remove() -> UnsupportedOperationException: removing from a frozen copy");
        System.out.println("would change nothing real, so the COW iterator refuses to pretend.");
    }

    @Test
    @DisplayName("putIfAbsent race: exactly ONE winner gets null back (start gun, joined)")
    void putIfAbsentRaceHasExactlyOneWinner() throws InterruptedException {
        final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<String, Account>();
        final Account fromA = new Account("ACC-1027", "Dana", new BigDecimal("100.00"));
        final Account fromB = new Account("ACC-1027", "Dana", new BigDecimal("250.00"));
        final CountDownLatch startGun = new CountDownLatch(1);
        final Account[] returned = new Account[2];             // each racer records its own return value

        Thread racerA = new Thread(new Runnable() {             // anonymous Runnable: classic idiom
            @Override
            public void run() {
                try {
                    startGun.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                returned[0] = accounts.putIfAbsent("ACC-1027", fromA);
            }
        }, "test-racer-A");
        Thread racerB = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startGun.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                returned[1] = accounts.putIfAbsent("ACC-1027", fromB);
            }
        }, "test-racer-B");

        racerA.start();
        racerB.start();
        startGun.countDown();
        racerA.join();                                          // DETERMINISM: judge only after join
        racerB.join();

        int winners = (returned[0] == null ? 1 : 0) + (returned[1] == null ? 1 : 0);
        assertEquals(1, winners,
                "exactly one racer saw 'absent' and won — WHICH one varies run to run, THAT one exists does not");

        Account winnerValue = returned[0] == null ? fromA : fromB;
        Account loserSaw = returned[0] == null ? returned[1] : returned[0];
        // assertSame, NOT assertEquals: both racers' Accounts are EQUAL by id (core-java 08),
        // so equals could never tell the winner's value from the loser's. Identity can.
        assertSame(winnerValue, accounts.get("ACC-1027"), "the map holds the winner's VERY object");
        assertSame(winnerValue, loserSaw, "and the loser received the winner's value as its return — the coordination IS the return value");
        System.out.println("racer-A got " + (returned[0] == null ? "null (WON)" : "the other value (lost)")
                + ", racer-B got " + (returned[1] == null ? "null (WON)" : "the other value (lost)")
                + " — exactly one null, every run");
    }

    @Test
    @DisplayName("computeIfAbsent installs ONE AtomicInteger; 4 threads x 25,000 increments = exactly 100,000")
    void computeIfAbsentCounterIsExact() throws InterruptedException {
        final ConcurrentHashMap<String, AtomicInteger> hits = new ConcurrentHashMap<String, AtomicInteger>();
        final AtomicInteger factoryRuns = new AtomicInteger();
        final Function<String, AtomicInteger> factory = new Function<String, AtomicInteger>() {
            @Override
            public AtomicInteger apply(String key) {            // anonymous Function: classic idiom
                factoryRuns.incrementAndGet();
                return new AtomicInteger(0);
            }
        };

        final int threads = 4;
        final int perThread = 25_000;
        final CountDownLatch startGun = new CountDownLatch(1);
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            workers[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        startGun.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < perThread; i++) {
                        hits.computeIfAbsent("ACC-1027", factory).incrementAndGet();
                    }
                }
            }, "test-counter-" + t);
            workers[t].start();
        }
        startGun.countDown();
        for (int t = 0; t < threads; t++) {
            workers[t].join();
        }

        assertEquals(threads * perThread, hits.get("ACC-1027").get(),
                "two atomicities stacked: build-once install + lock-free CAS increments");
        assertEquals(1, factoryRuns.get(),
                "100,000 racing calls installed exactly ONE counter — containsKey-then-put could install two and lose a batch");
        System.out.println("computeIfAbsent: factory ran " + factoryRuns.get() + " time, total hits "
                + hits.get("ACC-1027").get() + " — exact, joined, every run");
    }

    @Test
    @DisplayName("ConcurrentHashMap refuses null keys AND null values; HashMap allows both — the ambiguity is why")
    void chmRejectsNullKeysAndValues() {
        final ConcurrentHashMap<String, Account> concurrent = new ConcurrentHashMap<String, Account>();
        final Account dana = new Account("ACC-1027", "Dana", new BigDecimal("2450.00"));

        assertThrows(NullPointerException.class, () -> concurrent.put(null, dana));
        assertThrows(NullPointerException.class, () -> concurrent.put("ACC-1027", null));

        // Control group: HashMap shrugs at both — single-threaded, containsKey disambiguates.
        Map<String, Account> plain = new HashMap<String, Account>();
        plain.put(null, dana);
        plain.put("ACC-1005", null);
        assertNull(plain.get("ACC-1005"));
        assertTrue(plain.containsKey("ACC-1005"), "the null was a VALUE — and single-threaded, you can PROVE it");
        assertFalse(plain.containsKey("ACC-9999"), "...under concurrency that second question arrives too late (NullPolicyDemo)");
        System.out.println("CHM: NPE on null key and null value, every run. Ban nulls and get()==null means");
        System.out.println("exactly one thing: ABSENT — containsKey-then-get is a check-then-act compound.");
    }

    @Test
    @DisplayName("producer/consumer over ArrayBlockingQueue: exact FIFO list, joined")
    void producerConsumerDeliversExactFifo() throws InterruptedException {
        final BlockingQueue<String> queue = new ArrayBlockingQueue<String>(4);
        final String pill = "DONE";
        final List<String> processed = new ArrayList<String>();   // touched ONLY by the consumer;
                                                                  // main reads it after join (happens-before)
        Thread producer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 1; i <= 10; i++) {
                        queue.put(String.format("TX-%04d", i));   // parks while full — backpressure
                    }
                    queue.put(pill);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "test-producer");
        Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        String id = queue.take();                 // parks while empty — no busy-wait
                        if (pill.equals(id)) {
                            break;
                        }
                        processed.add(id);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "test-consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        List<String> expected = new ArrayList<String>();
        for (int i = 1; i <= 10; i++) {
            expected.add(String.format("TX-%04d", i));
        }
        assertEquals(expected, processed,
                "one producer + one consumer: each element goes to exactly one taker, in offer order");
        assertTrue(queue.isEmpty(), "the pill was consumed, not processed");
        System.out.println("consumer processed " + processed + " — exact FIFO, every run");
    }

    @Test
    @DisplayName("ArrayBlockingQueue honors its capacity: the third offer() on a 2-queue is false")
    void arrayBlockingQueueRespectsCapacity() {
        BlockingQueue<String> queue = new ArrayBlockingQueue<String>(2);

        assertTrue(queue.offer("TX-0001"));
        assertTrue(queue.offer("TX-0002"));
        assertFalse(queue.offer("TX-0003"), "full: offer answers false instead of waiting — put() would park");
        assertEquals(2, queue.size(), "the constructor promise is never broken");

        assertEquals("TX-0001", queue.poll(), "FIFO drain");
        assertTrue(queue.offer("TX-0003"), "a slot opened — the refused element fits now");
        System.out.println("capacity 2: offer,offer -> true,true; third -> false; after one poll the third fits");
    }

    @Test
    @DisplayName("take() parks until an element arrives: >=150ms blocked against a 300ms-late offer")
    void takeBlocksUntilAnElementArrives() throws InterruptedException {
        final BlockingQueue<String> queue = new ArrayBlockingQueue<String>(2);
        final CountDownLatch aboutToTake = new CountDownLatch(1);
        final long[] waitedMillis = new long[1];                  // written by the consumer; read after join
        final String[] received = new String[1];

        Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    aboutToTake.countDown();                      // "I am at take()'s doorstep"...
                    long t0 = System.currentTimeMillis();
                    received[0] = queue.take();                   // ...and now I park: the queue is empty
                    waitedMillis[0] = System.currentTimeMillis() - t0;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "test-take-consumer");

        consumer.start();
        aboutToTake.await();                                      // do not start the clock before the consumer is there
        Thread.sleep(300);                                        // generous: guarantees ~300ms of parking
        queue.offer("ACC-PAYMENT");                               // the wakeup
        consumer.join();                                          // DETERMINISM: read the arrays only after join

        assertEquals("ACC-PAYMENT", received[0]);
        // The margin: sleep(300) parks the consumer for AT LEAST ~300ms before the offer;
        // the verdict floor is 150ms — a 2x margin absorbing clock granularity and slow
        // machines. The observed number varies; "take() really blocked" cannot flip.
        assertTrue(waitedMillis[0] >= 150,
                "take() must have blocked; observed " + waitedMillis[0] + "ms");
        System.out.println("take() parked for " + waitedMillis[0] + "ms (floor 150ms) before the delayed offer woke it");
    }

    @Test
    @DisplayName("ConcurrentSkipListMap keeps keys sorted: firstEntry/lastEntry exact, headMap a live view")
    void skipListMapKeepsKeysSorted() {
        ConcurrentSkipListMap<String, Account> byId = new ConcurrentSkipListMap<String, Account>();
        for (Account a : Account.sampleAccounts()) {              // seeded 1027, 1005, 1011 — scrambled
            byId.put(a.getId(), a);
        }

        assertEquals(Arrays.asList("ACC-1005", "ACC-1011", "ACC-1027"),
                new ArrayList<String>(byId.keySet()), "sorted on arrival, always — the skip list's contract");
        assertEquals("ACC-1005", byId.firstEntry().getKey());
        assertEquals("ACC-1027", byId.lastEntry().getKey());
        assertEquals(Arrays.asList("ACC-1005"), new ArrayList<String>(byId.headMap("ACC-1011").keySet()),
                "headMap is exclusive at the bound — the TreeMap contract from module 05, concurrent");
        System.out.println("sorted keys " + byId.keySet() + ", first " + byId.firstEntry().getKey()
                + ", last " + byId.lastEntry().getKey() + " — O(log n), and writers touch a few nodes, not the map");
    }

    @Test
    @DisplayName("ConcurrentLinkedQueue drains FIFO and reports isEmpty() — the O(1) question")
    void concurrentLinkedQueueDrainsFifoAndReportsEmpty() {
        ConcurrentLinkedQueue<String> fifo = new ConcurrentLinkedQueue<String>();
        fifo.offer("TX-0001");
        fifo.offer("TX-0002");
        fifo.offer("TX-0003");

        List<String> drained = new ArrayList<String>();
        String polled;
        while ((polled = fifo.poll()) != null) {                  // poll never blocks: null means empty
            drained.add(polled);
        }

        assertEquals(Arrays.asList("TX-0001", "TX-0002", "TX-0003"), drained, "FIFO — CAS links, no locks");
        assertNull(fifo.poll(), "empty: poll answers null immediately (take() on a BlockingQueue would park)");
        assertTrue(fifo.isEmpty(), "isEmpty() is the O(1) question — size() would WALK the chain (O(n)) and can be stale mid-walk");
        System.out.println("drained " + drained + ", then poll() -> null and isEmpty() -> true");
    }
}
