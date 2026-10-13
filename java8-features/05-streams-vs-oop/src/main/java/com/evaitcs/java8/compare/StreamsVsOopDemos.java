package com.evaitcs.java8.compare;

/**
 * Launcher for the whole module — runs every task demo in teaching order (easy -> hard),
 * then the wrap-up summary table.
 *
 *   .\mvnw.cmd -pl 05-streams-vs-oop compile exec:java
 *
 * To run a single task while pointing at it:
 *   .\mvnw.cmd -pl 05-streams-vs-oop compile exec:java "-Dexec.mainClass=com.evaitcs.java8.compare.TaskChain"
 */
public class StreamsVsOopDemos {

    public static void main(String[] args) {
        section("TaskFilter — deposits over 100: for+if+add vs filter+collect");
        TaskFilter.main(args);

        section("TaskTransform — names uppercased: for+add vs map+collect");
        TaskTransform.main(args);

        section("TaskSum — total of all amounts: accumulator vs reduce(ZERO, ::add)");
        TaskSum.main(args);

        section("TaskMax — largest balance: bestSoFar+null vs max() returning Optional");
        TaskMax.main(args);

        section("TaskCount — customers per city: containsKey/get/put vs collector");
        TaskCount.main(args);

        section("TaskChain — balances of customers under 30: nested loops vs flatMap");
        TaskChain.main(args);

        section("TaskAnyMatch — any transaction over 2000: early return vs anyMatch");
        TaskAnyMatch.main(args);

        section("ComparisonSummaryDemo — the concept-mapping table");
        ComparisonSummaryDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
        System.out.println();
    }
}
