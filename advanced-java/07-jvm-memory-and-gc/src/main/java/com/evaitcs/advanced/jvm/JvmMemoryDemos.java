package com.evaitcs.advanced.jvm;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 07-jvm-memory-and-gc compile exec:java
 *
 * The order is the argument: first MAP the runtime data areas with live MXBean numbers,
 * then spend a whole stack (StackOverflowError, counted), then prove the collector's
 * only question is reachability-from-roots (cycles die), then walk the reference-strength
 * ladder (strong/soft/weak/phantom + ReferenceQueue + WeakHashMap), then hunt the classic
 * leak (static registry, heap checkpoints printed), and finally meet the collectors
 * themselves (GarbageCollectorMXBeans + an allocation burst).
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 07-jvm-memory-and-gc compile exec:java "-Dexec.mainClass=com.evaitcs.advanced.jvm.ReachabilityCyclesDemo"
 *
 * Two commands that must run in a SEPARATE process (README documents both, tests never
 * use them): live GC logging  java -Xlog:gc -cp 07-jvm-memory-and-gc\target\classes com.evaitcs.advanced.jvm.GcTourDemo
 * and the 16 MB OutOfMemoryError repro  java -Xmx16m -cp 07-jvm-memory-and-gc\target\classes com.evaitcs.advanced.jvm.LeakHuntDemo
 */
public class JvmMemoryDemos {

    public static void main(String[] args) throws Exception {
        section("MemoryMapDemo — heap, stacks, metaspace, code cache: narrated and measured live");
        MemoryMapDemo.main(args);

        section("StackOverflowDemo — the frame budget spent to the last frame, depth counted");
        StackOverflowDemo.main(args);

        section("ReachabilityCyclesDemo — a rootless cycle is collected: tracing beats counting");
        ReachabilityCyclesDemo.main(args);

        section("ReferenceTypesDemo — strong/soft/weak/phantom, the ReferenceQueue, and a self-cleaning WeakHashMap");
        ReferenceTypesDemo.main(args);

        section("LeakHuntDemo — a static registry grows 100k entries: reachable but useless = leak");
        LeakHuntDemo.main(args);

        section("GcTourDemo — meet the collectors, fire an allocation burst, count the young GCs");
        GcTourDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
