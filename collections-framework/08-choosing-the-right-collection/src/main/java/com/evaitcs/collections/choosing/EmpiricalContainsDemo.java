package com.evaitcs.collections.choosing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Demo 6 — THE TABLE, MEASURED: contains() on 100,000 elements, ArrayList vs HashSet.
 *
 * The Big-O sheet says contains is O(n) for ArrayList and O(1) for HashSet. Big-O is a
 * growth claim, not a stopwatch claim — this demo puts a stopwatch on it anyway, once,
 * as evidence that the columns are not decoration. The experiment is chosen to be the
 * ArrayList's absolute worst case and the HashSet's everyday case: a key that is NOT in
 * the collection, so the list must scan all 100,000 slots (equals on every one) while the
 * hash set computes one hash, looks at one (empty) bucket, and answers.
 *
 * HOW TO READ THE NUMBERS — and how not to:
 *   - The absolute milliseconds are YOUR machine's: CPU, JIT warmup, garbage collection,
 *     what else is running. Run it twice, get different numbers. That is expected.
 *   - The RATIO is the lesson: three-plus orders of magnitude, and it GROWS with n —
 *     double the elements and the list scan doubles while the hash lookup stays flat.
 *   - This is why the whole course prints timings in DEMOS but never ASSERTS them in
 *     TESTS: a test that fails on a slow laptop teaches nothing. ChoosingTest asserts
 *     structure and contracts only; the stopwatch stays here.
 *   - Both collections are built with capacity hints (demo 1, banner 3) so we measure
 *     contains, not a surprise resize.
 */
public class EmpiricalContainsDemo {

    private static final int SIZE = 100_000;
    private static final int REPEATS = 500;
    /** Absent on purpose: forces the full O(n) scan on the list and a clean hash miss on the set. */
    private static final Integer NEEDLE = Integer.valueOf(SIZE);

    public static void main(String[] args) {
        banner("1. Build both containers — same 100,000 boxed Integers (note: all above 127, so no cache sharing — demo 4)");

        List<Integer> list = new ArrayList<Integer>(SIZE);                  // capacity hint: no grow-and-copy
        Set<Integer> set = new HashSet<Integer>((int) (SIZE / 0.75f) + 1);  // hint formula: no rehash (module 05)
        for (int i = 0; i < SIZE; i++) {
            Integer boxed = Integer.valueOf(i);
            list.add(boxed);
            set.add(boxed);
        }
        System.out.println("ArrayList size = " + list.size() + ", HashSet size = " + set.size());
        System.out.println("needle = " + NEEDLE + " — deliberately ABSENT from both (worst case for the scan).");

        banner("2. Warm up, then time " + REPEATS + " contains() calls on each");

        // Warmup: the JIT needs a few passes before numbers mean anything. Skipping it is
        // the classic benchmarking sin — the first calls run in the interpreter.
        for (int i = 0; i < REPEATS; i++) {
            list.contains(NEEDLE);
            set.contains(NEEDLE);
        }

        long listStart = System.nanoTime();
        for (int i = 0; i < REPEATS; i++) {
            list.contains(NEEDLE);                                          // O(n): 100,000 equals() per call
        }
        long listNanos = System.nanoTime() - listStart;

        long setStart = System.nanoTime();
        for (int i = 0; i < REPEATS; i++) {
            set.contains(NEEDLE);                                           // O(1): one hash, one bucket, done
        }
        long setNanos = System.nanoTime() - setStart;

        System.out.printf("ArrayList.contains x %d : %8.2f ms  (O(n) — scanned %d slots every time)%n",
                REPEATS, listNanos / 1_000_000.0, SIZE);
        System.out.printf("HashSet.contains   x %d : %8.2f ms  (O(1) — one hash miss every time)%n",
                REPEATS, setNanos / 1_000_000.0);
        System.out.printf("ratio: the list took ~%.0fx longer — and at n = 1,000,000 it would be ~10x THAT gap.%n",
                (double) listNanos / Math.max(setNanos, 1));
        System.out.println("CAVEAT, printed on purpose: your absolute numbers WILL differ (machine, JIT, load).");
        System.out.println("Demos print timings; TESTS never assert them — the ratio is the claim, not the milliseconds.");

        banner("3. What the table actually told you");

        System.out.println("contains-heavy work on big data rules out the O(n) rows BEFORE you write any code:");
        System.out.println("List.contains in a loop over m candidates is O(n*m) — quadratic surprises are");
        System.out.println("designed, not encountered. One Set built once (O(n)) turns it into O(n+m).");
        System.out.println("That single sentence is why 'use a Set for membership' is the most repeated");
        System.out.println("advice in this course — and why module 04 comes before module 08.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
