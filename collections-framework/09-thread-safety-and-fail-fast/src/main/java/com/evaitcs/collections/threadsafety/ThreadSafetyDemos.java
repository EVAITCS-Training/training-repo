package com.evaitcs.collections.threadsafety;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 09-thread-safety-and-fail-fast compile exec:java
 *
 * The order is the argument: first the RAW race (failure mode #1 — corrupted state, and
 * why "not synchronized" is a design decision you must know about), then the CME machinery
 * (failure mode #2 — modCount under the hood of module 03's remove() rule), then the fix
 * ladder and its two traps (wrappers lock CALLS, not SEQUENCES — iteration and compound
 * actions stay broken), and finally the 1997 answer ("but Vector IS synchronized!") which
 * turns out to be the same granularity with the same traps. Module 10 replaces the top
 * rung of the ladder: engines with concurrency built into their structure.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 09-thread-safety-and-fail-fast compile exec:java "-Dexec.mainClass=com.evaitcs.collections.threadsafety.RaceCorruptionDemo"
 */
public class ThreadSafetyDemos {

    public static void main(String[] args) throws InterruptedException {
        section("RaceCorruptionDemo — failure mode #1: the plain-ArrayList race, and the synchronizedList fix");
        RaceCorruptionDemo.main(args);

        section("ModCountAnatomyDemo — failure mode #2: modCount vs expectedModCount, the escape, and set()");
        ModCountAnatomyDemo.main(args);

        section("WrapperIsNotEnoughDemo — fix levels (b) and (c): manual locks, wrappers, and the iteration trap");
        WrapperIsNotEnoughDemo.main(args);

        section("CheckThenActDemo — the lost update no wrapper can fix, scripted; module 10's atomic answer");
        CheckThenActDemo.main(args);

        section("VectorRevisitedDemo — legacy method-level locks: exact adds, broken compounds, fail-fast iterators");
        VectorRevisitedDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
