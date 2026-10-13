package com.evaitcs.java8.upgrades;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for RemoveIfDemo's claims: the CME trap is real, iterator.remove is the
 * safe old way, and removeIf agrees with it - in one line.
 */
class RemoveIfTest {

    private List<String> idsOf(List<Transaction> txns) {
        List<String> ids = new ArrayList<>();
        for (Transaction t : txns) {
            ids.add(t.getId());
        }
        return ids;
    }

    @Test
    @DisplayName("list.remove INSIDE a for-each throws ConcurrentModificationException")
    void removeInsideForEachExplodes() {
        List<Transaction> statement = new ArrayList<>(Transaction.sampleStatement());
        ConcurrentModificationException boom = assertThrows(ConcurrentModificationException.class, () -> {
            for (Transaction t : statement) {
                if (t.isDeposit()) {
                    statement.remove(t); // structural change behind the iterator's back
                }
            }
        });
        // WHY: the for-each iterator's expectedModCount no longer matches the list's modCount.
        System.out.println("caught as promised: " + boom.getClass().getName());
        assertEquals(ConcurrentModificationException.class, boom.getClass());
    }

    @Test
    @DisplayName("iterator.remove is the safe pre-Java-8 way: no exception, deposits gone")
    void iteratorRemoveIsSafe() {
        List<Transaction> statement = new ArrayList<>(Transaction.sampleStatement());
        Iterator<Transaction> it = statement.iterator();
        while (it.hasNext()) {
            if (it.next().isDeposit()) {
                it.remove(); // safe: the iterator re-syncs its own expectedModCount
            }
        }
        System.out.println("kept via iterator.remove: " + idsOf(statement));
        assertEquals(3, statement.size());
        assertFalse(statement.stream().anyMatch(Transaction::isDeposit));
    }

    @Test
    @DisplayName("removeIf removes exactly the same elements as iterator.remove - one line")
    void removeIfMatchesIteratorRemove() {
        List<Transaction> viaRemoveIf = new ArrayList<>(Transaction.sampleStatement());
        boolean changed = viaRemoveIf.removeIf(Transaction::isDeposit);

        List<Transaction> viaIterator = new ArrayList<>(Transaction.sampleStatement());
        Iterator<Transaction> it = viaIterator.iterator();
        while (it.hasNext()) {
            if (it.next().isDeposit()) {
                it.remove();
            }
        }
        System.out.println("removeIf kept: " + idsOf(viaRemoveIf) + " (changed=" + changed + ")");
        assertTrue(changed, "removeIf returns true when it actually removed something");
        assertEquals(idsOf(viaIterator), idsOf(viaRemoveIf));

        // and it reports false when there was nothing to do:
        assertFalse(viaRemoveIf.removeIf(Transaction::isDeposit), "second pass removes nothing");
    }

    @Test
    @DisplayName("List.sort produces the SAME order as Collections.sort")
    void listSortEqualsCollectionsSort() {
        Comparator<Transaction> byAmount = Comparator.comparing(Transaction::getAmount);

        List<Transaction> viaListSort = new ArrayList<>(Transaction.sampleStatement());
        viaListSort.sort(byAmount); // Java 8: default method on List

        List<Transaction> viaCollections = new ArrayList<>(Transaction.sampleStatement());
        Collections.sort(viaCollections, byAmount); // Java 7: helper class

        System.out.println("list.sort order: " + idsOf(viaListSort));
        assertEquals(idsOf(viaCollections), idsOf(viaListSort));
    }

    @Test
    @DisplayName("Iterable.forEach visits EVERY element exactly once")
    void forEachVisitsEveryElement() {
        List<Transaction> statement = Transaction.sampleStatement();
        final int[] visited = { 0 };
        statement.forEach(t -> visited[0]++);
        System.out.println("forEach visited " + visited[0] + " of " + statement.size());
        assertEquals(statement.size(), visited[0]);
    }
}
