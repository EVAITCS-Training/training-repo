package com.evaitcs.collections.queues;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Demo 3 — PRIORITYQUEUE: a heap, NOT a sorted list. THE trap of this module.
 *
 * A PriorityQueue is a BINARY HEAP wearing the Queue interface: a complete binary tree
 * stored in a plain array where every parent is <= its children (a "min-heap"). That one
 * shape buys the whole contract:
 *
 *   offer(e)  -> O(log n)   (put at the end of the array, "sift up" while smaller than parent)
 *   poll()    -> O(log n)   (take the root, move the last element to the root, "sift down")
 *   peek()    -> O(1)       (the root IS the minimum — that is the only invariant)
 *   contains / remove(Object) -> O(n)  (no index: the heap only knows the TOP, so it scans)
 *
 * THE TRAP: the heap invariant is parent <= children — it says NOTHING about siblings,
 * and nothing about array order beyond that. for-each walks the internal ARRAY, so the
 * elements come out in heap layout, which is NOT sorted. Only peek()/poll() — which
 * always look at the ROOT — give you the minimum. Students who print a PriorityQueue
 * with for-each and see "1.00, 5.00, 3.00" think it's broken; it isn't, it's a heap.
 * The sorted order exists only in the DRAIN: poll() repeatedly, and out comes the min
 * every time, i.e. fully sorted.
 *
 * Order: with no comparator it uses the elements' NATURAL order (Comparable — core-java
 * 10). Transaction's natural order is by id; to get "cheapest fee first" we hand it an
 * anonymous Comparator by amount instead. Same class, different order, one argument.
 */
public class PriorityQueueDemo {

    public static void main(String[] args) {
        banner("1. The heap is NOT sorted — offer 5, 1, 3 and watch for-each lie");

        // Deliberately chosen insertion sequence. The heap after these three offers is:
        //         1.00            internal array: [1.00, 5.00, 3.00]
        //        /    \           (root = min; 5.00 and 3.00 are siblings, in NO order)
        //     5.00    3.00
        Queue<BigDecimal> heap = new PriorityQueue<BigDecimal>();
        heap.offer(new BigDecimal("5.00"));
        heap.offer(new BigDecimal("1.00"));
        heap.offer(new BigDecimal("3.00"));

        // for-each prints the internal array layout — heap order, NOT sorted order:
        System.out.print("for-each order (heap array, NOT a contract): [");
        boolean first = true;
        for (BigDecimal b : heap) {
            if (!first) {
                System.out.print(", ");
            }
            System.out.print(b.toPlainString());
            first = false;
        }
        System.out.println("]");
        System.out.println("^ notice 5.00 BEFORE 3.00 — a sorted list would never do that. A heap may.");
        System.out.println("The ONLY promises: peek() = the min, and poll() drains in sorted order.");

        banner("2. peek() = the minimum, O(1) — the root of the heap");

        System.out.println("peek() -> " + ((BigDecimal) heap.peek()).toPlainString() + "   (the root; peek does not remove)");
        System.out.println("size still = " + heap.size());

        banner("3. poll() drains it in SORTED order — the sorted sequence lives HERE");

        List<BigDecimal> drained = new ArrayList<BigDecimal>();
        BigDecimal next;
        while ((next = heap.poll()) != null) {      // each poll re-heaps in O(log n)
            drained.add(next);
        }
        System.out.println("drain by poll(): " + drained + "   <- THIS is sorted. for-each was not.");
        System.out.println("Mental model: a PriorityQueue is a 'give me the smallest NEXT' machine,");
        System.out.println("not a sorted container. If you want it all sorted, DRAIN it (or use a TreeSet, module 04).");

        banner("4. Natural order vs Comparator — cheapest FEE first, not smallest id first");

        // No comparator -> natural order (Transaction.compareTo = by id). With a comparator
        // -> by amount. Classic idiom: anonymous inner class, no lambda (java8-features 01).
        Queue<Transaction> byId = new PriorityQueue<Transaction>();
        // Note the constructor: a comparator-only overload does NOT exist, so we pass the
        // default initial capacity (11) explicitly — a small API quirk worth remembering.
        Queue<Transaction> cheapestFirst = new PriorityQueue<Transaction>(11, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction a, Transaction b) {
                return a.getAmount().compareTo(b.getAmount());   // delegate to BigDecimal, never subtract
            }
        });
        for (Transaction fee : Transaction.sampleFees()) {
            byId.offer(fee);
            cheapestFirst.offer(fee);
        }
        System.out.println("natural order (by id)     peek -> " + byId.peek().getId());
        System.out.println("by-amount comparator      peek -> " + cheapestFirst.peek().getId()
                + " (" + cheapestFirst.peek().getAmount().toPlainString() + " — the cheapest fee)");

        banner("5. Same trap on the fee queue — for-each is heap order, drain is sorted");

        System.out.print("cheapestFirst for-each (heap layout): [");
        first = true;
        for (Transaction t : cheapestFirst) {
            if (!first) {
                System.out.print(", ");
            }
            System.out.print(t.getAmount().toPlainString());
            first = false;
        }
        System.out.println("]  <- NOT sorted (siblings unordered in a heap)");

        System.out.print("cheapestFirst drained by poll():    [");
        first = true;
        Transaction t;
        while ((t = cheapestFirst.poll()) != null) {
            if (!first) {
                System.out.print(", ");
            }
            System.out.print(t.getAmount().toPlainString());
            first = false;
        }
        System.out.println("]  <- sorted ascending. THE contract.");

        banner("6. contains/remove(Object) are O(n), and null is banned (the sentinel rule)");

        Queue<Transaction> fees = new PriorityQueue<Transaction>();
        fees.offer(new Transaction("T-2001", "ATM fee", new BigDecimal("8.00"), 5000L));
        System.out.println("contains(existing) -> " + fees.contains(new Transaction("T-2001", "any", BigDecimal.ZERO, 0L))
                + "   (found by equals-by-id, but it had to SCAN — O(n), the heap has no index)");
        try {
            fees.offer(null);
        } catch (NullPointerException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from offer(null): null is the poll/peek");
            System.out.println("sentinel, so it can never be an element (same rule as ArrayDeque, demo 1).");
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
