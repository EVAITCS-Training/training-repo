package com.evaitcs.java8.parallel;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Demo 1 — HOW a parallel stream actually works, before we ever trust it with money.
 *
 * THREE facts to say out loud while this runs:
 *
 *  1. The SOURCE is SPLIT recursively by a Spliterator (split + iterator: the object
 *     that chops a source into chunks) — the stream is chopped into
 *     smaller and smaller chunks until the pool decides the chunks are "small enough".
 *  2. Each chunk is processed by a worker thread from ForkJoinPool.commonPool().
 *     The common pool has (cores - 1) worker threads by default, because the calling
 *     thread ALSO helps — nobody sits idle.
 *  3. Partial results are COMBINED at the end. This is WHY module 06's collect()
 *     anatomy mattered: collect(supplier, accumulator, combiner) — in a SEQUENTIAL
 *     stream the combiner is never called; in a PARALLEL stream it is the whole game.
 *     A wrong combiner = a wrong answer, silently.
 */
public class HowParallelWorksDemo {

    public static void main(String[] args) {

        // ---- 1. The machine we are running on ----
        int cores = Runtime.getRuntime().availableProcessors();
        ForkJoinPool common = ForkJoinPool.commonPool();
        System.out.println("1. availableProcessors()          : " + cores);
        System.out.println("   commonPool parallelism          : " + common.getParallelism()
                + "   (usually cores-1: the CALLER thread joins in)");
        System.out.println("   commonPool toString             : " + common);

        // ---- 2. Watch the source get SPLIT (this is the Spliterator doing its job) ----
        // An ArrayList splits beautifully: trySplit() hands back exactly half, O(1).
        List<Integer> accounts = IntStream.rangeClosed(1, 16).boxed().collect(Collectors.toList());
        java.util.Spliterator<Integer> split = accounts.spliterator();
        System.out.println("\n2. splitting a 16-element ArrayList by hand:");
        System.out.println("   start           estimate=" + split.getExactSizeIfKnown());
        java.util.Spliterator<Integer> other = split.trySplit();
        if (other != null) {
            System.out.println("   after 1 trySplit: left=" + split.getExactSizeIfKnown()
                    + ", right=" + other.getExactSizeIfKnown());
        }

        // ---- 3. Which thread processes which element? (map + collect keeps it readable) ----
        // We ask each element to report the name of the thread that handled it.
        // Point at the output: multiple DIFFERENT worker names appear.
        List<String> who = IntStream.rangeClosed(1, 12).parallel()
                .mapToObj(i -> "acct-" + i + " on " + Thread.currentThread().getName())
                .collect(Collectors.toList());
        System.out.println("\n3. element -> thread (parallel, but collect() kept encounter order):");
        for (String line : who) {
            System.out.println("   " + line);
        }

        long distinctWorkers = who.stream()
                .map(s -> s.substring(s.indexOf(" on ") + 4))
                .distinct()
                .count();
        System.out.println("   => " + distinctWorkers + " distinct threads helped (incl. main: "
                + Thread.currentThread().getName() + ")");

        // ---- 4. Split-combine in one line: reduce SHOWS the combine step ----
        // Stream.reduce(identity, accumulator, COMBINER) — the 3rd arg only matters in parallel.
        // (Note: IntStream has no 3-arg reduce — box it, or use collect with a combiner.)
        int total = IntStream.rangeClosed(1, 16).parallel()
                .boxed()
                .reduce(0, (a, b) -> a + b, (left, right) -> left + right);
        System.out.println("\n4. reduce with an explicit combiner, parallel: " + total
                + "  (sequential would never call the combiner)");

        // ---- 5. What splits WELL vs BADLY (teaser for WhenParallelLosesDemo) ----
        // ArrayList splits in O(1): hand back half the backing array, done.
        // An iterator-based source cannot even tell you its SIZE without consuming it.
        java.util.Spliterator<Integer> unknown =
                java.util.Spliterators.spliteratorUnknownSize(accounts.iterator(), 0);
        System.out.println("\n5. spliterator estimates (exact + cheap to split = parallel-friendly):");
        System.out.println("   ArrayList        : " + accounts.spliterator().getExactSizeIfKnown());
        System.out.println("   iterator-based   : " + unknown.getExactSizeIfKnown()
                + "  (-1 = 'no idea how big I am' — cannot split smartly)");
        System.out.println("   (LinkedList knows its size but must WALK to the middle to split — also bad)");
    }
}
