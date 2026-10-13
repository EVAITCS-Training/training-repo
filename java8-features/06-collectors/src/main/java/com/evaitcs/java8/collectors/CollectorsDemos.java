package com.evaitcs.java8.collectors;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 06-collectors compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 06-collectors compile exec:java "-Dexec.mainClass=com.evaitcs.java8.collectors.GroupingByDemo"
 */
public class CollectorsDemos {

    public static void main(String[] args) {
        section("CollectAnatomyDemo — collect() is a mutable reduction");
        CollectAnatomyDemo.main(args);

        section("ToMapAndItsTrapDemo — lookup tables and the duplicate-key explosion");
        ToMapAndItsTrapDemo.main(args);

        section("SummarizingDemo — counting, summing, averaging, summarizing (and BigDecimal)");
        SummarizingDemo.main(args);

        section("GroupingByDemo — SQL GROUP BY in one line (module 05's promise kept)");
        GroupingByDemo.main(args);

        section("PartitioningByDemo — the boolean special case with two guaranteed buckets");
        PartitioningByDemo.main(args);

        section("JoiningDemo — glue, statement lines, and collectingAndThen finishes");
        JoiningDemo.main(args);

        section("ReduceVsCollectDemo — immutable fold vs mutable accumulation");
        ReduceVsCollectDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
