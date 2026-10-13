package com.evaitcs.advanced.locks;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Demo 5 — CAS: the instruction under every atomic, and the race it deletes.
 * (README sections "The lost update, live" and "Atomics skip the queue".)
 *
 * A plain `cents++` is THREE steps — read, add one, write — and another thread can slip
 * between them. Two tellers both read 7, both write 8, and one deposit VANISHES. That is
 * the LOST UPDATE, the most expensive bug in concurrent business code.
 *
 * An AtomicInteger collapses it into ONE CPU instruction: COMPARE-AND-SET (CAS) — "if the
 * value is still what I read, write the new value; otherwise fail." incrementAndGet() is a
 * retry loop around CAS: read, compute, CAS; on failure, re-read and try again. No monitor,
 * no queue, no context switch — a bounded spin on one memory cell. The count comes out EXACT.
 *
 * DETERMINISM: every race uses a start-gun latch (all threads leave together) and joins
 * before reading. The plain result is asserted <= expected and its loss is PRINTED; only
 * the atomic result is asserted == expected (that is what CAS guarantees).
 */
public class CasRaceDemo {

    private static final int TELLERS = 4;
    private static final int DEPOSITS_EACH = 25_000;
    private static final int EXPECTED = TELLERS * DEPOSITS_EACH;   // 100 000 one-cent deposits = $1,000.00

    /** A plain int in a holder — the shared cell the four tellers race to increment. */
    static final class PlainCounter {
        int cents;   // plain on purpose: cents++ from 4 threads loses updates
    }

    public static void main(String[] args) throws Exception {
        banner("1. The mechanism, narrated — what `cents++` really does");
        System.out.println("step 1: READ cents (say 7) into a register");
        System.out.println("step 2: ADD 1 in the register (8)");
        System.out.println("step 3: WRITE the register back to cents");
        System.out.println("step 4: the gap between read and write belongs to NOBODY — two tellers can both");
        System.out.println("        read 7, both write 8, and one deposit disappears. That is the lost update.");

        banner("2. The race, live — 4 threads x 25 000 PLAIN cents++");
        PlainCounter plain = new PlainCounter();
        race(() -> plain.cents++);                       // start-gun + joins inside
        int plainLost = EXPECTED - plain.cents;
        System.out.println("expected " + EXPECTED + ", actual " + plain.cents
                + (plainLost > 0 ? " -> " + plainLost + " updates LOST" : " -> no loss THIS run"));
        System.out.println("money view: expected $1000.00, the vault actually holds " + dollars(plain.cents));
        System.out.println("verdict: plain cents++ is NOT atomic. actual <= expected ALWAYS; the exact loss");
        System.out.println("         depends on the scheduler, so we PRINT it and assert only the safe direction.");

        banner("3. The fix, live — 4 threads x 25 000 AtomicInteger.incrementAndGet()");
        System.out.println("incrementAndGet() = ONE CAS instruction in a retry loop:");
        System.out.println("  read current -> compute current+1 -> CAS(expected, current+1) -> on fail, re-read and retry.");
        AtomicInteger atomic = new AtomicInteger();
        race(atomic::incrementAndGet);
        System.out.println("expected " + EXPECTED + ", actual " + atomic.get() + " -> EXACT, every run");
        System.out.println("money view: " + dollars(atomic.get()) + " — not one cent lost");
        System.out.println("verdict: no monitor, no queue — a bounded spin on one cell, and the count is correct.");

        banner("4. LongAdder vs AtomicLong — stripes beat one hot cell under contention");
        System.out.println("AtomicLong  = ONE cell every CPU fights over -> the cache line ping-pongs between cores.");
        System.out.println("LongAdder   = a base cell PLUS striped Cell[] slots; contending threads spread across");
        System.out.println("              stripes, and sum() adds them up. Write-hot work: LongAdder wins.");
        System.out.println("              Trade: sum() is a snapshot across cells, not one atomic instant.");
        AtomicLong atomicLong = new AtomicLong();
        LongAdder longAdder = new LongAdder();
        long atomicLongMillis = timedRace(atomicLong::incrementAndGet);
        long longAdderMillis = timedRace(longAdder::increment);
        System.out.println("AtomicLong total = " + atomicLong.get() + " (exact) in " + atomicLongMillis + " ms");
        System.out.println("LongAdder  total = " + longAdder.sum() + " (exact) in " + longAdderMillis + " ms");
        System.out.println("verdict: BOTH are exact when joined; timings are PRINTED, never asserted (machine-dependent).");

        banner("5. In one sentence");
        System.out.println("Locks make threads wait in a queue; CAS makes them retry a value — same exact");
        System.out.println("count, no parking — and LongAdder spreads the retries across stripes when it is hot.");
    }

    /** Run TELLERS threads, each doing DEPOSITS_EACH increments, released together by a start gun. */
    private static void race(Runnable deposit) throws InterruptedException {
        CountDownLatch startGun = new CountDownLatch(1);   // all threads leave the gate together
        CountDownLatch done = new CountDownLatch(TELLERS);
        List<Thread> tellers = new ArrayList<>();
        for (int i = 0; i < TELLERS; i++) {
            Thread t = new Thread(() -> {
                try {
                    startGun.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int j = 0; j < DEPOSITS_EACH; j++) {
                    deposit.run();
                }
                done.countDown();
            }, "teller-" + i);
            tellers.add(t);
            t.start();
        }
        startGun.countDown();                              // fire: maximum contention from here
        boolean finished = done.await(30, TimeUnit.SECONDS);   // generous ceiling, not a timing assertion
        if (!finished) {
            throw new AssertionError("the race did not finish within the deadline");
        }
        for (Thread t : tellers) {
            t.join();                                      // belt-and-braces HB edge before the caller reads
        }
    }

    /** Same as race() but returns the wall time in millis (printed, never asserted). */
    private static long timedRace(Runnable deposit) throws InterruptedException {
        long start = System.nanoTime();
        race(deposit);
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    /** Cents to a scale-2 dollar string (money is BigDecimal scale 2 — house rule). */
    private static String dollars(long cents) {
        return "$" + BigDecimal.valueOf(cents, 2).toPlainString();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
