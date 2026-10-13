package com.evaitcs.corejava.builders;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 04-stringbuilder-stringbuffer compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 04-stringbuilder-stringbuffer compile exec:java "-Dexec.mainClass=com.evaitcs.corejava.builders.CapacityGrowthDemo"
 */
public class StringBuilderBufferDemos {

    public static void main(String[] args) throws Exception {
        section("WhyBuildersDemo — the += trap in five lines; same object, growing array");
        WhyBuildersDemo.main(args);

        section("BuilderApiTourDemo — the whole API you will touch, with the traps");
        BuilderApiTourDemo.main(args);

        section("CapacityGrowthDemo — length vs capacity, the 2n+2 rule, pre-sizing");
        CapacityGrowthDemo.main(args);

        section("BufferVsBuilderThreadDemo — the synchronized sibling, honestly measured");
        BufferVsBuilderThreadDemo.main(args);

        section("PerformanceDemo — timed three ways (printed, never asserted); when NOT to use a builder");
        PerformanceDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
