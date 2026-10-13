package com.evaitcs.collections.queues;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 06-queue-and-deque compile exec:java
 *
 * The order is the argument: first the two API flavors every Queue operation comes in
 * (throw vs special-value — and the null-ban consequence), then FIFO as a real job
 * (the transaction line), then the PriorityQueue trap (a heap is NOT a sorted list),
 * then the Deque's two careers (queue and stack), and finally why the legacy Stack is
 * the wrong tool even though it works.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 06-queue-and-deque compile exec:java "-Dexec.mainClass=com.evaitcs.collections.queues.PriorityQueueDemo"
 */
public class QueueDemos {

    public static void main(String[] args) {
        section("QueueTwoFlavorsDemo — add/remove/element throw; offer/poll/peek return special values; nulls banned");
        QueueTwoFlavorsDemo.main(args);

        section("FifoLineDemo — a transaction processing line: offer at the tail, poll from the head");
        FifoLineDemo.main(args);

        section("PriorityQueueDemo — a binary heap, not a sorted list: peek/poll give the min, for-each does not");
        PriorityQueueDemo.main(args);

        section("DequeDemo — two-ended queue: FIFO career (offer/poll) and LIFO career (push/pop)");
        DequeDemo.main(args);

        section("StackVsArrayDequeDemo — the legacy Stack extends Vector; ArrayDeque is the recommendation");
        StackVsArrayDequeDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
