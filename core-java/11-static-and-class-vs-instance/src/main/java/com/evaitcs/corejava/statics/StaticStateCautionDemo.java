package com.evaitcs.corejava.statics;

/**
 * Demo 5 - the caution section: STATIC MUTABLE STATE IS GLOBAL STATE.
 *
 * Two "unrelated" features (checkout, reporting) both run through Configuration's
 * one static map. Neither method's signature admits the coupling - that is what
 * makes the real-world bug so expensive: tests pass alone, fail together; a write
 * here silently changes behavior over there; every thread sees every write.
 *
 * The forward pointer: the Stream API BANS stateful lambdas (a lambda that mutates
 * captured/shared state) for exactly this reason - see java8-features/07-parallel-streams,
 * where shared mutable state stops being a smell and starts being a data race.
 */
public class StaticStateCautionDemo {

    public static void main(String[] args) {

        banner("1. Two UNRELATED features, one global map");
        Configuration.reset(); // notice: we already need an apology method
        checkoutFeature();     // "one part of the app"...
        reportFeature();       // ..."a completely different part" - and it SEES checkout's write

        banner("2. Swap the order - the classic flaky-test bug, live");
        Configuration.reset();
        reportFeature();       // report first this time
        checkoutFeature();
        System.out.println("Same two calls, different order, different behavior.");
        System.out.println("THIS is how 'passes alone, fails together' happens: every test");
        System.out.println("leaves a write behind in the global, and the next test inherits it.");
        System.out.println("Nothing in either method's SIGNATURE admits Configuration exists -");
        System.out.println("the coupling is invisible at both call sites. And with threads it");
        System.out.println("gets worse: one shared HashMap, many writers, no synchronization.");

        banner("3. The rules");
        System.out.println("1. PREFER static FINAL IMMUTABLE data (BankUtils.CURRENCY_CODE).");
        System.out.println("2. If a mutable static is unavoidable: ENCAPSULATE it behind methods,");
        System.out.println("   SYNCHRONIZE access, and DOCUMENT that it is process-wide state.");
        System.out.println("3. Give tests a reset() hook - then ask if the global was worth it.");
        System.out.println("(One-line forward note: java.util.stream forbids STATEFUL lambdas for");
        System.out.println("the same principle - java8-features/07-parallel-streams.)");
        Configuration.reset(); // leave the JVM as we found it
    }

    /** "Feature A" - writes the global. Its signature shows no dependency at all. */
    private static void checkoutFeature() {
        Configuration.set("audit.mode", "verbose"); // hidden global write
        System.out.println("checkout: set audit.mode=verbose (a 'local' decision, globally visible)");
    }

    /** "Feature B" - reads the global. Equally innocent-looking, equally coupled. */
    private static void reportFeature() {
        String mode = Configuration.get("audit.mode");
        System.out.println("report:   audit.mode -> " + (mode == null ? "null (clean)" : mode + "  <- LEAKED from checkout!"));
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
