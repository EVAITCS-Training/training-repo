package com.evaitcs.advanced.threads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for module 04's claims. Run with:
 *   .\mvnw.cmd -pl 04-threads-and-jmm test
 * Each test proves ONE claim and prints its evidence, so the run doubles as a demo.
 *
 * DETERMINISM POLICY (the whole game in a concurrency module): every worker is
 * choreographed with a CountDownLatch start-gun and joined before we assert; states are
 * reached through awaitState()'s bounded poll; exact counts are asserted ONLY where the
 * memory model or an atomic guarantees them (join edge, AtomicInteger). The one place a
 * failure CANNOT be guaranteed — the plain non-volatile spin — asserts nothing about the
 * spin and only PRINTS which outcome happened. Threads that might spin forever are
 * daemons so the JVM always exits. No timing is asserted except generous ceilings.
 */
class ThreadModelTest {

    @Test
    @DisplayName("lifecycle: a thread passes through NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING and TERMINATED, and getState() names each exactly")
    void lifecycleStatesAreExact() throws Exception {
        // NEW — allocated, not started.
        Thread newborn = new Thread(() -> { });
        assertEquals(Thread.State.NEW, newborn.getState());

        // RUNNABLE — busy-spinning, so genuinely runnable.
        CountDownLatch spinning = new CountDownLatch(1);
        AtomicBoolean stop = new AtomicBoolean(false);
        Thread runner = new Thread(() -> {
            spinning.countDown();
            while (!stop.get()) {
                Thread.onSpinWait();
            }
        }, "runner");
        runner.start();
        spinning.await();
        LifecycleDemo.awaitState(runner, Thread.State.RUNNABLE, 5_000);
        assertEquals(Thread.State.RUNNABLE, runner.getState());
        stop.set(true);
        runner.join();

        // BLOCKED — main holds the monitor, so the worker cannot enter: BLOCKED is certain.
        Object monitor = new Object();
        Thread blocked = new Thread(() -> {
            synchronized (monitor) {
                // reached only after main releases
            }
        }, "blocked");
        synchronized (monitor) {
            blocked.start();
            LifecycleDemo.awaitState(blocked, Thread.State.BLOCKED, 5_000);
            assertEquals(Thread.State.BLOCKED, blocked.getState());
        }
        blocked.join();

        // WAITING — parked on a latch with no deadline.
        CountDownLatch gate = new CountDownLatch(1);
        Thread waiter = new Thread(() -> {
            try {
                gate.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "waiter");
        waiter.start();
        LifecycleDemo.awaitState(waiter, Thread.State.WAITING, 5_000);
        assertEquals(Thread.State.WAITING, waiter.getState());
        gate.countDown();
        waiter.join();

        // TIMED_WAITING — parked in sleep() with a deadline.
        Thread sleeper = new Thread(() -> {
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "sleeper");
        sleeper.start();
        LifecycleDemo.awaitState(sleeper, Thread.State.TIMED_WAITING, 5_000);
        assertEquals(Thread.State.TIMED_WAITING, sleeper.getState());
        sleeper.interrupt();
        sleeper.join();

        // TERMINATED — after run() finishes and join() returns.
        newborn.start();
        newborn.join();
        assertEquals(Thread.State.TERMINATED, newborn.getState());

        System.out.println("all six states observed exactly: NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED");
    }

    @Test
    @DisplayName("visibility: a volatile stop-flag is ALWAYS seen, so the spinning worker terminates within a generous deadline")
    void volatileFlagAlwaysStopsWorker() throws Exception {
        VisibilityDemo.VolatileFlag flag = new VisibilityDemo.VolatileFlag();
        Thread worker = new Thread(() -> {
            while (flag.running) {
                Thread.onSpinWait();
            }
        }, "volatile-spinner");
        worker.setDaemon(true);
        worker.start();
        Thread.sleep(50);            // let it get into the loop
        flag.running = false;        // volatile write => happens-before the worker's next volatile read
        worker.join(5_000);          // generous ceiling, not a timing assertion
        assertFalse(worker.isAlive(), "volatile write HB volatile read => the worker MUST have stopped");
        System.out.println("volatile worker stopped within join(5000): isAlive() = " + worker.isAlive());
    }

    @Test
    @DisplayName("visibility: a plain stop-flag MAY never be seen — we PRINT the observed outcome and assert only that the worker is a daemon, never that it spun")
    void plainFlagOutcomeIsReportedNotAsserted() throws Exception {
        VisibilityDemo.PlainFlag flag = new VisibilityDemo.PlainFlag();
        Thread worker = new Thread(() -> {
            while (flag.running) {
                Thread.onSpinWait();
            }
        }, "plain-spinner");
        worker.setDaemon(true);      // MANDATORY: it might never stop, so it must not block JVM exit
        worker.start();
        Thread.sleep(200);           // let the JIT compile the hot loop (raises the odds of a stale read)
        flag.running = false;        // plain write => NO happens-before edge to the worker
        worker.join(1_000);
        boolean stillSpinning = worker.isAlive();

        // The JMM permits BOTH outcomes, so we assert neither — we only report reality.
        System.out.println("plain worker still spinning after join(1000)? " + stillSpinning
                + (stillSpinning
                    ? "  -> stale/hoisted read REPRODUCED (the JMM, not a JVM bug)"
                    : "  -> the write happened to be seen; absence of a hang is NOT a guarantee"));

        // The one safety property we CAN assert: it is a daemon, so the build always exits.
        assertTrue(worker.isDaemon(), "the guarantee we can make: daemon => a forever-spin never hangs the JVM");
    }

    @Test
    @DisplayName("happens-before: after worker.join(), every plain (non-volatile) field the worker wrote is visible to the reader")
    void joinPublishesPlainFieldWrites() throws Exception {
        HappensBeforeDemo.Report report = new HappensBeforeDemo.Report();
        Account account = new Account("ACC-1027", "Dana", new BigDecimal("100.00"));

        Thread worker = new Thread(() -> {
            report.rowsProcessed = 42;                       // plain write
            report.message = "statement reconciled";         // plain write
            report.total = new BigDecimal("1250.75");        // plain write
            account.deposit(new BigDecimal("250.00"));      // plain writes: balance + version
            report.complete = true;                          // plain write
        }, "writer");

        worker.start();
        worker.join();   // THE EDGE: everything the worker did happens-before join() returns

        assertEquals(42, report.rowsProcessed);
        assertEquals("statement reconciled", report.message);
        assertEquals(new BigDecimal("1250.75"), report.total);
        assertTrue(report.complete);
        assertEquals(new BigDecimal("350.00"), account.getBalance());   // 100.00 + 250.00
        assertEquals(1, account.getVersion());

        System.out.println("after join(): rows=" + report.rowsProcessed + ", msg=" + report.message
                + ", total=" + report.total + ", complete=" + report.complete + ", account=" + account);
    }

    @Test
    @DisplayName("interruption: interrupt() leaves the flag set, a thrown InterruptedException CLEARS it, and re-interrupting restores it")
    void interruptFlagSemantics() throws Exception {
        InterruptDemo.Facts facts = new InterruptDemo.Facts();

        // (a) interrupt() with NO blocking call: the flag stays set, and isInterrupted() does not clear it.
        Thread selfInterrupter = new Thread(() -> {
            Thread.currentThread().interrupt();
            facts.flagAfterInterrupt = Thread.currentThread().isInterrupted();
            facts.flagAfterSecondRead = Thread.currentThread().isInterrupted();
        }, "self-interrupter");
        selfInterrupter.start();
        selfInterrupter.join();

        // (b) a blocking call throws AND clears the flag; (c) re-interrupt restores it.
        CountDownLatch blocking = new CountDownLatch(1);
        Thread sleeper = new Thread(() -> {
            blocking.countDown();
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                facts.flagInsideCatch = Thread.currentThread().isInterrupted();   // cleared by the throw
                Thread.currentThread().interrupt();                               // re-interrupt idiom
                facts.flagAfterReinterrupt = Thread.currentThread().isInterrupted();
            }
        }, "sleeper");
        sleeper.start();
        blocking.await();
        sleeper.interrupt();
        sleeper.join();

        assertTrue(facts.flagAfterInterrupt, "interrupt() with no blocking call leaves the flag SET");
        assertTrue(facts.flagAfterSecondRead, "isInterrupted() is a pure read — it does NOT clear the flag");
        assertFalse(facts.flagInsideCatch, "the thrown InterruptedException CLEARED the flag");
        assertTrue(facts.flagAfterReinterrupt, "re-interrupt() RESTORED the flag for callers above");

        System.out.println("flag: afterInterrupt=" + facts.flagAfterInterrupt
                + ", afterSecondRead=" + facts.flagAfterSecondRead
                + ", insideCatch=" + facts.flagInsideCatch
                + ", afterReinterrupt=" + facts.flagAfterReinterrupt);
    }

    @Test
    @DisplayName("atomics: 1000 virtual threads incrementing one AtomicInteger produce EXACTLY 1000 — CAS never loses an update")
    void thousandVirtualThreadsCountExactly() throws Exception {
        int n = 1_000;
        AtomicInteger counter = new AtomicInteger();
        CountDownLatch startGun = new CountDownLatch(1);   // release all at once => maximum contention
        CountDownLatch done = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            Thread.ofVirtual().name("incrementer-", i).start(() -> {
                try {
                    startGun.await();
                    counter.incrementAndGet();             // CAS retry loop => no lost update
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        startGun.countDown();
        boolean allFinished = done.await(30, TimeUnit.SECONDS);   // generous ceiling
        assertTrue(allFinished, "all 1000 virtual threads finished within the deadline");
        assertEquals(n, counter.get(), "AtomicInteger.incrementAndGet is exact under any contention");

        System.out.println("1000 virtual threads, one AtomicInteger -> " + counter.get() + " (exact, every run)");
    }
}
