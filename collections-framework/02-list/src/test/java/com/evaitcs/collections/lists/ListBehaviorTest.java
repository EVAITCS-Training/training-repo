package com.evaitcs.collections.lists;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 02-list test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC — including the failures: the skip bug leaves the
 * SAME surviving FEE on every run, the subList ConcurrentModificationException fires on chosen
 * values, and the remove(int)/remove(Integer) overload binding is a compile-time fact. Timings
 * are printed in ArrayListVsLinkedListDemo for illustration and are NEVER asserted here.
 * Lambdas appear only as assertThrows arguments — the main code stays classic.
 */
class ListBehaviorTest {

    private static List<String> idsOf(List<Transaction> statement) {
        List<String> ids = new ArrayList<String>();
        for (Transaction t : statement) {
            ids.add(t.getId());
        }
        return ids;
    }

    @Test
    @DisplayName("growth never loses elements: 1000 adds, all present, original order")
    void growthPreservesAllElements() {
        List<Integer> numbers = new ArrayList<Integer>();
        for (int i = 0; i < 1000; i++) {
            numbers.add(Integer.valueOf(i));       // explicit boxing — the classic idiom (core-java 02)
        }

        assertEquals(1000, numbers.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals(Integer.valueOf(i), numbers.get(i), "element " + i + " sits at position " + i);
        }
        System.out.println("1000 adds survived ~12 grow-and-copy events (10 -> 15 -> 22 -> ... -> 1234);");
        System.out.println("first = " + numbers.get(0) + ", last = " + numbers.get(999) + ", size = " + numbers.size()
                + " — growth reallocates, never loses or reorders");
    }

    @Test
    @DisplayName("add(i, e) shifts the tail right; remove(i) shifts it back")
    void middleInsertShiftsCorrectly() {
        List<String> ids = new ArrayList<String>(Arrays.asList("TXN-1", "TXN-2", "TXN-4"));

        ids.add(2, "TXN-3");                       // make room: TXN-4 slides one slot right
        assertEquals(Arrays.asList("TXN-1", "TXN-2", "TXN-3", "TXN-4"), ids);
        assertEquals(2, ids.indexOf("TXN-3"), "the newcomer sits exactly where it was told");
        assertEquals(3, ids.indexOf("TXN-4"), "everything at/after the hole moved one slot right");

        String removed = ids.remove(2);            // the tail slides back left
        assertEquals("TXN-3", removed, "remove(int) RETURNS the element that left");
        assertEquals(Arrays.asList("TXN-1", "TXN-2", "TXN-4"), ids);
        System.out.println("insert at 2 then remove(2): " + ids + " — the array underneath slid right, then left");
    }

    @Test
    @DisplayName("pitfall: forward loop + remove(i) skips elements — one FEE survives")
    void forwardRemovalSkipsElements() {
        List<String> kinds = new ArrayList<String>(Arrays.asList("PAY", "FEE", "PAY", "FEE", "FEE"));

        for (int i = 0; i < kinds.size(); i++) {
            if (kinds.get(i).equals("FEE")) {
                kinds.remove(i);                   // the shift slides the unexamined successor into slot i...
            }                                      // ...and i++ steps right past it
        }

        assertTrue(kinds.contains("FEE"), "the bug, reproduced: a FEE is still in the list");
        assertEquals(Arrays.asList("PAY", "PAY", "FEE"), kinds, "exactly the last FEE survived — deterministic");
        System.out.println("forward removal left " + kinds + " — every remove(i) skipped its own successor");
    }

    @Test
    @DisplayName("the fix: iterate backward and every remove(i) is safe")
    void backwardRemovalRemovesAll() {
        List<String> kinds = new ArrayList<String>(Arrays.asList("PAY", "FEE", "PAY", "FEE", "FEE"));

        for (int i = kinds.size() - 1; i >= 0; i--) {
            if (kinds.get(i).equals("FEE")) {
                kinds.remove(i);                   // removal only shifts elements AFTER i — already examined
            }
        }

        assertFalse(kinds.contains("FEE"), "every FEE is gone");
        assertEquals(Arrays.asList("PAY", "PAY"), kinds);
        System.out.println("backward removal: " + kinds + " — the shift and the index finally move in the same direction");
    }

    @Test
    @DisplayName("subList is a live view: set() writes through, remove() resizes the parent")
    void subListWritesThroughToParent() {
        List<Transaction> statement = Transaction.sampleStatement();
        List<Transaction> middle = statement.subList(2, 5);

        Transaction correction = new Transaction("TXN-1003", "Groceries (corrected)", new BigDecimal("84.60"),
                statement.get(2).getTimestampMillis());
        middle.set(0, correction);
        assertSame(correction, statement.get(2), "set() through the view rewrote the PARENT — the very same object");

        int sizeBefore = statement.size();
        middle.remove(0);                          // structural change THROUGH the view
        assertEquals(sizeBefore - 1, statement.size(), "removing from the view removed from the parent");
        System.out.println("view.set wrote through to parent slot 2; view.remove(0) shrank the parent to " + statement.size());
    }

    @Test
    @DisplayName("structurally modifying the parent invalidates the subList: ConcurrentModificationException")
    void parentStructuralChangeInvalidatesSubList() {
        List<Transaction> statement = Transaction.sampleStatement();
        final List<Transaction> window = statement.subList(1, 3);
        assertEquals(2, window.size(), "the view works while the parent stays quiet");

        // Structural change through the back door — an append FAR from the window's range.
        // It still bumps the parent's modCount; the view's remembered count is now stale.
        statement.add(new Transaction("TXN-1007", "Late posting", new BigDecimal("17.00"), 1_700_604_800_000L));

        ConcurrentModificationException e = assertThrows(ConcurrentModificationException.class,
                () -> window.get(0));
        System.out.println("CAUGHT -> " + e.getClass().getSimpleName()
                + " — the parent's modCount moved, the view's copy did not (module 09 owns the machinery)");
    }

    @Test
    @DisplayName("List<Integer>: remove(1) deletes the SLOT, remove(Integer.valueOf(1)) deletes the VALUE")
    void removeIntVersusRemoveInteger() {
        List<Integer> slots = new ArrayList<Integer>(Arrays.asList(Integer.valueOf(10), Integer.valueOf(20), Integer.valueOf(30)));
        Integer removedByIndex = slots.remove(1);                          // binds remove(int index)
        assertEquals(Integer.valueOf(20), removedByIndex, "the index overload RETURNS the element that left");
        assertEquals(Arrays.asList(Integer.valueOf(10), Integer.valueOf(30)), slots,
                "position 1 is gone — the value 1 was never involved");

        List<Integer> values = new ArrayList<Integer>(Arrays.asList(Integer.valueOf(10), Integer.valueOf(1), Integer.valueOf(30)));
        boolean removedByValue = values.remove(Integer.valueOf(1));        // binds remove(Object)
        assertTrue(removedByValue, "the Object overload RETURNS a boolean");
        assertEquals(Arrays.asList(Integer.valueOf(10), Integer.valueOf(30)), values,
                "the VALUE 1 is gone — from position 1, a coincidence, not an index");
        System.out.println("remove(1) returned " + removedByIndex + " from [10, 20, 30]; remove(Integer.valueOf(1)) returned "
                + removedByValue + " from [10, 1, 30]");
    }

    @Test
    @DisplayName("LinkedList is the same List: equal elements, equal order, equal hashCode")
    void linkedListBehavesIdenticallyAsList() {
        List<Transaction> arrayBacked = Transaction.sampleStatement();                           // a fresh ArrayList
        List<Transaction> nodeBacked = new LinkedList<Transaction>(Transaction.sampleStatement());

        assertEquals(arrayBacked, nodeBacked, "List.equals compares elements+order, never the engine");
        assertEquals(arrayBacked.hashCode(), nodeBacked.hashCode(),
                "equal Lists MUST hash equal — core-java 08's contract, applied to collections themselves");

        // The same surgery on both keeps them equal — behavior is the interface's, not the class's:
        arrayBacked.remove(0);
        nodeBacked.remove(0);
        assertEquals(arrayBacked, nodeBacked);
        System.out.println("ArrayList.equals(LinkedList) = true and hashCodes match; after remove(0) both are "
                + idsOf(nodeBacked));
    }
}
