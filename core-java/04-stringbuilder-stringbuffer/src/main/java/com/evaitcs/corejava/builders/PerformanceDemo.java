package com.evaitcs.corejava.builders;

/**
 * The performance point, MEASURED — but printed, never asserted.
 *
 * Why no timing assertions in tests: the JIT needs warmup before numbers mean
 * anything (module 01 showed the interpreter -> compiled switch), GC pauses
 * land at random, and machine load varies. A timing test that passes on your
 * laptop fails on CI on a bad morning. Tests here assert STRUCTURE (lengths,
 * capacities, identities); timings are demo material only.
 */
public class PerformanceDemo {

    public static void main(String[] args) {

        banner("1. Building one giant statement line, three ways");
        // += is kept at 20k parts on purpose: it is O(n^2), so 100k would take MINUTES.
        // The builders happily do 100k in milliseconds — that gap IS the lesson.
        final int concatParts = 20_000;
        final int builderParts = 100_000;

        long t0 = System.nanoTime();
        String concat = "";
        for (int i = 0; i < concatParts; i++) {
            concat += i % 10;                    // a brand-new String (and full re-copy) per iteration
        }
        long concatMs = (System.nanoTime() - t0) / 1_000_000;

        t0 = System.nanoTime();
        StringBuilder defaulted = new StringBuilder();
        for (int i = 0; i < builderParts; i++) {
            defaulted.append(i % 10);            // ~13 grow-and-copies total, amortized away
        }
        String defaultResult = defaulted.toString();
        long defaultMs = (System.nanoTime() - t0) / 1_000_000;

        t0 = System.nanoTime();
        StringBuilder presized = new StringBuilder(builderParts);
        for (int i = 0; i < builderParts; i++) {
            presized.append(i % 10);             // zero growths — the array already fits
        }
        String presizedResult = presized.toString();
        long presizedMs = (System.nanoTime() - t0) / 1_000_000;

        System.out.println("approach                        parts     ms   result chars");
        System.out.printf ("+= String concat (O(n^2))     %7d %6d %10d%n", concatParts, concatMs, concat.length());
        System.out.printf ("StringBuilder default         %7d %6d %10d%n", builderParts, defaultMs, defaultResult.length());
        System.out.printf ("StringBuilder pre-sized       %7d %6d %10d%n", builderParts, presizedMs, presizedResult.length());
        System.out.println();
        System.out.println("Read the first two rows: concat did FIVE TIMES LESS work and took");
        System.out.println("far longer — scale it to 100k parts and += would run for minutes.");
        System.out.println("Default vs pre-sized is a smaller, real win: ~13 allocate-and-copy");
        System.out.println("rounds vs zero. (Numbers vary per machine — that's why we never assert them.)");

        banner("2. When you DON'T need a builder");
        String account = "ACC-1042";
        String oneLiner = "Audit: " + account + " frozen";                    // single expression
        StringBuilder manual = new StringBuilder("Audit: ").append(account).append(" frozen");
        System.out.println("one-liner  : " + oneLiner);
        System.out.println("hand-built : " + manual);
        System.out.println("equal? " + oneLiner.equals(manual.toString()));
        System.out.println();
        System.out.println("The secret: a SINGLE '+' expression already compiles to a StringBuilder");
        System.out.println("(javap -c shows new StringBuilder / append / append / toString). Writing");
        System.out.println("the builder by hand for one expression is ceremony, not speed.");
        System.out.println("Rule: '+' in one expression = fine.  '+' inside a LOOP = the trap.");
        System.out.println("String.format for a readable one-liner is the same story: clarity wins.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
