package com.evaitcs.collections.why;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 01-why-collections compile exec:java
 *
 * The order is the argument: first the pain (every wall an array has), then the design
 * that answers it (a family of interfaces with swappable implementations), then the
 * shared vocabulary that makes the family feel like ONE library — including the equals
 * engine underneath contains/remove that the Set and Map modules will cash in.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 01-why-collections compile exec:java "-Dexec.mainClass=com.evaitcs.collections.why.ArraysHitAWallDemo"
 */
public class WhyCollectionsDemos {

    public static void main(String[] args) {
        section("ArraysHitAWallDemo — fixed size, manual surgery, no behavior, covariance, the asList trap");
        ArraysHitAWallDemo.main(args);

        section("HierarchyDemo — Iterable/Collection/List/Set/Queue/Map: guarantees, not classes");
        HierarchyDemo.main(args);

        section("CollectionApiDemo — one vocabulary, and the equals engine under contains/remove");
        CollectionApiDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
