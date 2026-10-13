package com.evaitcs.collections.iteration;

import java.util.Iterator;
import java.util.List;

/**
 * Demo 1 — THREE WAYS TO WALK A LIST, and what each one costs you.
 *
 * 1. The classic indexed for — the only shape that knows POSITIONS ("row 3 of 6"), the only
 *    one you can skip and jump inside ... and the one where every get(i) on a LinkedList walks
 *    the chain, quietly turning an innocent loop into O(n^2) (module 02's engine difference,
 *    hiding inside the syntax). The same shape as the charAt() loop over a String from
 *    core-java 03 — the index discipline is identical.
 * 2. The for-each — the reading loop: cleanest to write, works on ANY Iterable (Sets in
 *    module 04, a Map's keySet in 05, Queues in 06) — and it is COMPILED into an Iterator,
 *    which Demo 2 proves line by line. No index, no jumping, and no safe way to remove
 *    through the list (Demo 4).
 * 3. The explicit Iterator — the machinery for-each hides. The only loop shape that can remove
 *    safely, because it holds the one object whose remove() knows about the removal (Demo 3).
 *
 * The three spellings differ in ergonomics and risk; they never differ in sequence — banner 4
 * captures each walk into a string and proves all three print identical output.
 */
public class ThreeLoopsDemo {

    public static void main(String[] args) {
        List<Transaction> statement = Transaction.sampleStatement();

        banner("1. Loop one — the classic indexed for: POSITION is the feature");

        for (int i = 0; i < statement.size(); i++) {
            Transaction t = statement.get(i);
            System.out.println("  row " + (i + 1) + " of " + statement.size() + " : " + t);
        }
        System.out.println("Only this shape can say 'row 3 of 6', skip rows, or compare neighbors (get(i) vs get(i-1)).");
        System.out.println("Cost warning: on a LinkedList every get(i) walks the chain — this loop becomes O(n^2) (module 02).");

        banner("2. Loop two — the for-each: the reading loop");

        for (Transaction t : statement) {
            System.out.println("  " + t);
        }
        System.out.println("No index, no jumping — and no variable holding the iterator, hence no safe remove() in sight.");
        System.out.println("What it COMPILES to is Demo 2's whole subject; what removing through the LIST does is Demo 4's.");

        banner("3. Loop three — the explicit Iterator: the for-each before the sugar");

        Iterator<Transaction> it = statement.iterator();
        while (it.hasNext()) {
            Transaction t = it.next();
            System.out.println("  " + t);
        }
        System.out.println("The same walk, machinery visible: iterator() once, then hasNext()/next() until exhausted.");
        System.out.println("This spelling looks more ceremonial — but it is the only one holding an it.remove() (Demo 3).");

        banner("4. Three spellings, one sequence — proved, not asserted");

        String viaIndex = walkWithIndex(statement);
        String viaForEach = walkForEach(statement);
        String viaIterator = walkIterator(statement);
        System.out.println("index loop : " + viaIndex);
        System.out.println("for-each   : " + viaForEach);
        System.out.println("Iterator   : " + viaIterator);
        System.out.println("all identical? " + (viaIndex.equals(viaForEach) && viaForEach.equals(viaIterator)));
    }

    /** Each walker joins the ids into one string — comparable evidence instead of scrolling console output. */
    static String walkWithIndex(List<Transaction> statement) {
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < statement.size(); i++) {
            ids.append(statement.get(i).getId()).append(' ');
        }
        return ids.toString().trim();
    }

    static String walkForEach(List<Transaction> statement) {
        StringBuilder ids = new StringBuilder();
        for (Transaction t : statement) {
            ids.append(t.getId()).append(' ');
        }
        return ids.toString().trim();
    }

    static String walkIterator(List<Transaction> statement) {
        StringBuilder ids = new StringBuilder();
        Iterator<Transaction> it = statement.iterator();
        while (it.hasNext()) {
            ids.append(it.next().getId()).append(' ');
        }
        return ids.toString().trim();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
