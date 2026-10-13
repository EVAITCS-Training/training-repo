package com.evaitcs.java8.streams;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SHORT-CIRCUITING — laziness pays off.
 *
 * Because elements are PULLED one at a time through the chain (demo 4), a terminal op
 * that already knows its answer can simply STOP PULLING. anyMatch, findFirst and limit
 * are short-circuiting; count, forEach, reduce are not — they must see everything.
 *
 * The killer demo: a SHORT-CIRCUITING terminal op makes an INFINITE stream finite.
 * This is diagram (c) in the README.
 */
public class ShortCircuitDemo {

    public static void main(String[] args) {

        banner("1. anyMatch on an INFINITE stream — and it still terminates");

        AtomicInteger examined = new AtomicInteger();
        boolean anyOverTen = Stream.iterate(1, n -> n + 1)
                // counter inside the terminal op's own predicate: it runs once per
                // element actually PULLED, which is exactly what we want to measure
                .anyMatch(n -> {
                    examined.incrementAndGet();
                    return n > 10;
                });

        System.out.println("anyMatch(n -> n > 10) on 1,2,3,... = " + anyOverTen);
        System.out.println("elements actually examined: " + examined.get()
                + "   <- it examined 1..11, got its 'true', and STOPPED PULLING");
        System.out.println("A collection API cannot do this: you'd have to build the list of ALL naturals first.");

        banner("2. findFirst stops at the first survivor");

        AtomicInteger tested = new AtomicInteger();
        Optional<Integer> firstMultipleOf7 = Stream.iterate(1, n -> n + 1)
                .filter(n -> {
                    tested.incrementAndGet();      // count how many elements the filter actually saw
                    return n % 7 == 0;
                })
                .findFirst();

        System.out.println("first multiple of 7 among all naturals = " + firstMultipleOf7.get());
        System.out.println("elements the filter examined: " + tested.get()
                + "   <- findFirst cancelled the infinite source after the first hit");
        // findFirst returns Optional (module 03!) — "there might be no first element"
        // is a fact about the data, so it is part of the type.
        System.out.println("findFirst returns Optional: 'no result' is a value, not a null surprise.");

        banner("3. limit() — the explicit 'pull no more than N'");

        AtomicInteger delivered = new AtomicInteger();
        String firstPowers = Stream.iterate(1, n -> n * 2)
                .limit(6)
                .peek(n -> delivered.incrementAndGet())  // peek AFTER limit: counts what actually flows downstream
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        System.out.println("iterate(1, n -> n*2).limit(6) = [" + firstPowers + "]");
        System.out.println("only " + delivered.get()
                + " elements ever flowed — limit propagates upstream: 'stop generating after 6'");
        System.out.println("(Subtle trap: counting inside the iterate FUNCTION would print 5 — the seed never goes through f.)");

        banner("4. The contrast: NON-short-circuiting ops must see everything");

        AtomicInteger counted = new AtomicInteger();
        long deposits = Transaction.sampleStatement().stream()
                .filter(t -> { counted.incrementAndGet(); return t.isDeposit(); })
                .count();
        System.out.println("count() over the finite statement examined " + counted.get()
                + " of " + Transaction.sampleStatement().size() + " elements — all of them, found "
                + deposits + " deposits.");
        System.out.println("count()/forEach()/reduce() can NEVER stop early. On an infinite stream");
        System.out.println("count() runs forever — I am NOT executing that line; it would still be");
        System.out.println("running at graduation. Rule: know which terminal ops short-circuit.");
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
