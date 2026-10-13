package com.evaitcs.java8.parallel;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 07-parallel-streams compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 07-parallel-streams compile exec:java "-Dexec.mainClass=com.evaitcs.java8.parallel.SpeedupDemo"
 */
public class ParallelStreamDemos {

    public static void main(String[] args) throws Exception {
        section("HowParallelWorksDemo — split, process on the common pool, combine");
        HowParallelWorksDemo.main(args);

        section("SpeedupDemo — the ONE-WORD change (.parallel()), warmup + timed");
        SpeedupDemo.main(args);

        section("SharedStateTrapDemo — TRAP #1: shared mutable state loses data");
        SharedStateTrapDemo.main(args);

        section("OrderingDemo — TRAP #2: encounter order vs forEach chaos");
        OrderingDemo.main(args);

        section("WhenParallelLosesDemo — TRAP #3: tiny data, boxing, bad splitters, blocking");
        WhenParallelLosesDemo.main(args);

        section("CustomPoolDemo — the common pool is JVM-wide; the private-pool escape hatch");
        CustomPoolDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
