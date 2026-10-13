package com.evaitcs.collections.iteration;

import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;

/**
 * Demo 4 — ConcurrentModificationException: THE ALARM, AND THE TIMES THE ALARM DOES NOT RING.
 *
 * The mechanics in one paragraph: every ArrayList/LinkedList counts structural modifications in
 * a field called modCount (add/remove/clear bump it; set() does not). An iterator snapshots that
 * count at birth (expectedModCount) and re-checks it in every next(): mismatch ->
 * ConcurrentModificationException. Note the name's lie: no threads are needed — one loop, one
 * thread, one remove() through the wrong door is enough. (Threads sharing collections: modules
 * 09 and 10.)
 *
 * And the lesson that makes this module more than trivia, straight from the java.util.Iterator
 * javadoc: fail-fast iterators throw CME "on a best-effort basis" — the check happens where the
 * implementation can conveniently perform it, not after every single step — therefore "it would
 * be wrong to write a program that depended on this exception for its correctness: the fail-fast
 * behavior of iterators should be used only to detect bugs." Banners 2 and 3 are that sentence
 * made visible: the SAME bug, committed twice more — and the alarm stays silent both times.
 */
public class ConcurrentModificationDemo {

    public static void main(String[] args) {
        banner("1. The explosion — removing through the LIST during for-each");

        List<Transaction> statement = Transaction.sampleStatement();
        String victim = statement.get(1).getId();                // TXN-1002 — row 2 of 6, far from any slip case
        System.out.println("statement: " + idsOf(statement));
        try {
            for (Transaction t : statement) {
                System.out.println("  visiting " + t.getId());
                if (t.getId().equals(victim)) {
                    statement.remove(t);                         // structural change through the list — modCount++
                    System.out.println("  removed " + t.getId() + " through the LIST — the iterator was NOT told");
                }
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getName() + " (message: " + e.getMessage() + ")");
            System.out.println("The timeline: remove() bumped the list's modCount; the iterator's expectedModCount stayed;");
            System.out.println("hasNext() asked only 'cursor != size' — it never looks at modCount — and said yes;");
            System.out.println("next() ran checkForComodification() first: expected 0, found 1 -> boom.");
            System.out.println("(In most JDKs the message is null — the exception's TYPE is the message.)");
        }

        banner("2. The slip, ArrayList version — delete the SECOND-TO-LAST element: silence");

        List<Transaction> array = Transaction.sampleStatement();
        String secondToLast = array.get(array.size() - 2).getId();       // TXN-1005
        System.out.println("statement: " + idsOf(array) + "   — deleting " + secondToLast + " (second-to-last) via the list");
        for (Transaction t : array) {
            if (t.getId().equals(secondToLast)) {
                array.remove(t);                                 // the exact same bug as banner 1...
            }
        }
        System.out.println("...and the loop COMPLETED: " + idsOf(array));
        System.out.println("Why: visiting row 5 left cursor = 5; the removal made size = 5;");
        System.out.println("ArrayList's hasNext() is 'cursor != size' -> 5 != 5 -> false -> the loop ended");
        System.out.println("BEFORE a next() could check modCount. TXN-1006 was never even examined.");
        System.out.println("The bug happened. The alarm did not ring. That is 'best-effort'.");

        banner("3. The slip, LinkedList version — delete the LAST element: silence");

        List<Transaction> nodes = new LinkedList<Transaction>(Transaction.sampleStatement());
        String last = nodes.get(nodes.size() - 1).getId();               // TXN-1006
        System.out.println("statement: " + idsOf(nodes) + "   — deleting " + last + " (last) via the list");
        for (Transaction t : nodes) {
            if (t.getId().equals(last)) {
                nodes.remove(t);                                 // same bug, different engine...
            }
        }
        System.out.println("...and the loop COMPLETED: " + idsOf(nodes));
        System.out.println("Why: LinkedList's hasNext() is 'nextIndex < size' -> after the removal 6 < 5 -> false.");
        System.out.println("Same bug as banner 1, no exception — the check simply never got scheduled.");

        banner("4. The moral — CME is a smoke detector, not a sprinkler system");

        System.out.println("The java.util.Iterator javadoc, condensed: 'Fail-fast iterators throw");
        System.out.println("ConcurrentModificationException on a best-effort basis... it would be wrong to");
        System.out.println("write a program that depended on this exception for its correctness: [fail-fast]");
        System.out.println("should be used only to detect bugs.'");
        System.out.println();
        System.out.println("So: a CME always means a real bug (something mutated behind the iterator's back).");
        System.out.println("The ABSENCE of a CME means nothing at all — banners 2 and 3 are the proof.");
        System.out.println("The only correct removals during a walk: iterator.remove() (Demo 3), rebuild into a new list,");
        System.out.println("or Java 8's list.removeIf(...) — java8-features 01. The modCount machinery: module 09.");
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
