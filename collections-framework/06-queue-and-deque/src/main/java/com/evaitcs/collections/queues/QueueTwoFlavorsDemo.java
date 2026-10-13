package com.evaitcs.collections.queues;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Demo 1 — THE TWO API FLAVORS: every Queue operation comes in two styles.
 *
 * The Queue interface deliberately offers BOTH a "throw on failure" set and a
 * "return a special value" set for the same three operations. This is the table
 * students photograph, and the reason is a single design decision: a queue must be
 * able to say "I'm empty" WITHOUT an exception, because empty is a normal state,
 * not an error. The special-value flavor uses null (peek/poll) and false (offer)
 * so callers can branch; the throw flavor uses exceptions so callers cannot ignore it.
 *
 * The consequence — printed as evidence in the final banner — is that null is now
 * TAKEN as the "empty" sentinel, so a queue can never STORE null: offer(null) and
 * add(null) throw NullPointerException. That single rule explains why ArrayDeque and
 * PriorityQueue ban nulls, and it is asserted in QueueDequeContractTest.
 */
public class QueueTwoFlavorsDemo {

    public static void main(String[] args) {
        banner("1. The table — same three operations, two flavors");

        System.out.println("             | throws-on-failure | returns-special-value");
        System.out.println("  insert     | add(e)            | offer(e) -> false if it cannot fit");
        System.out.println("  remove head| remove()          | poll()   -> null if empty");
        System.out.println("  read head  | element()         | peek()   -> null if empty");
        System.out.println();
        System.out.println("Rule of thumb: PREFER offer/poll/peek. Empty is a normal state, not an error,");
        System.out.println("so a queue should be able to report it with a value instead of an exception.");

        banner("2. The THROW flavor on an empty queue — element() and remove() explode");

        Queue<String> empty = new ArrayDeque<String>();
        System.out.println("fresh ArrayDeque, isEmpty() = " + empty.isEmpty());
        try {
            empty.element();
        } catch (NoSuchElementException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from element()  (no head to read)");
        }
        try {
            empty.remove();
        } catch (NoSuchElementException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from remove()    (no head to take)");
        }
        System.out.println("element()/remove() refuse to pretend: if there is no head, you get an exception.");

        banner("3. The SPECIAL-VALUE flavor on the SAME empty queue — peek()/poll() return null");

        System.out.println("peek() -> " + empty.peek() + "   (null means 'empty', not 'the head was null')");
        System.out.println("poll() -> " + empty.poll() + "   (same null sentinel)");
        System.out.println("This is the idiomatic drain loop: while ((x = q.poll()) != null) process(x);");

        banner("4. offer() always returns true on an UNBOUNDED queue like ArrayDeque");

        System.out.println("offer(\"first\")  -> " + empty.offer("first"));
        System.out.println("add(\"second\")   -> " + empty.add("second") + " (add throws IllegalStateException when it cannot fit — the throw flavor)");
        System.out.println("size now = " + empty.size());
        System.out.println("offer() returns false ONLY when a bounded queue is full. ArrayDeque has no bound,");
        System.out.println("so it is always true here. Bounded queues that CAN say false live in module 10");
        System.out.println("(BlockingQueue.offer with a timeout — the producer/consumer machinery).");

        banner("5. The consequence — null is the sentinel, so null elements are BANNED");

        Queue<String> noNulls = new ArrayDeque<String>();
        try {
            noNulls.offer(null);
        } catch (NullPointerException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from offer(null)");
        }
        try {
            noNulls.add(null);
        } catch (NullPointerException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from add(null)");
        }
        System.out.println("If a queue stored null, poll() could not tell 'empty' from 'the head was null'.");
        System.out.println("So it refuses to store null at all. Same rule in PriorityQueue and ArrayDeque.");
        System.out.println("(LinkedList is the historical exception that ALLOWS null — one more reason to");
        System.out.println(" prefer ArrayDeque: its no-nulls rule removes a whole class of ambiguity.)");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
