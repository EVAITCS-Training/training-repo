package com.evaitcs.advanced.exceptions;

/**
 * Demo 6 — THE COST OF EXCEPTIONS: fillInStackTrace, and why control-flow-by-throw is slow.
 * (README "The cost of exceptions and fillInStackTrace".)
 *
 * Constructing an exception is NOT free: the constructor calls fillInStackTrace(), which
 * walks the current stack and stores a StackTraceElement[] snapshot. That is the expensive
 * part — not the throw, not the catch, but the STACK CAPTURE at construction.
 *
 * Consequences we prove here:
 *  1. A normal exception carries a non-empty trace; one built with writableStackTrace=false
 *     carries an EMPTY trace and skips the capture (getStackTrace().length == 0).
 *  2. Using exceptions for CONTROL FLOW (parseInt-by-throw vs a predicate check) pays that
 *     capture on every invalid input. We PRINT the two elapsed times as evidence but NEVER
 *     assert which is faster — timings are machine-dependent (repo determinism rule). We DO
 *     assert both approaches agree on every input.
 *  3. The legitimate use of writableStackTrace=false: a PRE-ALLOCATED control-flow exception
 *     thrown many times and never inspected, so skipping the capture is pure savings.
 */
public class ExceptionCostDemo {

    /** Pre-allocated, stack-free sentinel: reused on every "miss", safe because it has no trace to go stale. */
    private static final StacklessException NO_SUCH_ACCOUNT = new StacklessException("no such account");

    public static void main(String[] args) {
        banner("1. The cost is the STACK CAPTURE at construction (fillInStackTrace)");

        RuntimeException normal = new RuntimeException("normal");
        RuntimeException stackless = new StacklessException("stackless");
        System.out.println("step 1: new RuntimeException(...).getStackTrace().length = " + normal.getStackTrace().length);
        System.out.println("        -> fillInStackTrace() walked the stack and stored a snapshot (the expensive part).");
        System.out.println("step 2: new StacklessException(...).getStackTrace().length = " + stackless.getStackTrace().length);
        System.out.println("        -> built with writableStackTrace=false, so NO capture happened.");
        System.out.println("verdict: throw/catch are cheap; CAPTURING THE STACK is what costs. Skip it when no one reads it.");

        banner("2. ANTI-PATTERN — exceptions as control flow (parseInt-by-throw vs predicate)");

        String[] samples = { "42", "abc", "7", "12x", "1000", "", "-5", "99" };
        int iterations = 100_000;                 // enough to be visible, small enough to stay snappy
        int fallback = -1;

        // Both loops compute the SAME running total; we compare totals, never times.
        long totalByException = 0;
        long startEx = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (String s : samples) {
                totalByException += parseIntByException(s, fallback);
            }
        }
        long exMillis = (System.nanoTime() - startEx) / 1_000_000;

        long totalByPredicate = 0;
        long startPred = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (String s : samples) {
                totalByPredicate += parseIntByPredicate(s, fallback);
            }
        }
        long predMillis = (System.nanoTime() - startPred) / 1_000_000;

        System.out.println("step 1: parseInt-by-EXCEPTION over " + (iterations * samples.length)
                + " parses = " + exMillis + " ms   (printed, NEVER asserted — machine-dependent)");
        System.out.println("step 2: parseInt-by-PREDICATE over the same         = " + predMillis + " ms   (printed, NEVER asserted)");
        System.out.println("step 3: totals equal? " + (totalByException == totalByPredicate)
                + "  (exception=" + totalByException + ", predicate=" + totalByPredicate + ")");
        System.out.println("verdict: half the samples are invalid, so the exception path pays throw+catch+STACK CAPTURE");
        System.out.println("        on each one. The predicate path pays a few character comparisons. Exceptions are for");
        System.out.println("        EXCEPTIONAL cases — a wrong answer is a fact to CHECK, not a disaster to throw.");

        banner("3. The legit trick — a PRE-ALLOCATED, stack-free control-flow exception");

        // Because NO_SUCH_ACCOUNT has writableStackTrace=false, reusing the SAME instance over
        // and over is safe (there is no per-throw stack snapshot to go stale) and cheap.
        int misses = 0;
        for (int i = 0; i < 3; i++) {
            try {
                throw NO_SUCH_ACCOUNT;                 // reusing one pre-built, stack-free instance
            } catch (StacklessException e) {
                misses++;
            }
        }
        System.out.println("step 1: reused ONE static StacklessException " + misses + " times; its trace length stays "
                + NO_SUCH_ACCOUNT.getStackTrace().length + ".");
        System.out.println("step 2: no per-throw capture, no allocation — the signal is cheap BECAUSE it carries no stack.");
        System.out.println("verdict: if you must signal with an exception on a hot path, pre-allocate it with");
        System.out.println("        writableStackTrace=false. Better still: don't use an exception for control flow at all.");

        banner("4. In one sentence");
        System.out.println("An exception's price is the stack it captures at construction; use exceptions for the");
        System.out.println("exceptional, check ordinary outcomes with predicates, and skip the capture when no one reads it.");
    }

    /** ANTI-PATTERN: relies on NumberFormatException for the invalid case — pays a capture each time. */
    static int parseIntByException(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * The cheap path: validate the characters, return the fallback on anything non-numeric.
     * For this demo's in-range samples the final parseInt never throws; a production version
     * would also guard overflow. No exception is created on the common (invalid) path.
     */
    static int parseIntByPredicate(String s, int fallback) {
        if (s == null || s.isEmpty()) {
            return fallback;
        }
        int i = (s.charAt(0) == '-' || s.charAt(0) == '+') ? 1 : 0;
        if (i == s.length()) {
            return fallback;                          // a lone sign is not a number
        }
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return fallback;                       // non-digit: decide WITHOUT throwing
            }
        }
        return Integer.parseInt(s);
    }

    /**
     * A RuntimeException built with writableStackTrace=false: constructing it does NOT capture
     * the stack, so getStackTrace() is empty and repeated throws are cheap. This is the trick
     * behind pre-allocated control-flow signals.
     */
    static final class StacklessException extends RuntimeException {
        StacklessException(String message) {
            // (message, cause, suppression=false, writableStackTrace=false)
            super(message, null, false, false);
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
