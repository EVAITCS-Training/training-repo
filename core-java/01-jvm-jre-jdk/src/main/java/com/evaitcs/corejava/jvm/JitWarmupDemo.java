package com.evaitcs.corejava.jvm;

/**
 * Demo 3 — the JIT: bytecode is interpreted first, hot paths get compiled to native code.
 *
 * "Write once, run anywhere" has a price: the JVM starts by INTERPRETING bytecode
 * (portable, immediately runnable, but slow). While your program runs, the Just-In-Time
 * compiler watches for HOT methods and translates them to native machine code.
 *
 * HOW WE MAKE THE GAP VISIBLE: the same little loop is run several times as separate rounds.
 * Round 1 is the FIRST time the JVM ever sees it, so it is interpreted (slow). By round 2-3
 * the JIT has compiled it, so the later rounds are native speed. We keep each round small
 * (near the JVM's compile threshold) on purpose: if one loop ran for hundreds of millions of
 * iterations, the JIT would compile it mid-flight ("on-stack replacement") and even round 1
 * would look fast — hiding the very effect we want students to see.
 *
 * Print-only on purpose: timings vary between machines, so the TESTS never assert on time.
 */
public class JitWarmupDemo {

    // Small enough that round 1 is mostly interpreted; the story is the RATIO round1 : later.
    private static final int ITERATIONS = 30_000;
    private static final int ROUNDS = 6;

    public static void main(String[] args) {

        banner("Same loop, " + ROUNDS + " separate rounds — BankApp.countCents(3, " + ITERATIONS + ")");

        long firstResult = 0;
        double round1Micros = 0;
        double hotMicros = Double.MAX_VALUE;

        for (int round = 1; round <= ROUNDS; round++) {
            long start = System.nanoTime();
            long result = BankApp.countCents(3, ITERATIONS);
            double micros = (System.nanoTime() - start) / 1000.0;

            // Same input => same answer EVERY round. Optimization may change the speed,
            // never the result — if this ever trips, the JVM has a bug, not our demo.
            if (round == 1) {
                firstResult = result;
                round1Micros = micros;
            } else {
                if (result != firstResult) {
                    throw new IllegalStateException("JIT changed the answer?! " + result + " vs " + firstResult);
                }
                // track the fastest hot round as the "compiled" reference
                if (round >= 3) {
                    hotMicros = Math.min(hotMicros, micros);
                }
            }

            String phase = (round == 1) ? "  <- interpreted (first time the JVM sees this loop)"
                    : (round == 2 ? "  <- JIT compiling..." : "  <- native (compiled) code");
            System.out.printf("  round %d: %,9.1f us   (checksum %,d)%s%n", round, micros, result, phase);
        }

        System.out.println();
        System.out.printf("  Round 1 (interpreted) was %.1fx slower than the fastest compiled round (%.1f us vs %.1f us).%n",
                round1Micros / Math.max(hotMicros, 1e-9), round1Micros, hotMicros);
        System.out.println("  Consequences to remember:");
        System.out.println("   - microbenchmarks MUST warm up first, or they measure the interpreter;");
        System.out.println("   - 'Java is slow at startup' is interpreter + class loading, not the platform;");
        System.out.println("   - the JIT may not change what code DOES, only how fast it does it.");
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
