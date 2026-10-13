package com.evaitcs.java8.upgrades;

/**
 * Launcher for the whole module - runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 09-api-upgrades compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 09-api-upgrades compile exec:java "-Dexec.mainClass=com.evaitcs.java8.upgrades.ComputeIfAbsentStarDemo"
 */
public class ApiUpgradesDemos {

    public static void main(String[] args) {
        section("MapUpgradesDemo - getOrDefault, putIfAbsent, merge, replaceAll, forEach");
        MapUpgradesDemo.main(args);

        section("ComputeIfAbsentStarDemo - THE star of the Java 8 Map API");
        ComputeIfAbsentStarDemo.main(args);

        section("RemoveIfDemo - the CME trap, iterator.remove, then the one-liner");
        RemoveIfDemo.main(args);

        section("StringJoinerDemo - String.join and StringJoiner kill the setLength hack");
        StringJoinerDemo.main(args);

        section("ObjectsHelpersDemo - requireNonNull, equals/hash, Objects::nonNull");
        ObjectsHelpersDemo.main(args);

        section("QuickHitsDemo - Arrays.setAll and the Comparator recap");
        QuickHitsDemo.main(args);

        section("Base64Demo - java.util.Base64, new in Java 8");
        Base64Demo.main(args);

        System.out.println();
        System.out.println("That is the daily-driver tour - you now know Java 8.");
        System.out.println("See README.md for the full module-by-module checklist.");
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
