package com.evaitcs.advanced.threads;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 04-threads-and-jmm compile exec:java
 *
 * The order is the argument: first what a thread IS and the six states it lives in
 * (Lifecycle), then Java 21's cheap virtual threads and their carriers (VirtualThreads),
 * then the memory-model DANGER — visibility is not automatic (Visibility), then the FIX —
 * happens-before edges and safe publication (HappensBefore), and finally the one signal
 * every blocking call obeys, interruption as cooperation (Interrupt).
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 04-threads-and-jmm compile exec:java "-Dexec.mainClass=com.evaitcs.advanced.threads.VisibilityDemo"
 */
public class ThreadModelDemos {

    public static void main(String[] args) throws Exception {
        section("LifecycleDemo — the six Thread.State values, caught deterministically");
        LifecycleDemo.main(args);

        section("VirtualThreadsDemo — 10,000 virtual threads sleep on a handful of carriers");
        VirtualThreadsDemo.main(args);

        section("VisibilityDemo — the spin-flag: plain may hang forever, volatile always stops");
        VisibilityDemo.main(args);

        section("HappensBeforeDemo — join() publishes plain fields; the five HB rules; safe records");
        HappensBeforeDemo.main(args);

        section("InterruptDemo — interruption is a flag, a thrown exception clears it, re-interrupt restores it");
        InterruptDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
