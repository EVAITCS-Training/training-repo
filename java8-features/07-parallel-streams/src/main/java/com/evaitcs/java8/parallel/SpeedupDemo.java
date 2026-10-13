package com.evaitcs.java8.parallel;

import java.util.Arrays;

/**
 * Demo 2 — the ONE-WORD change, and an honest speedup.
 *
 * Three ways to ask for the same sum:
 *   Arrays.stream(balances)                      -> sequential
 *   Arrays.stream(balances).parallel()           -> parallel (added AFTER)
 *   Arrays.stream(balances, 0, n) ... same thing — .parallel() is a mode switch, not a new stream
 *
 * TEACHING TRAP #1 for the instructor: the JIT. The first time a loop runs it is
 * interpreted, then compiled, then optimized. If you time the FIRST run, "sequential"
 * may look SLOWER than it really is and your speedup is a lie. That is why this demo
 * runs a WARMUP round before the timed round. Never demo timings without warmup.
 *
 * TEACHING TRAP #2 for the instructor: NEVER promise a speedup in a live class.
 * Laptop cores, thermal throttling, and the OS scheduler all get a vote. We PRINT
 * both numbers and let the class react; the tests deliberately assert CORRECTNESS,
 * never timing.
 */
public class SpeedupDemo {

    /** 5 million accounts x ~20 sqrt each = enough real work that cores matter. */
    private static final int N = 5_000_000;

    public static void main(String[] args) {
        double[] balances = Workload.fakeBalances(N);
        System.out.println("portfolio size: " + N + " accounts, CPU-bound riskScore() each");
        System.out.println("common pool parallelism: " + java.util.concurrent.ForkJoinPool.commonPool().getParallelism());

        // ---- warmup: let the JIT compile riskScore() and the stream machinery ----
        System.out.print("\nwarming up (JIT)... ");
        timeSequential(balances);
        timeParallel(balances);
        System.out.println("done");

        // ---- timed round: the numbers you show the class (results kept for the check) ----
        long t0 = System.nanoTime();
        double seqResult = sumSequential(balances);
        long seq = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        double parResult = sumParallel(balances);
        long par = System.nanoTime() - t1;

        System.out.println();
        System.out.println("sequential : " + Workload.ms(seq) + " ms");
        System.out.println("parallel   : " + Workload.ms(par) + " ms");
        System.out.println("(printed, NOT asserted — timings vary per machine; results MUST match)");
        System.out.println();
        System.out.println("sequential total risk: " + seqResult);
        System.out.println("parallel   total risk: " + parResult);

        // The ONE claim we stand behind: same answer, both ways.
        // (Not bit-identical — floating point addition is not associative, and the
        //  parallel sum adds in a different ORDER. Equal within a tiny epsilon.)
        if (Math.abs(seqResult - parResult) < Math.abs(seqResult) * 1e-9 + 1e-6) {
            System.out.println("=> same total within floating-point tolerance (order of adds differs)");
        } else {
            throw new IllegalStateException("parallel and sequential disagreed — investigate!");
        }
    }

    // ---------- the one word is the difference between these two ----------

    static double sumSequential(double[] balances) {
        return Arrays.stream(balances)              // sequential by default
                .map(Workload::riskScore)
                .sum();
    }

    static double sumParallel(double[] balances) {
        return Arrays.stream(balances)
                .parallel()                         // <=== ONE WORD buys you threads
                .map(Workload::riskScore)
                .sum();
    }

    static long timeSequential(double[] balances) {
        long t0 = System.nanoTime();
        sumSequential(balances);
        return System.nanoTime() - t0;
    }

    static long timeParallel(double[] balances) {
        long t0 = System.nanoTime();
        sumParallel(balances);
        return System.nanoTime() - t0;
    }
}
