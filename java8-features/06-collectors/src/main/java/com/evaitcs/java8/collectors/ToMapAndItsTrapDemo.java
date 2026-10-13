package com.evaitcs.java8.collectors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * toMap: the lookup-table builder — and the exception everyone hits in their first week.
 *
 * THE ONE SENTENCE: toMap(keyMapper, valueMapper) turns a stream into a Map, and because a
 * Map physically cannot hold two values under one key, toMap REFUSES TO GUESS when two
 * elements produce the same key — it throws IllegalStateException instead.
 *
 * The fix (a merge function) is not a workaround; it is you finally STATING the business
 * rule: which value wins, or how the two combine.
 */
public class ToMapAndItsTrapDemo {

    public static void main(String[] args) {
        List<Customer> customers = SampleBank.customers();

        banner("1. The happy path: unique keys — id -> name");

        // keyMapper + valueMapper. Customer ids are unique in SampleBank, so this is safe.
        // The result is the classic lookup table: idToName.get("C3") instead of a search loop.
        Map<String, String> idToName = customers.stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        System.out.println("idToName = " + idToName);
        System.out.println("lookup idToName.get(\"C3\") = " + idToName.get("C3"));

        banner("2. THE TRAP: city -> name, but THREE customers live in Austin");

        try {
            Map<String, String> cityToName = customers.stream()
                    .collect(Collectors.toMap(Customer::getCity, Customer::getName));
            System.out.println(cityToName); // never reached — the second Austin explodes first
        } catch (IllegalStateException e) {
            System.out.println("BOOM -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("Ada, Grace and Katherine all produce the key \"Austin\".");
            System.out.println("toMap will NOT guess which one you meant — it fails loudly.");
            System.out.println("(Loud is a FEATURE: silently dropping customers is far worse.)");
        }

        banner("3. The fix: a merge function states which value wins");

        // (a, b) -> a keeps the FIRST value seen for a key; (a, b) -> b keeps the LAST.
        // The fourth argument is the Map factory — LinkedHashMap keeps insertion order so
        // the printout is deterministic on the projector (plain toMap gives a HashMap).
        Map<String, String> firstWins = customers.stream()
                .collect(Collectors.toMap(
                        Customer::getCity, Customer::getName,
                        (a, b) -> a,                    // merge: city seen again -> keep what's there
                        LinkedHashMap::new));
        Map<String, String> lastWins = customers.stream()
                .collect(Collectors.toMap(
                        Customer::getCity, Customer::getName,
                        (a, b) -> b,                    // merge: city seen again -> overwrite
                        LinkedHashMap::new));
        System.out.println("(a,b)->a  first wins : " + firstWins);
        System.out.println("(a,b)->b  last wins  : " + lastWins);

        // The merge function doesn't have to PICK — it can COMBINE. Counting per city:
        // every customer is worth 1, and duplicates ADD. This is EXACTLY module 05's
        // TaskCount containsKey/get/put dance — the merge function IS its else branch and
        // Integer::sum IS its get-plus-1. (GroupingByDemo shrinks it further, to ONE line.)
        Map<String, Integer> countByCity = customers.stream()
                .collect(Collectors.toMap(
                        Customer::getCity, c -> 1, Integer::sum, LinkedHashMap::new));
        System.out.println("c->1 + Integer::sum  : " + countByCity + "   <- TaskCount as one expression");

        banner("4. Bonus trap: toMap says NO to null VALUES");

        try {
            // A HashMap happily stores null values — but toMap is built on Map.merge,
            // and merge() forbids null. So a valueMapper that can return null blows up
            // with NullPointerException instead of producing a map with nulls in it.
            Map<String, String> youngOnly = customers.stream()
                    .collect(Collectors.toMap(
                            Customer::getId,
                            c -> c.getAge() < 30 ? c.getName() : null));
            System.out.println(youngOnly); // never reached
        } catch (NullPointerException e) {
            System.out.println("BOOM -> NullPointerException (from Map.merge under the hood)");
            System.out.println("The very first customer (Ada, 36) produced a null VALUE.");
            System.out.println("Java 8 workaround: filter the unwanted elements out BEFORE");
            System.out.println("collecting, so the valueMapper can never return null.");
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
