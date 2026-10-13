package com.evaitcs.java8.compare;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TASK 5 — "how many customers per city".
 *
 * This is a GROUPING reduction: the result isn't a number or a list, it's a MAP
 * from a key (city) to a count. The imperative version is the containsKey/get/put
 * dance every Java developer has hand-written a hundred times.
 *
 * TEACHING POINT: eight lines of plumbing. This is the one task where the imperative
 * version isn't just "a bit longer" — it's a whole idiom you have to get exactly right,
 * and getting it wrong (forgetting the else, unboxing a null Integer) is a rite of
 * passage. Module 06 reduces ALL of it to a single collector.
 *
 * IMPORTANT: we deliberately do NOT use Collectors.groupingBy here — that's module 06's
 * reveal. This module shows the plumbing so students FEEL the relief later.
 */
public class TaskCount {

    /*
     * LOOP ANATOMY — the containsKey/get/put dance:
     *
     *   (1) Map<String, Integer> counts = new HashMap<>();  <- the accumulator, a MAP this time
     *   (2) for (Customer c : customers) {                  <- iteration, by hand
     *   (3)     String city = c.getCity();                  <- the grouping key
     *   (4)     if (counts.containsKey(city)) {             <- "have I seen this city yet?"
     *   (5)         counts.put(city, counts.get(city) + 1); <- yes: read old, add one, write back
     *   (6)     } else {
     *   (7)         counts.put(city, 1);                     <- no: seed it at 1
     *   (8)     }
     *   (9) }
     *  (10) return counts;
     *
     * That if/else is pure plumbing — it exists only because a HashMap can't know that
     * "a missing key counts as zero". Traps to name out loud:
     *   - Write line (5) without the containsKey check and counts.get(city) returns null,
     *     then `null + 1` unboxes to a NullPointerException. This is THE classic bug here.
     *   - The getOrDefault one-liner (shown below) fixes that, but you still hand-write
     *     the loop, the key extraction, and the put. It's shorter plumbing, still plumbing.
     */
    public static Map<String, Integer> imperative(List<Customer> customers) {
        Map<String, Integer> counts = new LinkedHashMap<>();   // (1) Linked* = deterministic print order
        for (Customer c : customers) {                       // (2)
            String city = c.getCity();                       // (3) the grouping key
            if (counts.containsKey(city)) {                  // (4) seen this city before?
                counts.put(city, counts.get(city) + 1);      // (5) yes: increment
            } else {
                counts.put(city, 1);                         // (7) no: seed at 1
            }
        }
        return counts;                                       // (10)
    }

    /**
     * The SAME dance, modernized with getOrDefault — included so the teacher can show
     * that even Java 8's nicer Map methods only shrink the plumbing, they don't remove it.
     * Still a loop, still a manual key, still a manual put. Point at how the if/else is
     * gone but the SHAPE of the work is identical.
     */
    public static Map<String, Integer> imperativeWithGetOrDefault(List<Customer> customers) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Customer c : customers) {
            counts.put(c.getCity(), counts.getOrDefault(c.getCity(), 0) + 1);
        }
        return counts;
    }

    /*
     * The stream version still needs a collector to build the map — but look at what
     * we WRITE versus what the library does. We state three things:
     *   - the grouping key          : Customer::getCity
     *   - what one element counts as: 1  (a constant — every customer is "one")
     *   - how to combine two counts : Integer::sum  (the merge function for key collisions)
     *
     *   Collectors.groupingBy( key, Collectors.counting() )  <- MODULE 06. Not used here
     *                                                            on purpose; this module
     *                                                            earns it with plumbing first.
     *
     * We use toMap(...) instead so the "collect into a map" idea is visible NOW, while
     * saving the elegant groupingBy reveal for the collectors module. toMap with a merge
     * function is the honest stream equivalent of the containsKey/put dance.
     *
     * LinkedHashMap in both: keeps first-seen city order so the two maps print identically
     * and equals() on maps ignores order anyway (Map.equals is order-independent).
     */
    public static Map<String, Integer> withStreams(List<Customer> customers) {
        return customers.stream()
                .collect(Collectors.toMap(
                        Customer::getCity,        // key: which bucket
                        c -> 1,                   // value: each customer counts as 1
                        Integer::sum,             // merge: same city twice -> add the counts
                        LinkedHashMap::new));     // map factory: keep insertion order
    }

    public static void main(String[] args) {
        List<Customer> customers = SampleBank.customers();

        Map<String, Integer> imperative = imperative(customers);
        Map<String, Integer> streams = withStreams(customers);

        System.out.println("QUESTION: how many customers live in each city?");
        System.out.println();
        System.out.println("IMPERATIVE (HashMap + containsKey/get/put dance): " + imperative);
        System.out.println("STREAMS    (toMap with a merge function):          " + streams);
        System.out.println();
        System.out.println("MATCH? " + imperative.equals(streams));
        System.out.println("POINT: that if/else is 8 lines of plumbing you must get exactly right.");
        System.out.println();
        System.out.println("  >>> TEASER: module 06 does this in ONE line:");
        System.out.println("      Collectors.groupingBy(Customer::getCity, Collectors.counting())");
        System.out.println("      We're not allowed to show it yet. You have to earn it. <<<");
    }
}
