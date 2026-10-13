package com.evaitcs.java8.optional;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 03-optional compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 03-optional compile exec:java "-Dexec.mainClass=com.evaitcs.java8.optional.OrElseVsOrElseGetDemo"
 */
public class OptionalDemos {

    public static void main(String[] args) {
        section("TheNullProblemDemo — the billion dollar mistake, and the ambiguity problem");
        TheNullProblemDemo.main(args);

        section("CreatingOptionalsDemo — of / ofNullable / empty, and when to use which");
        CreatingOptionalsDemo.main(args);

        section("OrElseVsOrElseGetDemo — the eager-argument trap, orElseThrow, and dressed-up null checks");
        OrElseVsOrElseGetDemo.main(args);

        section("ChainingDemo — map vs flatMap, filter, ifPresent: pipelines that short-circuit");
        ChainingDemo.main(args);

        section("OptionalDesignRulesDemo — where Optional belongs, and before/after side by side");
        OptionalDesignRulesDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
