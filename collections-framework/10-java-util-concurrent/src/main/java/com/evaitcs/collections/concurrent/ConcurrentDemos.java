package com.evaitcs.collections.concurrent;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 10-java-util-concurrent compile exec:java
 *
 * The order is the argument: module 09 ended with "hold a global lock yourself" — correct,
 * and a throughput killer. This module answers its two open wounds first (the fail-fast
 * iterator that threw CME -> copy-on-write's frozen snapshot; the check-then-act lost
 * update -> ConcurrentHashMap's atomic compounds), then explains the null ban that makes
 * those guarantees unambiguous, then builds the real producer/consumer pipeline on a
 * BlockingQueue, separates the four blocking flavors, and closes with a tour of the rest
 * of the shelf plus the decision table to photograph.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 10-java-util-concurrent compile exec:java "-Dexec.mainClass=com.evaitcs.collections.concurrent.CopyOnWriteDemo"
 */
public class ConcurrentDemos {

    public static void main(String[] args) throws InterruptedException {
        section("CopyOnWriteDemo — the fail-safe iterator: frozen snapshots, no CME, and what they cost");
        CopyOnWriteDemo.main(args);

        section("ChmAtomicDemo — one bucket at a time: putIfAbsent, computeIfAbsent, replace — the lost update, deleted");
        ChmAtomicDemo.main(args);

        section("NullPolicyDemo — why ConcurrentHashMap refuses nulls: the ambiguity interleaving, scripted");
        NullPolicyDemo.main(args);

        section("ProducerConsumerDemo — BlockingQueue as the pattern: put/take, the poison pill, exact FIFO");
        ProducerConsumerDemo.main(args);

        section("BlockingFlavorsDemo — offer/poll vs put/take vs the timed pair, and proof take() really blocks");
        BlockingFlavorsDemo.main(args);

        section("ConcurrentTourDemo — LinkedQueue, SkipListMap, CopyOnWriteArraySet, PriorityBlockingQueue + the decision table");
        ConcurrentTourDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
