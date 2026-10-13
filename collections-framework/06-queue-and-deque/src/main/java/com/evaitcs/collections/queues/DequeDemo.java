package com.evaitcs.collections.queues;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Demo 4 — DEQUE: "double-ended queue", pronounced "deck". One class, TWO careers.
 *
 * A Deque extends Queue and adds the missing half: you can insert AND remove at BOTH
 * ends. That makes it a two-in-one tool:
 *
 *   Career 1 — QUEUE (FIFO): offer = addLast (in at the tail), poll = removeFirst
 *              (out at the head). First in, first out.
 *   Career 2 — STACK (LIFO): push = addFirst, pop = removeFirst — BOTH at the head,
 *              so the last one in is the first one out. This is the undo history,
 *              the browser back button, the call stack's shape.
 *
 * Every operation comes in the same two flavors as Queue (README table): addX throws
 * on failure, offerX returns false; removeX/getX throw when empty, pollX/peekX return
 * null. push/pop are the throw flavor of addFirst/removeFirst.
 *
 * WHY ArrayDeque and not LinkedList: ArrayDeque is backed by a GROWABLE ARRAY (one
 * allocation, cache-friendly), LinkedList by per-element nodes (two pointers each).
 * ArrayDeque is faster for both careers in practice, and — like every well-behaved
 * deque — it BANS nulls, for the same sentinel reason as Queue: poll/peek signal
 * "empty" with null, so null cannot also be a legal element.
 */
public class DequeDemo {

    public static void main(String[] args) {
        banner("1. Career: STACK — an undo history with push()/pop() (LIFO)");

        // push() = addFirst, pop() = removeFirst. Both touch the HEAD only, so the most
        // recent action is always the next one undone. This is what module 06 tells you
        // to use INSTEAD of the legacy java.util.Stack (see StackVsArrayDequeDemo).
        Deque<String> undo = new ArrayDeque<String>();
        undo.push("open account ACC-1005");
        undo.push("view statement");
        undo.push("transfer 50.00 to ACC-1027");
        System.out.println("after 3 actions, peek() (most recent) = " + undo.peek());

        System.out.println("undoing (pop until empty):");
        while (!undo.isEmpty()) {
            System.out.println("  <- undo: " + undo.pop());
        }
        System.out.println("Last in, FIRST out. The transfer (pushed last) is undone first.");
        System.out.println("Note: pop() is the THROW flavor — pop() on an empty deque is NoSuchElementException,");
        System.out.println("never null. The null-checked drain is pollFirst(): while ((x = d.pollFirst()) != null).");
        try {
            undo.pop();
        } catch (NoSuchElementException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from pop() on the now-empty deque");
        }

        banner("2. Both ends at once — addFirst/addLast, peekFirst/peekLast");

        Deque<String> deque = new ArrayDeque<String>();
        deque.addFirst("B");                        // [B]
        deque.addFirst("A");                        // [A, B]
        deque.addLast("C");                         // [A, B, C]
        deque.addLast("D");                         // [A, B, C, D]
        System.out.println("addFirst(A,B) then addLast(C,D) -> " + deque);
        System.out.println("peekFirst() = " + deque.peekFirst() + "   peekLast() = " + deque.peekLast());
        System.out.println("removeFirst() = " + deque.removeFirst() + "   removeLast() = " + deque.removeLast());
        System.out.println("now the middle remains: " + deque);

        banner("3. offerFirst/offerLast — the special-value flavor at BOTH ends");

        Deque<String> two = new ArrayDeque<String>();
        System.out.println("offerFirst(\"x\") -> " + two.offerFirst("x") + " (true = it fit)");
        System.out.println("offerLast(\"y\")  -> " + two.offerLast("y") + " -> deque = " + two);
        System.out.println("Same throw/special split as Queue: addX throws when it cannot, offerX returns false.");
        System.out.println("On an empty deque: peekFirst() = " + new ArrayDeque<String>().peekFirst()
                + " (null), getFirst() would throw NoSuchElementException.");

        banner("4. ArrayDeque vs LinkedList — two Deque engines, one recommendation");

        Deque<String> arrayBacked = new ArrayDeque<String>();
        Deque<String> nodeBacked = new LinkedList<String>();   // LinkedList IS a Deque (module 01)
        arrayBacked.offer("same");
        nodeBacked.offer("same");
        System.out.println("ArrayDeque engine : " + arrayBacked.getClass().getSimpleName() + " (growable array — the default choice)");
        System.out.println("LinkedList engine : " + nodeBacked.getClass().getSimpleName() + " (chain of nodes — also a List, module 02)");
        System.out.println("Both satisfy the Deque contract; ArrayDeque is faster and bans nulls. Pick ArrayDeque.");

        banner("5. The null ban, proven — addFirst(null) throws");

        Deque<String> noNulls = new ArrayDeque<String>();
        try {
            noNulls.addFirst(null);
        } catch (NullPointerException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " from addFirst(null)");
        }
        System.out.println("null is the peek/poll 'empty' signal, so it can never be an element. (Demo 1's rule.)");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
