package com.evaitcs.java8.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Demo 3 — TRAP #1: shared mutable state. The bug that keeps a size, then disappears.
 *
 * THE SENTENCE TO SAY IN CLASS:
 *   "Parallel streams don't make unsafe code safe — they make unsafe code INTERMITTENT,
 *    which is the worst kind of bug: it passes in dev, fails in prod, once a quarter."
 *
 * The wrong way: several worker threads call list.add(...) on ONE plain ArrayList.
 * ArrayList is not thread-safe. Under a race you can get:
 *   - LOST UPDATES      (two threads write the same slot; size ends up too small)
 *   - null holes        (a slot was reserved but never filled)
 *   - ArrayIndexOutOfBounds / IndexOutOfBounds (internal array resized mid-add)
 * And crucially: on any given run you might get LUCKY and see the right answer.
 * That is exactly why this is a trap and not a plain old bug.
 */
public class SharedStateTrapDemo {

    private static final int N = 200_000;

    public static void main(String[] args) {
        System.out.println("collecting " + N + " flagged accounts into a List...\n");

        int broken = brokenWay(N);
        System.out.println("WRONG  forEach + shared list.add : " + describe(broken, N));

        // The RIGHT ways — note none of them share mutable state between threads:
        int rightA = collectWay(N);
        System.out.println("RIGHT  #1 map(...).collect(toList()) : " + describe(rightA, N));

        int rightB = sequentialWay(N);
        System.out.println("RIGHT  #2 .sequential() + list.add   : " + describe(rightB, N));

        int rightC = countingWay(N);
        System.out.println("RIGHT  #3 counting collector         : " + describe(rightC, N));

        System.out.println("\n=> collect() is safe because EACH worker thread accumulates into");
        System.out.println("   its OWN container, and the combiner MERGES them (module 06 again!).");
        System.out.println("   No sharing, no race, no intermittent 3 a.m. pages.");
    }

    /**
     * WRONG: parallel forEach mutating one shared ArrayList.
     * Returns however many elements survived; the stream may also blow up,
     * which we CATCH and count as "lost" so the demo never crashes the launcher.
     * Package-visible so the test can reuse it as printed evidence.
     */
    static int brokenWay(int n) {
        List<Integer> flagged = new ArrayList<>();
        try {
            IntStream.rangeClosed(1, n).parallel()
                    .filter(i -> i % 100 == 0)     // ~1% flagged
                    .forEach(flagged::add);        // <== SHARED MUTABLE STATE. Never do this.
        } catch (RuntimeException e) {
            System.out.println("   (caught " + e.getClass().getSimpleName()
                    + " mid-add — ArrayList internals corrupted by concurrent writes)");
        }
        long nulls = flagged.stream().filter(x -> x == null).count();
        if (nulls > 0) {
            System.out.println("   (" + nulls + " null holes: slots reserved by one thread, never filled)");
        }
        return flagged.size();
    }

    static int expectedFlagged(int n) {
        return n / 100;
    }

    /** RIGHT #1: collect() — each thread gets its own container, combiner merges. */
    static int collectWay(int n) {
        return IntStream.rangeClosed(1, n).parallel()
                .filter(i -> i % 100 == 0)
                .mapToObj(Integer::valueOf)
                .collect(Collectors.toList())
                .size();
    }

    /** RIGHT #2: if you MUST mutate a shared list, go back to .sequential(). */
    static int sequentialWay(int n) {
        List<Integer> flagged = new ArrayList<>();
        IntStream.rangeClosed(1, n)
                .filter(i -> i % 100 == 0)
                .forEach(flagged::add);            // safe: ONE thread, in order
        return flagged.size();
    }

    /** RIGHT #3: often you don't need the list at all — count with a collector. */
    static int countingWay(int n) {
        return (int) IntStream.rangeClosed(1, n).parallel()
                .filter(i -> i % 100 == 0)
                .count();
    }

    private static String describe(int actual, int n) {
        int expected = expectedFlagged(n);
        return actual + " of " + expected + " expected"
                + (actual == expected ? "  (correct)" : "  <== LOST " + (expected - actual) + "!");
    }
}
