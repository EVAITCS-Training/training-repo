package com.evaitcs.java8.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Demo 5 — TRAP #3: when .parallel() makes things SLOWER (or dangerous).
 *
 * Four classic losers, in the order we show them:
 *
 *  1. TINY DATA — splitting the work and recombining it costs more than doing the work.
 *  2. BOXED PRIMITIVES — Stream<Integer> allocates an object per element and can't use
 *     CPU registers the way IntStream does. Boxing kills more speedups than bad hardware.
 *  3. DATA STRUCTURES THAT SPLIT BADLY — LinkedList and iterator-based sources must be
 *     WALKED to be divided; ArrayList, arrays and IntStream.range split in O(1).
 *  4. BLOCKING / IO-BOUND WORK — a blocked worker parks a thread of the SHARED common
 *     pool. Your "fast" API calls quietly starve every other parallel stream in the JVM.
 *
 * Timings are PRINTED, never asserted — a slow classroom laptop must not fail a test.
 */
public class WhenParallelLosesDemo {

    public static void main(String[] args) {

        // ---- 1. Tiny data: overhead of splitting > the work itself ----
        // (printed in MICROseconds — milliseconds are too coarse to show the overhead)
        double[] tiny = Workload.fakeBalances(100);
        time(() -> sumRisk(Arrays.stream(tiny)));                          // warmup
        time(() -> sumRisk(Arrays.stream(tiny).parallel()));               // warmup
        long seqTiny = time(() -> sumRisk(Arrays.stream(tiny)));
        long parTiny = time(() -> sumRisk(Arrays.stream(tiny).parallel()));
        System.out.println("1. TINY DATA (100 accounts)");
        System.out.println("   sequential: " + (seqTiny / 1000) + " us   parallel: " + (parTiny / 1000) + " us");
        System.out.println("   => on most machines parallel does NOT win. Splitting 100 items across");
        System.out.println("      threads costs more than scoring them on one core.\n");

        // ---- 2. Boxed primitives: Stream<Integer> vs IntStream, 10M elements ----
        final int big = 10_000_000;
        long primSeq = time(() -> IntStream.range(0, big).sum());
        long primPar = time(() -> IntStream.range(0, big).parallel().sum());
        long boxSeq  = time(() -> sumBoxed(IntStream.range(0, big).boxed()));
        long boxPar  = time(() -> sumBoxed(IntStream.range(0, big).parallel().boxed()));
        System.out.println("2. BOXING (sum of 10,000,000 ints)");
        System.out.println("   IntStream        sequential: " + Workload.ms(primSeq) + " ms   parallel: " + Workload.ms(primPar) + " ms");
        System.out.println("   Stream<Integer>  sequential: " + Workload.ms(boxSeq) + " ms   parallel: " + Workload.ms(boxPar) + " ms");
        System.out.println("   => read the SEQUENTIAL column — that is the pure boxing tax: 10M Integer");
        System.out.println("      objects allocated (and garbage-collected) instead of raw ints. Parallel");
        System.out.println("      can throw cores at it, but you still pay in memory traffic and GC.");
        System.out.println("      The fix is not threads, it is IntStream / mapToInt / mapToLong.\n");

        // ---- 3. Splitting quality: LinkedList vs ArrayList ----
        // The honest measurement is the SPLIT ITSELF (that is the cost parallel adds).
        // Total-sum timings depend on the machine, so we print them as context only.
        final int n = 1_000_000;
        List<Integer> arrayList = new ArrayList<>(n);
        LinkedList<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }
        long[] chunks = new long[1];
        long arrSplit  = time(() -> chunks[0] = splitAll(arrayList.spliterator()));
        long linkSplit = time(() -> chunks[0] = splitAll(linkedList.spliterator()));
        System.out.println("3. SPLITTING QUALITY (recursively split 1,000,000 elements — the cost parallel ADDS)");
        System.out.println("   ArrayList  fully split in : " + Workload.ms(arrSplit) + " ms   (chunks=" + chunks[0] + ")");
        System.out.println("   LinkedList fully split in : " + Workload.ms(linkSplit) + " ms   <== must WALK nodes to find each midpoint");
        System.out.println("   => same elements, same math — the DATA STRUCTURE decides how cheaply work");
        System.out.println("      can be divided. (IntStream.range is even better: no data at all, just numbers.)");
        System.out.println("      This is the split side of the fork/join diagram in the README.\n");

        // ---- 4. Blocking work on the shared common pool ----
        // Each "API call" sleeps 50ms. Watch: the calls finish quickly for US...
        List<Integer> calls = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        long blocked = time(() -> calls.parallelStream().forEach(WhenParallelLosesDemo::fakeApiCall));
        System.out.println("4. BLOCKING WORK (8 'API calls' x 50 ms sleep, parallel)");
        System.out.println("   total wall time: " + Workload.ms(blocked) + " ms");
        System.out.println("   => fast for US — but we just parked common-pool workers in Thread.sleep.");
        System.out.println("      Every OTHER parallel stream and CompletableFuture in this JVM was");
        System.out.println("      waiting behind our naps. Use an ExecutorService for blocking work,");
        System.out.println("      or the custom-pool trick from CustomPoolDemo.");
    }

    private static double sumRisk(java.util.stream.DoubleStream stream) {
        return stream.map(Workload::riskScore).sum();
    }

    /**
     * Recursively split a spliterator until it cannot be split; return the number of
     * leaf chunks. This isolates the pure SPLITTING cost — the overhead parallel adds
     * before a single element is processed. ArrayList splits by halving an index range
     * (O(1) per split); LinkedList must WALK to each midpoint (O(n) per split).
     */
    private static long splitAll(java.util.Spliterator<?> sp) {
        java.util.Spliterator<?> half = sp.trySplit();
        if (half == null) {
            return 1;
        }
        return splitAll(half) + splitAll(sp);
    }

    private static long sumBoxed(Stream<Integer> stream) {
        return stream.mapToLong(Integer::longValue).sum();
    }

    private static void fakeApiCall(int id) {
        try {
            Thread.sleep(50); // pretend network latency to the credit bureau
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static long time(Runnable r) {
        long t0 = System.nanoTime();
        r.run();
        return System.nanoTime() - t0;
    }
}
