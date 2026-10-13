package com.evaitcs.collections.iteration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 03-iteration-and-iterators test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC — including the explosions AND the silences: the
 * CME fires on chosen values every run (and, per the best-effort nature of fail-fast, the slip
 * test pins down exactly which values must NOT fire it), and the two IllegalStateExceptions are
 * the iterator's own documented contract. Lambdas appear only as assertThrows arguments — the
 * main code stays classic.
 */
class IterationContractTest {

    private static List<String> idsOf(List<Transaction> statement) {
        List<String> ids = new ArrayList<String>();
        for (Transaction t : statement) {
            ids.add(t.getId());
        }
        return ids;
    }

    @Test
    @DisplayName("removing a non-last element through the LIST during for-each -> ConcurrentModificationException")
    void cmeWhenRemovingDuringForEach() {
        final List<Transaction> statement = Transaction.sampleStatement();     // 6 rows
        final String victim = statement.get(1).getId();                        // TXN-1002 — far from any slip case

        ConcurrentModificationException e = assertThrows(ConcurrentModificationException.class,
                () -> {
                    for (Transaction t : statement) {
                        if (t.getId().equals(victim)) {
                            statement.remove(t);   // structural change through the list: modCount++, iterator not told
                        }
                    }
                });
        System.out.println("CAUGHT -> " + e.getClass().getName() + " (message: " + e.getMessage()
                + ") — expectedModCount and modCount disagreed at the next next()");
    }

    @Test
    @DisplayName("fail-fast is best-effort: removing the LAST element in a for-each can complete silently")
    void lastElementRemovalSlipsThrough() {
        // LinkedList's hasNext() is `nextIndex < size` and NEVER looks at modCount: delete the
        // last element mid-walk and 6 < 5 is false — the loop ends before a next() could throw.
        // The bug happened; the alarm stayed silent.
        List<Transaction> nodes = new LinkedList<Transaction>(Transaction.sampleStatement());
        String lastId = nodes.get(nodes.size() - 1).getId();
        for (Transaction t : nodes) {
            if (t.getId().equals(lastId)) {
                nodes.remove(t);
            }
        }
        assertEquals(5, nodes.size(), "the element IS gone...");
        assertFalse(idsOf(nodes).contains(lastId));
        System.out.println("LinkedList, last element removed in a for-each: no exception, ids = " + idsOf(nodes));

        // The ArrayList flavor of the same silence: the SECOND-TO-LAST element (after its removal
        // cursor == size, so hasNext() is false). Different comparison, identical lesson.
        List<Transaction> array = Transaction.sampleStatement();
        String secondToLastId = array.get(array.size() - 2).getId();
        for (Transaction t : array) {
            if (t.getId().equals(secondToLastId)) {
                array.remove(t);
            }
        }
        assertEquals(5, array.size());
        assertFalse(idsOf(array).contains(secondToLastId));
        System.out.println("ArrayList, second-to-last removed: also no exception, ids = " + idsOf(array));
        System.out.println("-> java.util.Iterator javadoc: CME is thrown 'on a best-effort basis' — never depend on it for correctness.");
    }

    @Test
    @DisplayName("iterator.remove() is the safe in-loop removal: the filter works and the list reflects it")
    void iteratorRemoveWorksAndListReflectsIt() {
        List<Transaction> statement = Transaction.sampleStatement();
        BigDecimal auditMinimum = new BigDecimal("100.00");

        int removed = 0;
        Iterator<Transaction> it = statement.iterator();
        while (it.hasNext()) {
            Transaction t = it.next();
            if (t.getAmount().compareTo(auditMinimum) < 0) {   // BigDecimal: compareTo, never equals (scale!)
                it.remove();                                   // deletes the last next()'s element — and syncs modCount
                removed++;
            }
        }

        assertEquals(2, removed, "exactly the 86.40 groceries and the 3.75 interest are below 100.00");
        assertEquals(4, statement.size());
        for (Transaction t : statement) {
            assertTrue(t.getAmount().compareTo(auditMinimum) >= 0, "every survivor passes the audit");
        }
        System.out.println("it.remove() dropped " + removed + " rows; survivors = " + idsOf(statement));
    }

    @Test
    @DisplayName("remove() twice per next() -> IllegalStateException, and the list is untouched by the second")
    void doubleRemoveThrowsIllegalState() {
        List<Transaction> statement = Transaction.sampleStatement();
        final Iterator<Transaction> it = statement.iterator();
        it.next();
        it.remove();
        assertEquals(5, statement.size(), "the first remove() did its job");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> it.remove());
        assertEquals(5, statement.size(), "the second remove() did NOT touch the list");
        System.out.println("CAUGHT -> " + e.getClass().getName() + " — remove() is legal exactly once per next(); lastRet was reset");
    }

    @Test
    @DisplayName("remove() before any next() -> IllegalStateException: there is no 'last returned' to remove")
    void removeBeforeNextThrowsIllegalState() {
        List<Transaction> statement = Transaction.sampleStatement();
        final Iterator<Transaction> it = statement.iterator();

        assertThrows(IllegalStateException.class, () -> it.remove());
        assertEquals(6, statement.size(), "the list is intact");
        System.out.println("CAUGHT -> IllegalStateException on a fresh iterator — note: NOT a CME. Different contract, different bug.");
    }

    @Test
    @DisplayName("ListIterator: set() rewrites in place, add() inserts at the cursor, previous() walks back")
    void listIteratorSetAddPrevious() {
        List<String> ids = new ArrayList<String>(Arrays.asList("A", "B", "C"));
        ListIterator<String> lit = ids.listIterator();

        assertEquals("A", lit.next());
        lit.set("A2");
        assertEquals(Arrays.asList("A2", "B", "C"), ids, "set() replaced the element the last next() returned");

        assertEquals("B", lit.next());
        lit.add("X");
        assertEquals(Arrays.asList("A2", "B", "X", "C"), ids, "add() inserted AT the cursor — structural, but the iterator is in on it");
        assertEquals("X", lit.previous(), "after add(), previous() returns the new element");
        assertEquals("B", lit.previous());
        assertEquals(1, lit.nextIndex(), "cursor introspection: sitting between A2 and B again");
        System.out.println("set/add/previous over [A, B, C] -> " + ids + ", nextIndex = " + lit.nextIndex());
    }

    @Test
    @DisplayName("for-each and the explicit Iterator print identical sequences — because they ARE the same walk")
    void forEachAndIteratorPrintIdenticalSequences() {
        List<Transaction> statement = Transaction.sampleStatement();

        StringBuilder viaSugar = new StringBuilder();
        for (Transaction t : statement) {
            viaSugar.append(t.getId()).append(' ');
        }

        StringBuilder viaMachine = new StringBuilder();
        Iterator<Transaction> it = statement.iterator();
        while (it.hasNext()) {
            Transaction t = it.next();
            viaMachine.append(t.getId()).append(' ');
        }

        assertEquals(viaMachine.toString(), viaSugar.toString(), "javac's desugaring, pinned as evidence");
        System.out.println("both loops printed: " + viaSugar.toString().trim());
    }
}
