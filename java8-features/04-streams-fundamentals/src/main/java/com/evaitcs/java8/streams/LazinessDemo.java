package com.evaitcs.java8.streams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * LAZINESS — the most important demo in this module. Two claims to prove on screen:
 *
 *   1. Nothing executes until a TERMINAL operation demands a result.
 *      Intermediate ops just build the recipe.
 *   2. When it DOES execute, elements flow one at a time through the WHOLE chain
 *      (pipelining) — not stage by stage over the whole collection (staging).
 *
 * We prove both with counters/logs inside the lambdas, because "trust me" is not
 * teaching. AtomicInteger because lambdas can only capture effectively-final locals
 * (module 01!) — the counter OBJECT never changes, its value does.
 */
public class LazinessDemo {

    public static void main(String[] args) {

        banner("1. Build a pipeline WITHOUT a terminal op — watch the counters");

        List<Transaction> statement = Transaction.sampleStatement();
        AtomicInteger filterCalls = new AtomicInteger();
        AtomicInteger mapCalls = new AtomicInteger();

        Stream<String> pipeline = statement.stream()
                .filter(t -> {
                    filterCalls.incrementAndGet();
                    return t.isDeposit();
                })
                .map(t -> {
                    mapCalls.incrementAndGet();
                    return t.getDescription();
                });

        System.out.println("pipeline built and stored in a variable...");
        System.out.println("  filter lambda ran " + filterCalls.get() + " times");
        System.out.println("  map    lambda ran " + mapCalls.get() + " times");
        System.out.println("ZERO. The filter and map are registered, not executed. Recipe written, meal not cooked.");

        banner("2. Now add the terminal op — everything runs");

        List<String> deposits = pipeline.collect(Collectors.toList());
        System.out.println("collect() returned " + deposits);
        System.out.println("  filter lambda ran " + filterCalls.get() + " times (every element was tested)");
        System.out.println("  map    lambda ran " + mapCalls.get() + " times (only the " + deposits.size()
                + " that passed filter were mapped)");
        System.out.println("map ran FEWER times than filter: rejected elements never reach later stages.");

        banner("3. Pipelining, not staging — each element goes all the way through first");

        // The common misconception: "filter processes ALL 6, THEN map processes the survivors".
        // If that were true the log below would show 6 filter lines, then map lines. Watch.
        List<String> log = new ArrayList<>();
        statement.stream()
                .filter(t -> { log.add("filter " + t.getId()); return t.isDeposit(); })
                .map(t -> { log.add("  map  " + t.getId()); return t.getDescription(); })
                .forEach(d -> log.add("    use  " + d));

        for (String line : log) {
            System.out.println(line);
        }
        System.out.println("The log INTERLEAVES: filter T-101, map T-101, use Payroll, filter T-102...");
        System.out.println("One element flows through the whole chain before the next one starts.");
        System.out.println("Why it matters: a 10-stage pipeline needs NO 10 intermediate collections —");
        System.out.println("memory stays flat, and short-circuiting (next demo) becomes possible.");

        banner("4. The takeaway sentence");

        System.out.println("Building a stream pipeline does nothing. The TERMINAL op is the on-switch.");
        System.out.println("(And a pipeline nobody terminates is dead code — the compiler won't even warn you.)");
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
