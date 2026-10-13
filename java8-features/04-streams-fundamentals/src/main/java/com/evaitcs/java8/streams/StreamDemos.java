package com.evaitcs.java8.streams;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 04-streams-fundamentals compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 04-streams-fundamentals compile exec:java "-Dexec.mainClass=com.evaitcs.java8.streams.LazinessDemo"
 */
public class StreamDemos {

    public static void main(String[] args) {
        section("StreamVsCollectionDemo — a stream COMPUTES, a collection STORES");
        StreamVsCollectionDemo.main(args);

        section("PipelineAnatomyDemo — source -> intermediate* -> terminal");
        PipelineAnatomyDemo.main(args);

        section("CreatingStreamsDemo — every source you'll actually use");
        CreatingStreamsDemo.main(args);

        section("LazinessDemo — nothing runs until the terminal op pulls");
        LazinessDemo.main(args);

        section("ShortCircuitDemo — infinite streams that terminate");
        ShortCircuitDemo.main(args);

        section("OneShotDemo — reuse a stream and watch it throw");
        OneShotDemo.main(args);

        section("OperationsTourDemo — the vocabulary of streams");
        OperationsTourDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
