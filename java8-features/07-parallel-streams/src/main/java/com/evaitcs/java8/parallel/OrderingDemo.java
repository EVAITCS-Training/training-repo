package com.evaitcs.java8.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Demo 4 — TRAP #2: ordering. "Parallel" does NOT automatically mean "scrambled".
 *
 * The rules, stated the way you'll say them in class:
 *
 *  - An ORDERED source (List, array, IntStream.range) keeps its ENCOUNTER ORDER — the
 *    order elements leave the source — in a parallel stream: collect(toList())
 *    reassembles the chunks back in order. The framework pays extra to preserve it,
 *    but it does.
 *  - forEach is the EXCEPTION: it hands results to your Consumer the MOMENT each worker
 *    finishes them. Fast, but in whatever order the threads happen to run.
 *  - forEachOrdered forces the encounter order back on — at the cost of making the
 *    workers wait for each other (you just paid for threads, then serialized the output).
 *
 * So: collect(toList()) == source order. ALWAYS. forEach == "whoever finished first".
 */
public class OrderingDemo {

    private static final List<String> AUDIT = Arrays.asList(
            "login", "view-balance", "transfer", "view-balance", "logout",
            "login", "transfer", "export-csv", "logout", "login",
            "view-balance", "change-pin");

    public static void main(String[] args) {
        System.out.println("source order (" + AUDIT.size() + " audit events):");
        System.out.println("  " + AUDIT + "\n");

        // ---- 1. forEach on a parallel stream: order is whoever-finishes-first ----
        System.out.println("1. parallel forEach — watch the order AND the thread names:");
        AUDIT.parallelStream()
                .forEach(event -> System.out.println("   [" + Thread.currentThread().getName() + "] " + event));
        System.out.println("   => same list, different order on every run. forEach gives NO ordering guarantee.\n");

        // ---- 2. forEachOrdered: encounter order restored, threads still did the work ----
        List<String> ordered = new ArrayList<>();
        // NOTE: we only add to `ordered` via forEachOrdered, which delivers one at a time
        // in encounter order — safe here, but see SharedStateTrapDemo for why mutating a
        // list from a parallel forEach is normally the WRONG move (delivery is serialized,
        // but this is still a teaching shortcut, not production style).
        AUDIT.parallelStream().forEachOrdered(ordered::add);
        System.out.println("2. parallel forEachOrdered:");
        System.out.println("   " + ordered);
        System.out.println("   => equals source order? " + ordered.equals(AUDIT) + "\n");

        // ---- 3. collect(toList()): order preserved AND no shared-state games ----
        List<String> collected = AUDIT.parallelStream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        System.out.println("3. parallel map(...).collect(toList()):");
        System.out.println("   " + collected);
        System.out.println("   => uppercased, still in encounter order? "
                + equalsIgnoreCaseWise(collected, AUDIT));
        System.out.println("\n=> RULE: ordered source + collect  = order kept.");
        System.out.println("        ordered source + forEach   = order LOST.");
    }

    private static boolean equalsIgnoreCaseWise(List<String> upper, List<String> source) {
        if (upper.size() != source.size()) {
            return false;
        }
        for (int i = 0; i < upper.size(); i++) {
            if (!upper.get(i).equals(source.get(i).toUpperCase())) {
                return false;
            }
        }
        return true;
    }
}
