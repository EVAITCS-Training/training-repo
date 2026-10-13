package com.evaitcs.collections.concurrent;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Demo 2 — ConcurrentHashMap: the ATOMIC compound actions module 09 could not build.
 *
 * The engine, narrated in banner 1 (no reflection needed — this is the documented design):
 * since Java 8 there are no segments. The table and node links are volatile, so READS take
 * no lock at all; a WRITE either CAS-es an empty bucket into existence or synchronizes on
 * THAT BUCKET's head node only. Lock granularity: one bucket, not the map. That is the
 * direct answer to module 09's global mutex — and to module 05's bucket machinery, which
 * now gets a lock per bucket instead of one lock per map.
 *
 * The headline act is what the fine locking BUYS: putIfAbsent / replace(k, old, new) /
 * computeIfAbsent fuse check-and-act into ONE atomic call, so module 09's lost update is
 * not "prevented" — it is UNREPRESENTABLE. There is no gap between the check and the act
 * for a second thread to fall into.
 *
 * DETERMINISM: banner 1 scripts the doomed interleaving (module 09's technique — the
 * interleaving is the lesson); banner 2 races two REAL threads through a start gun and
 * asserts by JOINED return values — which racer wins varies, that EXACTLY ONE wins does
 * not; banner 3's 4 x 25,000 counter lands on exactly 100,000 every run.
 */
public class ChmAtomicDemo {

    private static final int THREADS = 4;
    private static final int INCREMENTS_PER_THREAD = 25_000;   // 4 x 25,000 = exactly 100,000

    public static void main(String[] args) throws InterruptedException {
        banner("1. Module 09's lost update, replayed — putIfAbsent deletes the gap");

        System.out.println("The engine first, in four lines: no segments since Java 8. Reads are LOCK-FREE");
        System.out.println("(volatile table + nodes). A write CAS-es an empty bucket or synchronizes on THAT");
        System.out.println("BUCKET'S HEAD NODE only. size() sums counter cells afterwards — an ESTIMATE by design.");
        System.out.println();
        System.out.println("Now module 09's CheckThenActDemo script, line for line — same tellers, same gap:");

        ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<String, Account>();
        Account fromA = new Account("ACC-1027", "Dana", new BigDecimal("100.00"));
        Account fromB = new Account("ACC-1027", "Dana", new BigDecimal("250.00"));

        Account aResult = accounts.putIfAbsent("ACC-1027", fromA);   // A: check AND act, ONE call
        System.out.println("A: putIfAbsent(ACC-1027, Dana@100.00) -> " + aResult
                + "   <- null: 'you WON — your value is in'");
        Account bResult = accounts.putIfAbsent("ACC-1027", fromB);   // B: same single call, too late
        System.out.println("B: putIfAbsent(ACC-1027, Dana@250.00) -> " + bResult
                + "   <- A's account: 'already present — HERE is the current value'");
        System.out.println("final: " + accounts.get("ACC-1027"));
        System.out.println("Module 09's script ended at 250.00 with a vanished deposit. This one ends at");
        System.out.println("100.00 INTACT — because there is no gap: the check and the act are one atomic");
        System.out.println("step, and the RETURN VALUE is the coordination. The lost update is unrepresentable.");

        banner("2. The same race with REAL threads — exactly ONE winner, every run");

        final ConcurrentHashMap<String, Account> live = new ConcurrentHashMap<String, Account>();
        final Account racerAValue = new Account("ACC-1027", "Dana", new BigDecimal("100.00"));
        final Account racerBValue = new Account("ACC-1027", "Dana", new BigDecimal("250.00"));
        final CountDownLatch startGun = new CountDownLatch(1);
        final Account[] returned = new Account[2];         // each racer records its own return value

        Thread racerA = new Thread(new Runnable() {        // anonymous Runnable: classic idiom
            @Override
            public void run() {
                try {
                    startGun.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                returned[0] = live.putIfAbsent("ACC-1027", racerAValue);
            }
        }, "racer-A");
        Thread racerB = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startGun.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                returned[1] = live.putIfAbsent("ACC-1027", racerBValue);
            }
        }, "racer-B");

        racerA.start();
        racerB.start();
        startGun.countDown();
        racerA.join();                                     // DETERMINISM: judge the returns only after join
        racerB.join();

        int winners = (returned[0] == null ? 1 : 0) + (returned[1] == null ? 1 : 0);
        System.out.println("racer-A got back: " + (returned[0] == null ? "null  <- A WON" : returned[0] + "  <- A lost"));
        System.out.println("racer-B got back: " + (returned[1] == null ? "null  <- B WON" : returned[1] + "  <- B lost"));
        System.out.println("nulls handed out: " + winners + " — EXACTLY ONE, every run. WHICH racer wins varies;");
        System.out.println("THAT exactly one wins does not. That is what atomic buys: not who, but that one.");
        Account winnerValue = returned[0] == null ? racerAValue : racerBValue;
        System.out.println("the map holds the winner's VERY object: " + (live.get("ACC-1027") == winnerValue)
                + " — compared with ==, because both racers' Accounts are EQUAL by id (core-java 08):");
        System.out.println("equals could never tell the winner's value from the loser's. Identity can.");

        banner("3. computeIfAbsent — the build-once factory (4 threads x 25,000 increments)");

        final ConcurrentHashMap<String, AtomicInteger> hitsPerAccount =
                new ConcurrentHashMap<String, AtomicInteger>();
        final AtomicInteger factoryRuns = new AtomicInteger();

        // The factory as an anonymous class — java8-features 01 shortens this to
        // key -> new AtomicInteger(0): same function object, a tenth of the ceremony.
        // Keep factories CHEAP: computeIfAbsent runs the factory under the bucket lock,
        // at most once per key — never do real I/O in there. (The one-line print below
        // exists purely to PROVE the at-most-once guarantee on the console.)
        final Function<String, AtomicInteger> factory = new Function<String, AtomicInteger>() {
            @Override
            public AtomicInteger apply(String key) {
                factoryRuns.incrementAndGet();
                System.out.println("  [factory] building the counter for " + key + " — watch: this line prints ONCE");
                return new AtomicInteger(0);
            }
        };

        final CountDownLatch counterGun = new CountDownLatch(1);
        Thread[] workers = new Thread[THREADS];
        for (int t = 0; t < THREADS; t++) {
            workers[t] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        counterGun.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int i = 0; i < INCREMENTS_PER_THREAD; i++) {
                        hitsPerAccount.computeIfAbsent("ACC-1027", factory).incrementAndGet();
                    }
                }
            }, "counter-" + t);
            workers[t].start();
        }
        counterGun.countDown();
        for (int t = 0; t < THREADS; t++) {
            workers[t].join();
        }

        System.out.println("factory runs      : " + factoryRuns.get()
                + " — 100,000 racing calls, and EXACTLY ONE AtomicInteger was ever installed");
        System.out.println("total increments  : " + hitsPerAccount.get("ACC-1027").get() + " — exactly 100000, every run");
        System.out.println("TWO atomicities stacked: computeIfAbsent guarantees the counter is built-and-");
        System.out.println("installed exactly once (module 09's containsKey-then-put on a synchronizedMap can");
        System.out.println("install TWO counters and silently drop one batch); incrementAndGet guarantees no");
        System.out.println("increment is lost on the way in — a plain int++ under this race loses like demo 1 of");
        System.out.println("module 09 (AtomicInteger is a CAS loop: no lock at all).");

        banner("4. replace(key, old, new) — 'write ONLY if nothing changed since I read'");

        ConcurrentHashMap<String, BigDecimal> balances = new ConcurrentHashMap<String, BigDecimal>();
        balances.put("ACC-1027", new BigDecimal("100.00"));
        BigDecimal stale = balances.get("ACC-1027");                    // teller A reads 100.00...
        balances.put("ACC-1027", new BigDecimal("130.00"));             // ...meanwhile B's write lands (played directly)
        boolean refused = balances.replace("ACC-1027", stale, stale.add(new BigDecimal("50.00")));
        System.out.println("replace(key, what-I-read, my-new-value) -> " + refused
                + " — the map moved under A; the CAS-style replace REFUSED the stale write.");
        BigDecimal fresh = balances.get("ACC-1027");
        boolean accepted = balances.replace("ACC-1027", fresh, fresh.add(new BigDecimal("50.00")));
        System.out.println("the same call with a FRESH read -> " + accepted + ", balance now " + balances.get("ACC-1027"));
        System.out.println("The idiom: read, compute, replace-if-unchanged, RETRY on false.");
        System.out.println("Core-java 08 fine print: replace compares values with equals() — BigDecimal's equals");
        System.out.println("demands equal SCALE (2.0 is not 2.00; compareTo would say 0). That is the RIGHT");
        System.out.println("contract for a compare-and-swap. Account's equals-by-id would have matched ANY");
        System.out.println("version of the same account — the wrong contract here, which is why this banner");
        System.out.println("uses raw BigDecimal values. The equals you choose decides what 'unchanged' means.");

        banner("5. What ConcurrentHashMap does NOT promise");

        System.out.println("size() on a quiet map: " + balances.size() + " — exact when nothing moves. Under concurrent");
        System.out.println("writers it is a sum of counter cells gathered after the fact: an ESTIMATE, by design.");
        System.out.println("Iterators are WEAKLY CONSISTENT: never CME (fail-safe, like demo 1's snapshot) but");
        System.out.println("they may or may not show writes made after they were created — no exception, no promise.");
        System.out.println("Atomicity lives INSIDE the provided compound methods only: two separate calls —");
        System.out.println("get() then put() — still race exactly like module 09. The map is concurrent;");
        System.out.println("YOUR sequences still are not, unless one atomic call spans them.");
        System.out.println("And no nulls — keys OR values. That is not a limitation, it is a proof: next demo.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
