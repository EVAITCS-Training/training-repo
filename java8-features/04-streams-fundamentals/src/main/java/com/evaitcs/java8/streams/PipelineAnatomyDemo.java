package com.evaitcs.java8.streams;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PIPELINE ANATOMY — the shape every stream expression has:
 *
 *   SOURCE  ->  zero or more INTERMEDIATE ops  ->  exactly one TERMINAL op
 *
 * Intermediate: lazy, Stream -> Stream, they just add a stage to the recipe.
 * Terminal:     eager, Stream -> something-else, and it CONSUMES the stream.
 *
 * This is diagram (a) in the README — point at the diagram, then at this code.
 */
public class PipelineAnatomyDemo {

    public static void main(String[] args) {

        banner("1. The whole pipeline in one expression");

        // Read it aloud stage by stage while pointing at the projector:
        List<String> report = Transaction.sampleStatement().stream()   // SOURCE — where elements come from
                .filter(Transaction::isDeposit)                        // INTERMEDIATE — keep matching elements
                .map(Transaction::getDescription)                      // INTERMEDIATE — transform each element
                .sorted()                                              // INTERMEDIATE — reorder (stateful!)
                .collect(Collectors.toList());                         // TERMINAL — gather into a List

        System.out.println("deposits, alphabetically: " + report);
        System.out.println("Notice: four stages, ONE expression, no temporary lists in between.");

        banner("2. Intermediate ops return a NEW Stream — and compute nothing");

        Stream<Transaction> source = Transaction.sampleStatement().stream();
        Stream<Transaction> filtered = source.filter(Transaction::isDeposit);

        // filter() did not touch a single element. It wrapped the source in a new
        // stream object that REMEMBERS "apply this predicate when pulled".
        System.out.println("source == filtered ? " + (source == filtered)
                + "   <- filter built a NEW stage, it did not filter anything yet");

        // You can keep chaining forever; still nothing runs. This is why a stream
        // pipeline reads like a recipe: mixing ingredients != cooking the meal.
        Stream<String> descriptions = filtered
                .map(Transaction::getDescription)
                .sorted();
        System.out.println("chained map+sorted, still nothing computed. Recipe so far: "
                + "stream -> filter -> map -> sorted");

        banner("3. The TERMINAL op consumes the stream and produces a non-Stream");

        // The moment a terminal op appears, the whole recipe executes — and the
        // stream is USED UP. `descriptions` cannot be consumed twice (demo 6 proves it).
        long count = descriptions.count();
        System.out.println("count() = " + count + "   <- a long, NOT a Stream: terminal ops end the pipeline");
        System.out.println("Terminal ops you already know: forEach, count, collect, reduce, min/max, anyMatch, findFirst.");

        banner("4. The grammar rule to memorize");

        System.out.println("  SOURCE                 exactly one   (collection.stream(), Stream.of, iterate, generate...)");
        System.out.println("  INTERMEDIATE ops       zero or more  (lazy; each returns a new Stream)");
        System.out.println("  TERMINAL op            exactly one   (eager; returns non-Stream; consumes the stream)");
        System.out.println("TRAP: sorted() is the odd one out — it must see EVERY element before emitting ANY.");
        System.out.println("      Laziness still holds (nothing runs before the terminal op), but sorted");
        System.out.println("      buffers internally. We call that a STATEFUL intermediate op.");
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
