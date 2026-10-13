package com.evaitcs.java8.parallel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 07-parallel-streams test
 *
 * DETERMINISM RULE (read this before adding a test here): we NEVER assert on
 * timings, thread names of the common pool, or interleavings. Machines differ,
 * laptops throttle, CI boxes are shared. We assert CORRECTNESS — parallel must
 * produce the same answer as sequential — and we demonstrate the BROKEN patterns
 * with printed evidence plus generous inequalities that hold on every run.
 */
class ParallelStreamsTest {

    @Test
    @DisplayName("parallel sum of 2M longs equals the sequential sum EXACTLY")
    void parallelSumEqualsSequential() {
        long sequential = IntStream.rangeClosed(1, 2_000_000).asLongStream().sum();
        long parallel = IntStream.rangeClosed(1, 2_000_000).parallel().asLongStream().sum();

        System.out.println("sequential sum = " + sequential);
        System.out.println("parallel   sum = " + parallel);
        assertEquals(sequential, parallel, "integer math is exact — no excuse for a different answer");
    }

    @Test
    @DisplayName("parallel riskScore total matches sequential within floating-point tolerance")
    void parallelRiskScoreMatchesSequential() {
        double[] balances = Workload.fakeBalances(200_000);

        double sequential = Arrays.stream(balances).map(Workload::riskScore).sum();
        double parallel = Arrays.stream(balances).parallel().map(Workload::riskScore).sum();

        System.out.println("sequential total risk = " + sequential);
        System.out.println("parallel   total risk = " + parallel);
        System.out.println("difference            = " + Math.abs(sequential - parallel));
        // WHY a tolerance: floating-point addition is not associative, and the parallel
        // sum adds partial results in a DIFFERENT ORDER. Equal within epsilon, never bit-identical.
        assertEquals(sequential, parallel, Math.abs(sequential) * 1e-9 + 1e-6);
    }

    @Test
    @DisplayName("TRAP #1: shared list.add can lose data; collect(), sequential and count() cannot")
    void sharedStateTrapAndTheRightWays() {
        int n = 200_000;
        int expected = SharedStateTrapDemo.expectedFlagged(n); // 2000

        int broken = SharedStateTrapDemo.brokenWay(n);
        int collected = SharedStateTrapDemo.collectWay(n);
        int sequential = SharedStateTrapDemo.sequentialWay(n);
        int counted = SharedStateTrapDemo.countingWay(n);

        System.out.println("expected flagged        : " + expected);
        System.out.println("broken forEach + add    : " + broken + "  (may be < expected, may 'get lucky')");
        System.out.println("collect(toList())       : " + collected);
        System.out.println("sequential forEach + add: " + sequential);
        System.out.println("count() collector       : " + counted);

        // The broken way may crash, lose updates, or — on a lucky run — be right.
        // The only claim that holds on EVERY machine: it can never beat the correct count.
        assertTrue(broken <= expected,
                "shared-state add cannot produce MORE than expected (it can only lose)");
        // The three right ways are exact, every time, on every machine:
        assertEquals(expected, collected, "collect() is the safe way");
        assertEquals(expected, sequential, "sequential() is the safe way");
        assertEquals(expected, counted, "count() is the safe way");
    }

    @Test
    @DisplayName("TRAP #2: forEachOrdered and collect(toList()) preserve encounter order")
    void encounterOrderIsPreserved() {
        List<String> source = Arrays.asList("login", "transfer", "logout", "export", "login",
                "view", "transfer", "logout", "login", "export", "view", "transfer");

        List<String> ordered = new ArrayList<>();
        source.parallelStream().forEachOrdered(ordered::add); // delivers one-at-a-time, in order

        List<String> collected = source.parallelStream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());

        System.out.println("source            : " + source);
        System.out.println("forEachOrdered    : " + ordered);
        System.out.println("collect(toList()) : " + collected);

        assertEquals(source, ordered, "forEachOrdered must reproduce encounter order");
        assertEquals(source.size(), collected.size());
        for (int i = 0; i < source.size(); i++) {
            assertEquals(source.get(i).toUpperCase(), collected.get(i),
                    "collect(toList()) must reproduce encounter order at index " + i);
        }
        // NOTE: we deliberately do NOT assert anything about plain parallel forEach —
        // its order is undefined, and 'undefined' cannot be tested, only demonstrated.
    }

    @Test
    @DisplayName("submitting into a private ForkJoinPool keeps work OFF the common pool")
    void customPoolAvoidsCommonPool() throws Exception {
        ForkJoinPool privatePool = new ForkJoinPool(2);
        try {
            List<String> threads = privatePool.submit(() ->
                    IntStream.rangeClosed(1, 8).parallel()
                            .mapToObj(i -> Thread.currentThread().getName())
                            .collect(Collectors.toList())
            ).get();

            System.out.println("threads seen inside the private pool: " + threads);
            assertEquals(8, threads.size());
            for (String name : threads) {
                assertFalse(name.contains("commonPool"),
                        "work escaped to the shared common pool: " + name);
            }
        } finally {
            privatePool.shutdown();
        }
    }

    @Test
    @DisplayName("an ArrayList spliterator splits in half — the engine behind fork/join")
    void spliteratorHalvesTheSource() {
        List<Integer> accounts = IntStream.rangeClosed(1, 16).boxed().collect(Collectors.toList());
        Spliterator<Integer> left = accounts.spliterator();

        assertEquals(16, left.getExactSizeIfKnown());
        Spliterator<Integer> right = left.trySplit();
        assertNotNull(right, "ArrayList must be splittable");

        System.out.println("after trySplit(): left=" + left.getExactSizeIfKnown()
                + ", right=" + right.getExactSizeIfKnown());
        assertEquals(8, left.getExactSizeIfKnown());
        assertEquals(8, right.getExactSizeIfKnown());
        System.out.println("=> this halving, repeated recursively, IS the fork side of fork/join");
    }
}
