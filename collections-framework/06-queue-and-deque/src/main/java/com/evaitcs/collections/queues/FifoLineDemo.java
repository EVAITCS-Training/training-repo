package com.evaitcs.collections.queues;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Demo 2 — FIFO IN ACTION: the transaction processing line.
 *
 * A Queue is the data-structure version of one sentence: "process these IN THE ORDER
 * THEY ARRIVED." SecureBank's nightly job is the canonical shape — statements land in
 * a queue as they arrive, a worker drains them one by one, first in first out.
 *
 * The engine here is ArrayDeque: despite the name it implements Queue (via Deque),
 * backing itself with a GROWABLE ARRAY instead of LinkedList's chain of nodes —
 * fewer allocations, friendlier to the CPU cache, faster in practice (module 02
 * measured why nodes lose; module 08 puts it in the Big-O table).
 *
 * Note what the queue does NOT give you: no get(i), no contains-in-order, no sorting.
 * You may only look at the head and take from the head. Giving up random access is
 * the PRICE of the FIFO guarantee — interfaces document what you may do (module 01).
 */
public class FifoLineDemo {

    public static void main(String[] args) {
        banner("1. Arrival: offer() every statement line, in arrival order");

        // Declare with the INTERFACE (Queue), instantiate with the CLASS (ArrayDeque) —
        // the module-01 rule. If tonight's job later needs blocking hand-off between
        // threads, only the `new` line changes (to a BlockingQueue — module 10).
        Queue<Transaction> line = new ArrayDeque<Transaction>();
        for (Transaction t : Transaction.sampleStatement()) {
            line.offer(t);                          // enters at the TAIL
            System.out.println("offered " + t.getId() + " (" + t.getDescription() + ") — size = " + line.size());
        }

        banner("2. Processing: peek() looks, poll() takes — both from the HEAD");

        // peek() first: a real worker inspects the head before committing to it
        // (can we process this now, or must it wait?). peek() does NOT remove.
        Transaction head = line.peek();
        System.out.println("peek() sees the head WITHOUT removing: " + head.getId() + " — size still " + line.size());

        banner("3. The drain loop — poll() until null; the order IS the arrival order");

        Transaction current;
        int processed = 0;
        while ((current = line.poll()) != null) {   // poll() removes and returns the head
            processed++;
            System.out.println("  processed #" + processed + ": " + current);
        }
        System.out.println("drained " + processed + " lines; queue isEmpty() = " + line.isEmpty());
        System.out.println("poll() past the end returns " + line.poll() + " — the sentinel, never an exception.");

        banner("4. The guarantee, stated once: FIRST IN, FIRST OUT");

        Queue<Transaction> second = new ArrayDeque<Transaction>();
        second.offer(new Transaction("T-9001", "first in", new BigDecimal("1.00"), 1L));
        second.offer(new Transaction("T-9002", "second in", new BigDecimal("2.00"), 2L));
        System.out.println("poll #1 -> " + second.poll().getDescription() + "   (offered first, leaves first)");
        System.out.println("poll #2 -> " + second.poll().getDescription());
        System.out.println("A List could fake this with remove(0) — but on ArrayList that is an O(n) shift");
        System.out.println("EVERY time (module 02). ArrayDeque removes from the head in O(1). That is the");
        System.out.println("whole reason Queue exists as its own interface: the guarantee AND the speed.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
