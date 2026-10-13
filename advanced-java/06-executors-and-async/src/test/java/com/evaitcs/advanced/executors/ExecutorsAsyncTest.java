package com.evaitcs.advanced.executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 06-executors-and-async test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Determinism contract (README "Class flow"): every task is JOINED before anything is
 * asserted — futures.get(), latches with generous deadlines (>= 5 s for work that takes
 * milliseconds), awaitTermination, or ExecutorService.close(). Exact counts are asserted
 * only where the mechanism guarantees them (pool maximum, allOf, close()). No test
 * asserts a wall-clock duration; the only sleeps are visibility floors and simulated IO.
 */
class ExecutorsAsyncTest {

    @Test
    @DisplayName("a fixed pool of 2 runs 6 tasks on at most 2 distinct threads — threads are rented, not sold")
    void poolReusesThreads() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Set<String> names = Collections.synchronizedSet(new HashSet<>());
        AtomicInteger completed = new AtomicInteger();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                futures.add(pool.submit(() -> {
                    names.add(Thread.currentThread().getName());
                    completed.incrementAndGet();
                    sleep(150);   // generous floor (>= 150 ms) so both workers are provably busy at once;
                                  // the <= 2 verdict comes from the pool's MAXIMUM size, never from timing
                }));
            }
            for (Future<?> f : futures) {
                f.get();          // join EVERY task before asserting — no race with the count
            }
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "generous deadline for ~1 s of work");
        }
        assertEquals(6, completed.get());
        assertTrue(names.size() <= 2, "six tasks, pool maximum 2 -> at most two distinct thread names");
        System.out.println("6 tasks completed on " + names.size() + " distinct thread(s): " + names);
    }

    @Test
    @DisplayName("a saturated tiny pool refuses work: busy core + full bounded queue + max reached throws RejectedExecutionException")
    void saturatedPoolRejects() throws Exception {
        ThreadPoolExecutor tiny = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        CountDownLatch occupantStarted = new CountDownLatch(1);
        CountDownLatch releaseOccupant = new CountDownLatch(1);
        try {
            tiny.execute(() -> {
                occupantStarted.countDown();
                await(releaseOccupant);        // holds the ONLY worker hostage, on cue
            });
            assertTrue(occupantStarted.await(5, TimeUnit.SECONDS), "start gun: the core worker is provably busy");
            tiny.execute(() -> { });           // fills the bounded queue (capacity exactly 1)

            RejectedExecutionException e =
                    assertThrows(RejectedExecutionException.class, () -> tiny.execute(() -> { }));
            assertNotNull(e.getMessage());
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName()
                    + " — README walkthrough step 4: busy core, full queue, max reached -> AbortPolicy fired");
        } finally {
            releaseOccupant.countDown();       // let the hostage go so shutdown can finish
            tiny.shutdown();
            assertTrue(tiny.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    @DisplayName("Future.get() blocks until the Callable finishes and returns exactly its value")
    void futureGetReturnsCallableValue() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<BigDecimal> total = pool.submit(() -> FutureBasicsDemo.sumOf(List.of(
                    new Transaction("T-1", "Payroll", new BigDecimal("2450.00")),
                    new Transaction("T-2", "Rent", new BigDecimal("-1200.00")))));
            BigDecimal value = total.get();    // get() did the waiting — nothing else synchronizes here
            assertEquals(new BigDecimal("1250.00"), value);
            assertTrue(total.isDone());
            System.out.println("get() returned " + value.toPlainString() + " (scale " + value.scale() + ") — money is BigDecimal, scale 2");
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    @DisplayName("the four-stage bank pipeline chains into one exact report string")
    void completableChainBuildsExactReport() {
        String report = CompletableFuturePipelineDemo
                .buildReport(CompletableFuturePipelineDemo.OK_ACCOUNT)
                .join();
        assertEquals("REPORT A-101 | Dana | balance 1500.00 | risk LOW | APPROVED", report);
        System.out.println("report = " + report);
    }

    @Test
    @DisplayName("exceptionally() catches the planted riskCheck failure and returns the exact MANUAL REVIEW fallback")
    void exceptionallyCatchesPlantedFailure() {
        String fallback = CompletableFuturePipelineDemo
                .buildReportSafely(CompletableFuturePipelineDemo.DOOMED_ACCOUNT)
                .join();
        assertEquals("REPORT A-666 | MANUAL REVIEW | risk service unreachable", fallback);

        // ...and the raw pipeline really did fail: join() throws CompletionException
        // wrapping the planted IllegalStateException (README walkthrough step 4).
        CompletionException raw = assertThrows(CompletionException.class,
                () -> CompletableFuturePipelineDemo
                        .buildReport(CompletableFuturePipelineDemo.DOOMED_ACCOUNT)
                        .join());
        assertTrue(raw.getCause() instanceof IllegalStateException, "the planted cause survives the wrap");
        assertEquals("risk service unreachable", raw.getCause().getMessage());
        System.out.println("fallback = " + fallback);
        System.out.println("raw join threw " + raw.getClass().getSimpleName() + " caused by " + raw.getCause());
    }

    @Test
    @DisplayName("thenCombine waits for two independent stages and sums their BigDecimals exactly")
    void thenCombineSumsTwoStages() {
        CompletableFuture<BigDecimal> balance =
                CompletableFuture.supplyAsync(() -> new BigDecimal("1200.50"));
        CompletableFuture<BigDecimal> creditLine =
                CompletableFuture.supplyAsync(() -> new BigDecimal("300.25"));

        BigDecimal exposure = balance.thenCombine(creditLine, BigDecimal::add).join();
        assertEquals(new BigDecimal("1500.75"), exposure);
        System.out.println("exposure = balance + creditLine = " + exposure.toPlainString());
    }

    @Test
    @DisplayName("allOf().join() returns only after EVERY stage finished — the counter is exact")
    void allOfWaitsForAll() {
        int tasks = 50;
        AtomicInteger completed = new AtomicInteger();
        CompletableFuture<?>[] all = new CompletableFuture<?>[tasks];
        for (int i = 0; i < tasks; i++) {
            all[i] = CompletableFuture.runAsync(completed::incrementAndGet);
        }
        CompletableFuture.allOf(all).join();   // THE guarantee: after join, every stage is done
        assertEquals(tasks, completed.get());
        System.out.println("allOf joined after exactly " + completed.get() + "/" + tasks + " increments");
    }

    @Test
    @DisplayName("the virtual-thread executor completes exactly 1000 tasks — close() waits for every one")
    void virtualThreadExecutorCompletesEveryTask() throws Exception {
        int tasks = 1000;
        AtomicInteger completed = new AtomicInteger();
        CountDownLatch allDone = new CountDownLatch(tasks);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                executor.submit(() -> {
                    completed.incrementAndGet();
                    allDone.countDown();
                });
            }
        }   // close() = shutdown + awaitTermination: every submitted task has finished HERE
        assertTrue(allDone.await(15, TimeUnit.SECONDS), "generous deadline — close() already waited");
        assertEquals(tasks, completed.get());
        System.out.println("completed = " + completed.get() + " of " + tasks + " on virtual threads — exact, by close()");
    }

    @Test
    @DisplayName("shutdown() + awaitTermination: the pool reports termination only after every task's result exists")
    void awaitTerminationTrueAfterShutdown() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            final int id = i;
            futures.add(pool.submit(() -> {
                sleep(50);        // visibility floor only; the verdict below comes from awaitTermination
                return id;
            }));
        }
        pool.shutdown();          // stop ACCEPTING; running + queued tasks finish first
        boolean terminated = pool.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(terminated, "awaitTermination is the ONLY proof a pool is done (README shutdown walkthrough)");

        List<Integer> results = new ArrayList<>();
        for (Future<Integer> f : futures) {
            assertTrue(f.isDone());
            results.add(f.get()); // returns immediately: everything finished before termination
        }
        assertEquals(List.of(0, 1, 2, 3), results);
        System.out.println("awaitTermination = true; results = " + results + " (submission order, 2 workers)");
    }

    // ---- helpers ----

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // module 04: restore the flag, never swallow it
            throw new AssertionError("interrupted while waiting", e);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting", e);
        }
    }
}
