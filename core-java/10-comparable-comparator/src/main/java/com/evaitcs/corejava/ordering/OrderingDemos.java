package com.evaitcs.corejava.ordering;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 10-comparable-comparator compile exec:java
 *
 * The order is the argument: start from the runtime crash (Java cannot guess your
 * order), read the compareTo contract, give Transaction its ONE natural order,
 * then break out of the one-order limit with Comparators — custom orders, the
 * subtraction trap, chained tie-breaks, reversal and nulls — and close with a
 * single glimpse of the Java 8 API that replaces all the ceremony.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 10-comparable-comparator compile exec:java "-Dexec.mainClass=com.evaitcs.corejava.ordering.SubtractionTrapDemo"
 */
public class OrderingDemos {

    public static void main(String[] args) {
        section("SortingProblemDemo — sort without Comparable: ClassCastException at runtime");
        SortingProblemDemo.main(args);

        section("CompareToContractDemo — the sign is the contract; TreeSet uses compareTo, not equals");
        CompareToContractDemo.main(args);

        section("ComparableNaturalOrderDemo — the natural order lives INSIDE the class (exactly one)");
        ComparableNaturalOrderDemo.main(args);

        section("ComparatorCustomOrderDemo — custom orders OUTSIDE the class: same list, three ways");
        ComparatorCustomOrderDemo.main(args);

        section("SubtractionTrapDemo — (int)(a - b) overflows and the sign flips");
        SubtractionTrapDemo.main(args);

        section("ChainedOrderDemo — primary key + tie-breaks, the hand-written ladder");
        ChainedOrderDemo.main(args);

        section("ReverseAndNullsDemo — reverseOrder vs reverse; nulls need a policy");
        ReverseAndNullsDemo.main(args);

        section("Java8PreviewDemo — ONE glimpse of comparing/thenComparing/reversed (print only)");
        Java8PreviewDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
