package com.evaitcs.java8.parallel;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Demo 6 — the common pool is JVM-WIDE, and the custom-pool escape hatch.
 *
 * ForkJoinPool.commonPool() is a SINGLE pool shared by:
 *   - every parallel stream in the JVM (yours AND every library you depend on)
 *   - CompletableFuture.supplyAsync/runAsync when no executor is passed
 *
 * So one blocking or runaway task hurts EVERYONE. If a library hogs the pool,
 * your "parallel" stream quietly degrades toward sequential.
 *
 * THE TRICK: submit your parallel stream to a ForkJoinPool YOU own. When a parallel
 * stream runs inside a ForkJoinPool task, it uses THAT pool instead of the common one.
 * (This behavior is a widely used implementation characteristic of the JDK, not a
 * documented guarantee — mention that caveat to the class. For blocking work, the
 * cleaner answer is a plain ExecutorService.)
 */
public class CustomPoolDemo {

    public static void main(String[] args) throws Exception {

        // ---- 1. Default: work lands on the common pool ----
        List<String> commonThreads = IntStream.rangeClosed(1, 8).parallel()
                .mapToObj(i -> Thread.currentThread().getName())
                .collect(Collectors.toList());
        System.out.println("1. plain parallel stream runs on:");
        commonThreads.forEach(t -> System.out.println("   " + t));

        // ---- 2. Our own pool: a bounded blast radius ----
        // Size it deliberately: 2 workers, so a misbehaving task can only ever
        // tie up OUR two threads — the rest of the JVM keeps its common pool.
        ForkJoinPool privatePool = new ForkJoinPool(2);
        try {
            List<String> privateThreads = privatePool.submit(() ->
                    IntStream.rangeClosed(1, 8).parallel()
                            .mapToObj(i -> Thread.currentThread().getName())
                            .collect(Collectors.toList())
            ).get();   // get() rethrows whatever happened inside the task

            System.out.println("\n2. submitted into a private ForkJoinPool(2):");
            privateThreads.forEach(t -> System.out.println("   " + t));

            boolean escaped = privateThreads.stream().noneMatch(t -> t.contains("commonPool"));
            System.out.println("   => ran entirely off the common pool? " + escaped);
        } finally {
            privatePool.shutdown();   // pools don't die on their own — always shut them down
        }

        System.out.println("\nRULE OF THUMB (the slide to end on):");
        System.out.println("  parallel streams are for CPU-BOUND, DATA-PARALLEL,");
        System.out.println("  NO-SHARED-STATE, BIG-ENOUGH workloads.");
        System.out.println("  When in doubt: sequential. And then MEASURE.");
    }
}
