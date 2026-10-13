package com.evaitcs.collections.iteration;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 03-iteration-and-iterators compile exec:java
 *
 * The order is the argument: first the three loop shapes (what you type), then the proof that
 * for-each is exactly an Iterator in disguise (what javac types), then the one safe removal
 * during a walk (the contract), then the alarm AND — more importantly — its silences (the
 * best-effort nature of fail-fast), and finally the iterator with a gearbox (ListIterator)
 * plus one line of legacy history (Enumeration).
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 03-iteration-and-iterators compile exec:java "-Dexec.mainClass=com.evaitcs.collections.iteration.IteratorRemoveDemo"
 */
public class IterationDemos {

    public static void main(String[] args) {
        section("ThreeLoopsDemo — indexed for, for-each, explicit Iterator: three spellings, one sequence");
        ThreeLoopsDemo.main(args);

        section("ForEachIsSugarDemo — what javac emits, driven by hand");
        ForEachIsSugarDemo.main(args);

        section("IteratorRemoveDemo — the one safe in-loop removal, and its once-per-next() contract");
        IteratorRemoveDemo.main(args);

        section("ConcurrentModificationDemo — the alarm, and the famous silences");
        ConcurrentModificationDemo.main(args);

        section("ListIteratorDemo — backward, set(), add(), and the Enumeration fossil");
        ListIteratorDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
