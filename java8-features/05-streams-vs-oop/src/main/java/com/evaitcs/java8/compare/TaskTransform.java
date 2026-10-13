package com.evaitcs.java8.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TASK 2 — "names of all customers, uppercased".
 *
 * Now the task isn't SELECTING elements (that was filter) — it's CHANGING them.
 * One Customer goes in, one String comes out. The type of the collection changes,
 * and that type change is exactly what map() expresses.
 *
 * TEACHING POINT: map = "transform each". No bookkeeping: you never create the
 * output list, never decide where to put the next element, never touch a counter.
 * You write the transformation for ONE element and the stream applies it to all.
 */
public class TaskTransform {

    /*
     * LOOP ANATOMY:
     *
     *   (1) List<String> names = new ArrayList<>();   <- output collection, by hand
     *   (2) for (Customer c : customers) {            <- iteration, by hand
     *   (3)     names.add(c.getName().toUpperCase()); <- THE TRANSFORMATION (the only real line)
     *   (4) }
     *   (5) return names;
     *
     * Trap to name out loud: line (3) both transforms AND places the result. Two jobs
     * fused into one statement — which is why a bug like "added the raw name, forgot
     * toUpperCase()" or "added it twice" is easy to write and hard to spot in review.
     */
    public static List<String> imperative(List<Customer> customers) {
        List<String> names = new ArrayList<>();        // (1)
        for (Customer c : customers) {                 // (2)
            names.add(c.getName().toUpperCase());      // (3) transform + place, fused
        }
        return names;                                  // (5)
    }

    /*
     * Same anatomy, mapped one-to-one:
     *   the transformation (3) -> map(Function)        "turn each element into something else"
     *   the list + add (1)(3)(5) -> collect(toList())  "gather the results"
     *   the for (2) -> GONE again. Internal iteration, same as TaskFilter.
     *
     * map() is typed: Function<Customer, String> here. The compiler CHECKS that
     * "one customer in, one string out" — the element count can't drift, something
     * the imperative loop never guaranteed (you could forget the add entirely).
     */
    public static List<String> withStreams(List<Customer> customers) {
        return customers.stream()
                .map(c -> c.getName().toUpperCase())
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        List<Customer> customers = SampleBank.customers();

        List<String> imperative = imperative(customers);
        List<String> streams = withStreams(customers);

        System.out.println("QUESTION: what are all customer names, uppercased?");
        System.out.println();
        System.out.println("IMPERATIVE (for + add):        " + imperative);
        System.out.println("STREAMS    (map + collect):    " + streams);
        System.out.println();
        System.out.println("MATCH? " + imperative.equals(streams));
        System.out.println("POINT: map means 'transform each' — one element in, one element out.");
        System.out.println("       You write the transformation for ONE customer; the stream repeats it.");
    }
}
