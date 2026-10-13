package com.evaitcs.java8.collectors;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * groupingBy — the star of the whole Collectors class.
 *
 * THE ONE SENTENCE: groupingBy(classifier) is SQL's GROUP BY as a single expression — it
 * splits the stream into buckets by a key, and a DOWNSTREAM collector decides what each
 * bucket becomes (a list by default; a count, a sum, a joined string, or another
 * groupingBy for nested reports).
 *
 * PROMISE KEPT: module 05's TaskCount hand-wrote the containsKey/get/put dance for
 * "how many customers per city" and teased:
 *     Collectors.groupingBy(Customer::getCity, Collectors.counting())
 *     "module 06 does this in ONE line. We're not allowed to show it yet."
 * That line is section 1 below — eight lines of plumbing become one.
 */
public class GroupingByDemo {

    public static void main(String[] args) {
        List<Customer> customers = SampleBank.customers();
        List<Account> accounts = SampleBank.accounts();

        banner("1. Single level: customers by city — the default downstream is toList()");

        Map<String, List<Customer>> byCity = customers.stream()
                .collect(Collectors.groupingBy(Customer::getCity));
        byCity.forEach((city, people) ->
                System.out.println("   " + city + " -> " + people.size() + " : " + people));

        // And THE promised line — TaskCount's entire imperative method in one expression:
        Map<String, Long> countByCity = customers.stream()
                .collect(Collectors.groupingBy(Customer::getCity, Collectors.counting()));
        System.out.println("the promise kept: " + countByCity);
        System.out.println("groupingBy creates ONLY the keys it sees — there is no Phoenix");
        System.out.println("entry with an empty list. (partitioningBy is DIFFERENT on exactly");
        System.out.println("this point — PartitioningByDemo.)");

        banner("2. Downstream collectors — what each bucket BECOMES");

        // mapping(): transform elements on the way INTO the bucket — names, not Customers.
        Map<String, List<String>> namesByCity = customers.stream()
                .collect(Collectors.groupingBy(
                        Customer::getCity,
                        Collectors.mapping(Customer::getName, Collectors.toList())));
        System.out.println("mapping(getName, toList()) : " + namesByCity);

        // summingInt(): bucket -> a number. Total age per city:
        Map<String, Integer> ageByCity = customers.stream()
                .collect(Collectors.groupingBy(Customer::getCity, Collectors.summingInt(Customer::getAge)));
        System.out.println("summingInt(getAge)         : " + ageByCity);

        System.out.println("Read the TYPES: Map<String, List<String>> vs Map<String, Integer> —");
        System.out.println("the downstream collector decides the value type of the result map.");
        System.out.println("For MONEY the downstream is mapping + reducing (SummarizingDemo §4).");

        banner("3. Multi-level: accounts by city, THEN by type — nested GROUP BY");

        // The join first: an Account knows its ownerId, not its owner's city. Build the
        // lookup table with toMap — ids are UNIQUE, so no duplicate-key trap here
        // (ToMapAndItsTrapDemo); this is exactly what toMap is for.
        Map<String, String> cityOfOwner = customers.stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getCity));

        // A groupingBy INSIDE a groupingBy: the inner one is just another downstream.
        // (The explicit (Account a) type helps Java 8's inference through two nested
        // generic calls — a fair price for a nested report.)
        Map<String, Map<Account.Type, Long>> byCityThenType = accounts.stream()
                .collect(Collectors.groupingBy(
                        (Account a) -> cityOfOwner.get(a.getOwnerId()),   // outer key: owner's city
                        Collectors.groupingBy(Account::getType,           // inner key: account type
                                Collectors.counting())));                 // innermost: how many
        byCityThenType.forEach((city, perType) ->
                System.out.println("   " + city + " -> " + perType));
        System.out.println("Look at Denver: SAVINGS only — no CHECKING key at all. Again:");
        System.out.println("groupingBy materializes only buckets that caught something.");
        System.out.println("The same trick nests three deep; whole reports write themselves.");

        banner("4. Sorted keys: pass a Map factory");

        // Three-arg groupingBy: classifier, mapFactory, downstream. A TreeMap sorts the
        // city keys alphabetically — the report prints in a stable, readable order.
        // (LinkedHashMap::new would keep encounter order instead.)
        Map<String, List<Customer>> sorted = customers.stream()
                .collect(Collectors.groupingBy(Customer::getCity, TreeMap::new, Collectors.toList()));
        System.out.println("keySet with TreeMap::new : " + sorted.keySet());
        System.out.println("The default map is a HashMap — fine for lookups, useless for");
        System.out.println("projectors. When the ORDER of the report matters, choose the map.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
