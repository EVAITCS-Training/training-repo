package com.evaitcs.java8.collectors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * collect() demystified: a MUTABLE REDUCTION.
 *
 * THE ONE SENTENCE: reduce folds elements into ONE immutable value; collect pours them
 * into ONE mutable container — a supplier makes the bucket, an accumulator drops elements
 * in, and a combiner merges buckets (which only parallel streams ever need — module 07).
 *
 * Every other demo in this module is a variation on this machinery, so we open the box
 * exactly once, here, and then use the ready-made Collectors.* factories with confidence.
 */
public class CollectAnatomyDemo {

    public static void main(String[] args) {

        banner("1. reduce folds into a VALUE; collect accumulates into a CONTAINER");

        // reduce: every step produces a NEW immutable BigDecimal — nothing is mutated,
        // the "answer so far" is a fresh value each time (module 05's TaskSum).
        BigDecimal folded = SampleBank.transactions().stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // collect: ONE list is created up front (the supplier), then MUTATED once per
        // element (the accumulator). The list IS the answer being built.
        List<Transaction> deposits = SampleBank.transactions().stream()
                .filter(Transaction::isDeposit)
                .collect(Collectors.toList());

        System.out.println("reduce  -> one value     : total of all 15 amounts = " + folded);
        System.out.println("collect -> one container : " + deposits.size() + " deposits gathered in a "
                + deposits.getClass().getSimpleName());
        System.out.println("WHY a mutable bucket for collections: appending to an ArrayList is O(1)");
        System.out.println("amortized, while 'immutable list building' would COPY the whole list at");
        System.out.println("every step. ReduceVsCollectDemo turns that claim into running code.");

        banner("2. The three moving parts — a collector built BY HAND");

        // Collector.of(supplier, accumulator, combiner) — the trio EVERY collector carries.
        // Accumulator and combiner print when called, so the class can SEE the machinery
        // work — and see that the combiner is never called on a sequential stream.
        Collector<String, List<String>, List<String>> namePicker = Collector.of(
                ArrayList::new,                       // SUPPLIER    : build the empty container
                (list, name) -> {                     // ACCUMULATOR : fold ONE element in (mutates!)
                    System.out.println("   accumulate: " + name);
                    list.add(name);
                },
                (left, right) -> {                    // COMBINER    : merge two containers
                    System.out.println("   combine: merging " + right + " into " + left);
                    left.addAll(right);
                    return left;
                });

        List<String> names = SampleBank.customers().stream()
                .map(Customer::getName)
                .collect(namePicker);

        System.out.println("collected: " + names);
        System.out.println("Count the prints: 'accumulate' ran 6 times, 'combine' ran ZERO.");
        System.out.println("The combiner only matters when there are multiple containers to");
        System.out.println("merge — that's parallel streams. Module 07 makes it fire.");

        banner("3. The ready-made ones: toList / toSet / toCollection");

        List<String> cities = SampleBank.customers().stream()
                .map(Customer::getCity)
                .collect(Collectors.toList());
        System.out.println("toList() : " + cities + "   <- encounter order, duplicates KEPT");

        Set<String> distinct = SampleBank.customers().stream()
                .map(Customer::getCity)
                .collect(Collectors.toSet());
        System.out.println("toSet()  : " + distinct + "   <- duplicates GONE, order GONE (HashSet = hash order)");

        // When "distinct but in first-seen order" or "distinct and sorted" is the actual
        // requirement, toSet() can't promise it — toCollection() names the container YOU want.
        Set<String> firstSeen = SampleBank.customers().stream()
                .map(Customer::getCity)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        System.out.println("toCollection(LinkedHashSet::new) : " + firstSeen + "   <- distinct, encounter order");

        Set<String> sorted = SampleBank.customers().stream()
                .map(Customer::getCity)
                .collect(Collectors.toCollection(TreeSet::new));
        System.out.println("toCollection(TreeSet::new)       : " + sorted + "   <- distinct, sorted");

        // TRAP to name out loud: toList()/toSet() guarantee NOTHING about the concrete type,
        // mutability, or ordering of what they return (today it happens to be ArrayList /
        // HashSet). Code that DEPENDS on the concrete type should say so explicitly:
        // toCollection(ArrayList::new). And this is Java 8 — there is no Stream.toList()
        // (16+) and no Collectors.toUnmodifiableList() (10+). Freezing a result is
        // collectingAndThen's job (JoiningDemo, section 3).
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
