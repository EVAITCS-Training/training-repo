package com.evaitcs.collections.queues;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 06-queue-and-deque test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC. Note carefully what is NOT asserted: the
 * for-each order of a PriorityQueue. Heap layout is an implementation detail, not a
 * contract — the test proves what IS contracted (peek = min, drain = sorted, iteration
 * holds the same multiset) and prints the raw heap order as evidence only.
 */
class QueueDequeContractTest {

    @Test
    @DisplayName("empty queue, two flavors: element/remove THROW, peek/poll return null, offer returns true")
    void twoFlavorsOnEmptyQueue() {
        Queue<String> empty = new ArrayDeque<String>();

        // The THROW flavor: failure is an exception you cannot ignore.
        assertThrows(NoSuchElementException.class, () -> empty.element());
        assertThrows(NoSuchElementException.class, () -> empty.remove());

        // The SPECIAL-VALUE flavor: failure is null — the idiomatic, branchable answer.
        assertNull(empty.peek(), "peek on empty = null, not an exception");
        assertNull(empty.poll(), "poll on empty = null, not an exception");

        // And insertion: ArrayDeque is unbounded, so offer has nothing to refuse —
        // it returns true always (false only ever happens on a BOUNDED full queue).
        assertTrue(empty.offer("first"));
        assertTrue(empty.add("second"));
        assertEquals(2, empty.size());
        System.out.println("empty queue: element()/remove() -> NoSuchElementException; peek()/poll() -> null; offer() -> true");
    }

    @Test
    @DisplayName("FIFO: poll() drains the statement line in exact arrival order")
    void fifoDrainsInOfferOrder() {
        Queue<Transaction> line = new ArrayDeque<Transaction>();
        for (Transaction t : Transaction.sampleStatement()) {
            line.offer(t);
        }
        assertEquals(Transaction.sampleStatement().size(), line.size());

        List<String> drained = new ArrayList<String>();
        Transaction current;
        while ((current = line.poll()) != null) {
            drained.add(current.getId());
        }
        assertEquals(Arrays.asList("T-1041", "T-1042", "T-1043", "T-1044"), drained,
                "first in, first out — the arrival order, exactly");
        assertNull(line.poll(), "past the end: the null sentinel, never an exception");
        System.out.println("drained FIFO: " + drained);
    }

    @Test
    @DisplayName("PriorityQueue: peek = min, drain by poll = sorted, for-each = heap order (contains-all only)")
    void priorityQueueDrainsSortedButIteratesHeapOrder() {
        Queue<BigDecimal> heap = new PriorityQueue<BigDecimal>();
        heap.offer(new BigDecimal("5.00"));
        heap.offer(new BigDecimal("1.00"));
        heap.offer(new BigDecimal("3.00"));

        // Claim 1: the root is the minimum — the ONLY positional promise a heap makes.
        assertEquals(0, heap.peek().compareTo(new BigDecimal("1.00")), "peek() = the minimum");

        // Claim 2: for-each holds every element — but in HEAP layout, so we may only
        // assert contains-all (same multiset), NEVER the exact sequence.
        List<BigDecimal> iterated = new ArrayList<BigDecimal>(heap);
        assertEquals(3, iterated.size());

        // Claim 3: draining by poll() IS fully sorted — that is where the order lives.
        List<BigDecimal> drained = new ArrayList<BigDecimal>();
        BigDecimal next;
        while ((next = heap.poll()) != null) {
            drained.add(next);
        }
        assertEquals(Arrays.asList(new BigDecimal("1.00"), new BigDecimal("3.00"), new BigDecimal("5.00")),
                drained, "poll() hands out the min every time — the drain is sorted");

        List<BigDecimal> iteratedSorted = new ArrayList<BigDecimal>(iterated);
        Collections.sort(iteratedSorted);
        assertEquals(drained, iteratedSorted, "iteration and drain are the same MULTISET — only the order differs");
        System.out.println("raw for-each (heap layout, NOT asserted): " + iterated);
        System.out.println("drain by poll (sorted, THE contract):    " + drained);
    }

    @Test
    @DisplayName("PriorityQueue with a comparator: cheapest fee first, not smallest id first")
    void priorityQueueUsesComparatorWhenGiven() {
        // Classic idiom: anonymous Comparator, no lambda (main code never uses them either).
        Queue<Transaction> cheapestFirst = new PriorityQueue<Transaction>(11, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction a, Transaction b) {
                return a.getAmount().compareTo(b.getAmount());
            }
        });
        for (Transaction fee : Transaction.sampleFees()) {
            cheapestFirst.offer(fee);
        }

        assertEquals("T-2004", cheapestFirst.peek().getId(), "peek = the 2.00 paper-statement fee");
        List<String> drain = new ArrayList<String>();
        Transaction t;
        while ((t = cheapestFirst.poll()) != null) {
            drain.add(t.getId());
        }
        // by amount ascending: 2.00 (T-2004), 3.75 (T-2002), 8.00 (T-2001), 12.50 (T-2003)
        assertEquals(Arrays.asList("T-2004", "T-2002", "T-2001", "T-2003"), drain);
        System.out.println("drain by amount: " + drain + " — the comparator, not the id, decided the order");
    }

    @Test
    @DisplayName("Deque as a stack: push/pop is LIFO — the undo history")
    void dequePushPopIsLifo() {
        Deque<String> undo = new ArrayDeque<String>();
        undo.push("open account");
        undo.push("view statement");
        undo.push("transfer");

        assertEquals("transfer", undo.peek(), "peek sees the most recent push");
        assertEquals("transfer", undo.pop(), "last in, first out");
        assertEquals("view statement", undo.pop());
        assertEquals("open account", undo.pop());
        assertTrue(undo.isEmpty());
        assertThrows(NoSuchElementException.class, () -> undo.pop(),
                "pop is the THROW flavor — empty is NoSuchElementException, never null");
        System.out.println("LIFO proven: transfer, view statement, open account — reverse of push order");
    }

    @Test
    @DisplayName("null is the empty-sentinel, so queues and deques BAN null elements (NPE)")
    void nullIsBannedAsAnElement() {
        final Deque<String> deque = new ArrayDeque<String>();
        assertThrows(NullPointerException.class, () -> deque.add(null));
        assertThrows(NullPointerException.class, () -> deque.offerFirst(null));

        final Queue<Transaction> heap = new PriorityQueue<Transaction>();
        assertThrows(NullPointerException.class, () -> heap.offer(null));
        System.out.println("ArrayDeque.add(null), offerFirst(null), PriorityQueue.offer(null) -> NullPointerException");
        System.out.println("Reason: poll()/peek() already use null to mean 'empty' — null cannot ALSO be data.");
    }

    @Test
    @DisplayName("the legacy Stack IS a Vector (that is the whole problem); ArrayDeque is not")
    void stackIsLegacyVector() {
        Stack<String> legacy = new Stack<String>();
        assertTrue(legacy instanceof Stack);
        assertTrue(legacy instanceof Vector, "Stack extends Vector — the Java 1.0 growable array");
        Deque<String> modern = new ArrayDeque<String>();   // via the INTERFACE, so instanceof compiles
        assertFalse(modern instanceof Vector, "ArrayDeque: framework-native, no Vector baggage");

        // Inheritance hands every caller Vector's indexed side doors: LIFO unenforceable.
        legacy.push("bottom");
        legacy.push("top");
        legacy.add(0, "injected in the middle — legal, because a Stack IS a Vector");
        assertEquals("injected in the middle — legal, because a Stack IS a Vector", legacy.get(0));
        System.out.println("Stack instanceof Vector = true, and add(0, ...) broke LIFO legally. Use ArrayDeque.");
    }
}
