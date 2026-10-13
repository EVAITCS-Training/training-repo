package com.evaitcs.collections.lists;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * Demo 3 — subList(from, to) IS A LIVE WINDOW, NOT A COPY — and windows have expiry dates.
 *
 * The view reads the parent's storage (offsets into the very same elementData on an ArrayList):
 *   - view.set(i, e)     rewrites the PARENT;
 *   - parent.set(i, e)   shows up in the VIEW;
 *   - view.add/remove    are STRUCTURAL and resize the parent;
 *   - parent.add/remove  are structural too — and they INVALIDATE the view.
 *
 * The invalidation is fail-fast's first appearance in this course: every structural modification
 * bumps the list's modCount; the view remembers the count it was born with; the next call through
 * the view compares the two and throws ConcurrentModificationException on a mismatch. Module 03
 * trips the same mechanism while iterating; module 09 dissects it line by line.
 *
 * The toIndex is EXCLUSIVE (like every range in Java): subList(2, 5) is positions 2, 3, 4 —
 * and subList(i, i + n) reads as "n rows starting at i".
 */
public class SubListViewDemo {

    public static void main(String[] args) {
        banner("1. The window — same objects, not copies");

        List<Transaction> statement = Transaction.sampleStatement();
        List<Transaction> middle = statement.subList(2, 5);        // positions 2, 3, 4 — toIndex exclusive
        System.out.println("parent : " + statement);
        System.out.println("view   : " + middle + "   <- subList(2, 5)");
        System.out.println("view.get(0) == statement.get(2) ? " + (middle.get(0) == statement.get(2))
                + "   <- ==, identity: the very same object behind both names");

        banner("2. Writes go through — in BOTH directions");

        Transaction correction = new Transaction("TXN-1003", "Groceries (corrected)", new BigDecimal("84.60"),
                statement.get(2).getTimestampMillis());
        middle.set(0, correction);
        System.out.println("view.set(0, correction) -> parent slot 2 now : " + statement.get(2));
        Transaction note = new Transaction("TXN-1005", "Interest (bank note)", new BigDecimal("3.75"),
                statement.get(4).getTimestampMillis());
        statement.set(4, note);
        System.out.println("parent.set(4, note)     -> view slot 2 now   : " + middle.get(2));
        System.out.println("One memory, two names: a view is a range + a parent reference, nothing more.");

        banner("3. Structural changes through the view resize the PARENT");

        int sizeBefore = statement.size();
        middle.remove(0);                                          // takes out what was statement index 2
        System.out.println("view.remove(0): parent size " + sizeBefore + " -> " + statement.size());
        middle.add(new Transaction("TXN-1008", "Fee refund", new BigDecimal("12.00"), 1_700_604_800_000L));
        System.out.println("view.add(e)   : parent size -> " + statement.size() + ", and the parent gained the row:");
        System.out.println("parent : " + statement);

        banner("4. The expiry — a structural change to the PARENT kills the view");

        // The back door: append to the parent, far from the view's range — does not matter.
        // ANY structural change to the parent bumps its modCount; the view's remembered count is stale.
        statement.add(new Transaction("TXN-1009", "Late posting", new BigDecimal("17.25"), 1_700_691_200_000L));
        try {
            System.out.println("view.get(0) -> " + middle.get(0));
        } catch (ConcurrentModificationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " (message: " + e.getMessage() + ")");
            System.out.println("No threads involved — one thread, two lines apart. The view compared its");
            System.out.println("birth modCount with the parent's current one, and they disagreed.");
            System.out.println("Module 03 makes iterators throw this same exception; module 09 shows the machinery.");
        }

        banner("5. When you need data that SURVIVES the parent: take a snapshot");

        List<Transaction> fresh = Transaction.sampleStatement();
        List<Transaction> snapshot = new ArrayList<Transaction>(fresh.subList(1, 4));   // module 01's copy idiom
        fresh.add(new Transaction("TXN-9999", "Anything structural", new BigDecimal("1.00"), 0L));
        System.out.println("parent structurally modified again — but the snapshot is an independent ArrayList:");
        System.out.println("snapshot : " + snapshot);
        System.out.println("Rule: hold a subList for the length of one operation; hold a COPY for the length of the program.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
