package com.evaitcs.collections.sorting;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 07-sorting-and-collections-utility compile exec:java
 *
 * The order is the argument: first sorting and its stability guarantee (the tie you can
 * SEE surviving), then binarySearch and its two failure modes (the miss encoding, and
 * silent garbage on unsorted lists), then the small games (seeded shuffle, min/max,
 * frequency, disjoint), then the module's big trap — unmodifiableList is a live WINDOW,
 * not a copy — and finally the true immutables that have no original to leak through.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 07-sorting-and-collections-utility compile exec:java "-Dexec.mainClass=com.evaitcs.collections.sorting.UnmodifiableIsAViewDemo"
 */
public class SortingUtilityDemos {

    public static void main(String[] args) {
        section("StableSortDemo — TimSort is stable: the T-1001/T-1004 tie keeps arrival order");
        StableSortDemo.main(args);

        section("BinarySearchDemo — O(log n) on a SORTED list; the -(ip)-1 miss encoding; silent garbage otherwise");
        BinarySearchDemo.main(args);

        section("ShuffleAndGamesDemo — seeded shuffle replays; min/max/frequency/disjoint on statement data");
        ShuffleAndGamesDemo.main(args);

        section("UnmodifiableIsAViewDemo — the wrapper is a live window: writes throw, original changes show through");
        UnmodifiableIsAViewDemo.main(args);

        section("ImmutableHelpersDemo — emptyList/singletonList/nCopies: the true immutables, no original to leak");
        ImmutableHelpersDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
