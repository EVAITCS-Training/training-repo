package com.evaitcs.collections.sets;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 04-set compile exec:java
 *
 * The order is the argument: first the contract (add() refuses duplicates, and
 * "duplicate" means equals), then the three engines holding the SAME data in three
 * orders, then TreeSet's special law (compareTo, never equals — the comparator
 * collapse and the BigDecimal 2.0/2.00 trap), then the algebra Sets exist for, then
 * nulls and the ordered-set vocabulary, and finally the corrupted set — the mutable
 * identity trap that module 05 replays on a HashMap.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 04-set compile exec:java "-Dexec.mainClass=com.evaitcs.collections.sets.NoDuplicatesDemo"
 */
public class SetDemos {

    public static void main(String[] args) {
        section("NoDuplicatesDemo — add() returns false; 'duplicate' means equals(), not ==");
        NoDuplicatesDemo.main(args);

        section("ThreeSetsSameDataDemo — one statement in HashSet / LinkedHashSet / TreeSet: three engines, three orders");
        ThreeSetsSameDataDemo.main(args);

        section("TreeSetOrderingDemo — compareTo, never equals: the comparator collapse and BigDecimal 2.0 vs 2.00");
        TreeSetOrderingDemo.main(args);

        section("SetAlgebraDemo — union / intersection / difference: addAll, retainAll, removeAll (they MUTATE)");
        SetAlgebraDemo.main(args);

        section("NullsAndNavigableDemo — one null is legal (hash Sets); TreeSet needs an order; views, not copies");
        NullsAndNavigableDemo.main(args);

        section("CorruptedSetDemo — mutate an element after insert: in the set, unreachable");
        CorruptedSetDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
