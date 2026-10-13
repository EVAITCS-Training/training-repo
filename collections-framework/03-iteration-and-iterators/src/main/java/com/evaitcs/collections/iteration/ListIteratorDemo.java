package com.evaitcs.collections.iteration;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;

/**
 * Demo 5 — ListIterator: THE ITERATOR WITH A GEARBOX.
 *
 * An Iterator walks one way and has one verb (remove). A ListIterator — which every List hands
 * out via listIterator(), optionally pre-positioned via listIterator(int) — adds:
 *   previous()  walk BACKWARD (on a LinkedList, the only backward walk that is not O(n^2));
 *   set(e)      replace the element the LAST next()/previous() returned — NOT structural
 *               (modCount untouched), hence always safe mid-walk;
 *   add(e)      insert AT the cursor — structural, but the iterator updates its OWN
 *               expectedModCount, so the walk legally survives it;
 *   nextIndex()/previousIndex()  cursor introspection.
 *
 * The mental model: the cursor sits BETWEEN elements — next() takes the one to the right and
 * steps right; previous() takes the one to the left and steps left.
 *
 * The last banner is one line of history: Enumeration, the pre-1.2 ancestor
 * (hasMoreElements()/nextElement(), no remove()), frozen into old APIs like Vector and Hashtable.
 */
public class ListIteratorDemo {

    public static void main(String[] args) {
        banner("1. Two directions — one iterator");

        List<Transaction> statement = Transaction.sampleStatement();
        ListIterator<Transaction> lit = statement.listIterator();
        System.out.println("forward:");
        while (lit.hasNext()) {
            System.out.println("  nextIndex=" + lit.nextIndex() + " -> " + lit.next().getId());
        }
        System.out.println("backward (the cursor already sits at the end — no second walk from the start):");
        while (lit.hasPrevious()) {
            System.out.println("  previousIndex=" + lit.previousIndex() + " -> " + lit.previous().getId());
        }

        banner("2. set() — correct in place, safe mid-walk");

        ListIterator<Transaction> auditor = statement.listIterator();
        while (auditor.hasNext()) {
            Transaction t = auditor.next();
            if (t.getId().equals("TXN-1003")) {
                // Transaction is immutable (all fields final) — "correcting a row" means
                // REPLACING the object in its slot, which is exactly what set() does.
                auditor.set(new Transaction(t.getId(), "Groceries (audited)", t.getAmount(), t.getTimestampMillis()));
                System.out.println("set() replaced the element the last next() returned: " + statement.get(2));
            }
        }
        System.out.println("statement after the audit pass: " + idsOf(statement));
        System.out.println("set() is NOT structural: no modCount bump, no shifting — which is why it can never trip fail-fast.");

        banner("3. add() — insert at the cursor, and the iterator survives it");

        // listIterator(2) starts the cursor at index 2 — O(1) on an ArrayList; a LinkedList walks there.
        ListIterator<Transaction> inserter = statement.listIterator(2);
        Transaction fee = new Transaction("TXN-1002f", "Foreign exchange fee", new BigDecimal("7.50"), 1_700_172_800_000L);
        inserter.add(fee);                                   // inserts BEFORE the implicit cursor
        System.out.println("after add() at cursor 2: " + idsOf(statement));
        System.out.println("previous() now returns the inserted element: " + inserter.previous().getId());
        System.out.println("add() IS structural — but the ListIterator updates its own expectedModCount,");
        System.out.println("so the walk continues legally. That is the difference between it.add() and statement.add():");
        System.out.println("one tells the iterator, the other does not — and 'the other' is Demo 4's CME.");

        banner("4. One line of history — Enumeration, the grandfather");

        Enumeration<Transaction> legacy = Collections.enumeration(statement);
        StringBuilder ids = new StringBuilder();
        while (legacy.hasMoreElements()) {                   // hasMoreElements()/nextElement(), and NO remove()
            ids.append(legacy.nextElement().getId()).append(' ');
        }
        System.out.println("Enumeration walk: " + ids.toString().trim());
        System.out.println("Pre-1.2 iteration, frozen into old APIs (Vector, Hashtable) — new code uses Iterator.");
    }

    static String idsOf(List<Transaction> statement) {
        StringBuilder ids = new StringBuilder("[");
        for (Transaction t : statement) {
            ids.append(t.getId()).append(' ');
        }
        return ids.length() > 1 ? ids.toString().trim() + "]" : "[]";
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
