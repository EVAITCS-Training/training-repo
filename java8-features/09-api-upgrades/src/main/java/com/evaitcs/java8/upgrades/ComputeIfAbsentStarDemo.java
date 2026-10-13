package com.evaitcs.java8.upgrades;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * computeIfAbsent deserves its own demo class: it is THE star of the Java 8 Map API.
 *
 * The task every codebase has: build a Map&lt;String, List&lt;Transaction&gt;&gt; by hand -
 * "group transactions by city". Pre-Java-8 that is a five-line ritual everyone wrote
 * slightly differently. Java 8 collapses it to one line that reads like the requirement.
 */
public class ComputeIfAbsentStarDemo {

    public static void main(String[] args) {
        List<Transaction> statement = Transaction.sampleStatement();

        System.out.println("=== The OLD 5-line grouping ritual (Java 7) ===");
        Map<String, List<Transaction>> byCityOld = new TreeMap<>();
        for (Transaction t : statement) {
            String city = t.getCity();
            if (!byCityOld.containsKey(city)) {                  // 1. lookup #1
                byCityOld.put(city, new ArrayList<Transaction>()); // 2. create + insert
            }
            byCityOld.get(city).add(t);                          // 3. lookup #2, then add
        }
        printGrouped(byCityOld);

        System.out.println("=== The SAME grouping, Java 8 (one line) ===");
        Map<String, List<Transaction>> byCity = new TreeMap<>();
        for (Transaction t : statement) {
            // Read it out loud: "compute a value IF the key is ABSENT - otherwise just
            // hand me what is already there." Either way I get THE list, and I add to it.
            byCity.computeIfAbsent(t.getCity(), k -> new ArrayList<>()).add(t);
        }
        printGrouped(byCity);

        System.out.println("=== PROOF the mapping function runs ONLY when the key is absent ===");
        final int[] creations = { 0 };   // array trick to mutate a captured local (module 01!)
        Map<String, List<Transaction>> counted = new TreeMap<>();
        for (Transaction t : statement) {
            counted.computeIfAbsent(t.getCity(), k -> {
                creations[0]++;          // count how many lists we REALLY build
                return new ArrayList<>();
            });
        }
        System.out.println("   6 transactions, 3 cities -> mapping function ran "
                + creations[0] + " times (NOT 6). Present key = lambda never called.");

        System.out.println("=== TRAP #1: putIfAbsent builds the value EAGERLY; computeIfAbsent is LAZY ===");
        // byCity.putIfAbsent(city, new ArrayList<>()) allocates a fresh list on EVERY call,
        // even when the key is present and the list is thrown away. computeIfAbsent's lambda
        // only runs when the value is actually needed. Cheap values: putIfAbsent is fine.
        // Expensive values (lists, connections, big objects): computeIfAbsent.

        System.out.println("=== TRAP #2: the mapping function must NOT modify THE SAME map ===");
        // byCity.computeIfAbsent("Austin", k -> { byCity.put("Dallas", ...); return ...; });
        // The Javadoc calls this "undefined behavior": on HashMap you risk lost updates or a
        // corrupted internal state; on ConcurrentHashMap you risk DEADLOCK (the function runs
        // while the bin is locked). Rule for class: the lambda computes A NEW VALUE, full stop.

        System.out.println("=== BONUS: counting instead of collecting -> merge ===");
        Map<String, Integer> txnCountByCity = new TreeMap<>();
        for (Transaction t : statement) {
            txnCountByCity.merge(t.getCity(), 1, Integer::sum);  // "add 1 into the counter"
        }
        txnCountByCity.forEach((city, n) -> System.out.println("   " + city + " : " + n + " transactions"));
        // (In module 06 we did this whole job with Collectors.groupingBy/counting - here you
        //  see the machinery those collectors use underneath.)
    }

    private static void printGrouped(Map<String, List<Transaction>> grouped) {
        grouped.forEach((city, txns) -> {
            System.out.println("   " + city + " (" + txns.size() + "):");
            txns.forEach(t -> System.out.println("      " + t));
        });
    }
}
