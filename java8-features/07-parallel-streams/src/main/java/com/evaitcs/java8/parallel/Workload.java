package com.evaitcs.java8.parallel;

/**
 * The fake SecureBank workload used by every demo in this module.
 *
 * WHY a dedicated class: parallel-stream demos need CPU-BOUND, DETERMINISTIC busy-work.
 * If the per-element cost is too small, splitting overhead dominates and the "speedup"
 * demo embarrasses you live. If the data is random (new Random()), runs are not repeatable
 * on the projector. So: fixed pseudo-random balances + a fixed amount of real math per element.
 *
 * The story we tell students: every night the bank re-scores the risk of each account.
 * riskScore() is our stand-in for that calculation — deliberately heavy (a loop of sqrt)
 * so the timing difference between sequential and parallel is visible to the naked eye.
 */
public final class Workload {

    private Workload() { }

    /**
     * Deterministic "account balances" — no Random, so every run prints the same numbers.
     * A cheap integer hash spreads the values over a plausible-ish range.
     */
    public static double[] fakeBalances(int n) {
        double[] balances = new double[n];
        for (int i = 0; i < n; i++) {
            long mix = (i + 1L) * 2654435761L;          // Knuth multiplicative hash
            balances[i] = Math.abs(mix % 100_000L) + 0.5;
        }
        return balances;
    }

    /**
     * The per-element CPU work. ~20 sqrt calls each, so a few million elements
     * keep every core honestly busy for a measurable amount of time.
     *
     * IMPORTANT: this function is PURE — same input, same output, no shared state.
     * Purity is the price of admission for parallel streams (see SharedStateTrapDemo).
     */
    public static double riskScore(double balance) {
        double score = 0.0;
        for (int i = 1; i <= 20; i++) {
            score += Math.sqrt(balance * i + score);
        }
        return score;
    }

    /** Milliseconds from nanos, for readable printed output. */
    public static long ms(long nanos) {
        return nanos / 1_000_000L;
    }
}
