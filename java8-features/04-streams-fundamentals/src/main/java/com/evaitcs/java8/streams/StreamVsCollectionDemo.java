package com.evaitcs.java8.streams;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * WHAT IS A STREAM? The one-sentence pitch:
 *
 *   A Collection STORES data. A Stream COMPUTES over data, on demand, as it is pulled.
 *
 * This distinction is the foundation of the whole course — module 05 rewrites real
 * OOP code as pipelines, and none of that makes sense until this clicks.
 */
public class StreamVsCollectionDemo {

    public static void main(String[] args) {

        banner("1. A collection stores, a stream computes");

        List<Transaction> statement = Transaction.sampleStatement();
        // The list already holds every element in memory. size() is a stored FACT —
        // nobody has to look at the data to answer it.
        System.out.println("list.size() = " + statement.size() + "   <- stored, instantly known");
        System.out.println("list.get(0) = " + statement.get(0));

        Stream<Transaction> stream = statement.stream();
        // TRAP: students immediately look for stream.size() or stream.get(0).
        // Neither exists — a Stream has NO storage. It is a one-shot recipe for
        // producing elements when something asks for them.
        System.out.println("A Stream has no size() and no get(i) — it computes, it does not store.");

        // The payoff of "no storage": an INFINITE stream is perfectly legal,
        // because the elements only exist when pulled, one at a time.
        Stream<Integer> naturals = Stream.iterate(1, n -> n + 1);
        List<Integer> firstFive = naturals.limit(5).collect(Collectors.toList());
        System.out.println("first 5 of an infinite stream: " + firstFive
                + "   <- no collection could hold this; a stream never has to");

        banner("2. External iteration — YOU drive the loop");

        // The pre-Java-8 way: you write the for, you decide how to walk, you say STOP.
        // Control lives in YOUR code. This is called EXTERNAL iteration.
        BigDecimal totalIn = BigDecimal.ZERO;
        for (Transaction t : statement) {
            if (t.isDeposit()) {
                totalIn = totalIn.add(t.getAmount());
            }
        }
        System.out.println("for-loop  : total deposits = " + totalIn);

        banner("3. Internal iteration — the LIBRARY drives, you describe");

        // The stream way: you describe WHAT you want (keep deposits, take amounts, add them).
        // The library owns the loop. This is INTERNAL iteration — you handed over the steering wheel.
        BigDecimal totalInStreams = statement.stream()
                .filter(Transaction::isDeposit)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("pipeline  : total deposits = " + totalInStreams);

        banner("4. Same answer, different ownership");

        System.out.println("equal? " + (totalIn.compareTo(totalInStreams) == 0));
        System.out.println("Why give up control of the loop? Because the library can now:");
        System.out.println("  - run NOTHING until a result is demanded (laziness — demo 4)");
        System.out.println("  - STOP early once the answer is known (short-circuiting — demo 5)");
        System.out.println("  - split the work across threads safely (parallel — module 07)");
        System.out.println("Module 05 is entirely this move: imperative loops -> declarative pipelines.");
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
