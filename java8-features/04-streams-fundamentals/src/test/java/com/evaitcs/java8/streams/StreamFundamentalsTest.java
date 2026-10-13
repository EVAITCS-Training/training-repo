package com.evaitcs.java8.streams;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 04-streams-fundamentals test
 * Each test proves exactly one behavior and prints the evidence it asserts,
 * so the test run doubles as a demo.
 */
class StreamFundamentalsTest {

    private final List<Transaction> statement = Transaction.sampleStatement();

    @Test
    @DisplayName("LAZINESS: without a terminal op, nothing executes — with one, it does")
    void nothingExecutesWithoutTerminalOp() {
        AtomicInteger filterCalls = new AtomicInteger();
        AtomicInteger mapCalls = new AtomicInteger();

        // Build the full pipeline and hold it in a variable. No terminal op yet.
        Stream<String> pipeline = statement.stream()
                .filter(t -> { filterCalls.incrementAndGet(); return t.isDeposit(); })
                .map(t -> { mapCalls.incrementAndGet(); return t.getDescription(); });

        System.out.println("after building the pipeline : filter=" + filterCalls.get() + " map=" + mapCalls.get());
        assertEquals(0, filterCalls.get(), "filter must NOT run before the terminal op");
        assertEquals(0, mapCalls.get(), "map must NOT run before the terminal op");

        List<String> result = pipeline.collect(Collectors.toList());
        System.out.println("after collect()             : filter=" + filterCalls.get() + " map=" + mapCalls.get()
                + " result=" + result);
        assertEquals(statement.size(), filterCalls.get(), "filter sees every element");
        assertEquals(3, mapCalls.get(), "map only sees the 3 deposits that passed filter");
    }

    @Test
    @DisplayName("PIPELINING not STAGING: each element flows through the whole chain before the next")
    void elementsFlowOneAtATimeThroughWholeChain() {
        List<String> log = new ArrayList<>();

        statement.stream()
                .filter(t -> { log.add("filter " + t.getId()); return t.isDeposit(); })
                .map(t -> { log.add("map " + t.getId()); return t.getDescription(); })
                .forEach(d -> log.add("use " + d));

        log.forEach(System.out::println);

        // If streams were staged, ALL filters would run before ANY map.
        // They are not: T-101 is mapped (and used) before T-102 is even filtered.
        assertTrue(log.indexOf("map T-101") < log.indexOf("filter T-102"),
                "T-101 must reach map before T-102 reaches filter");
        assertEquals("filter T-101", log.get(0), "the very first event is filtering the FIRST element");
    }

    @Test
    @DisplayName("SHORT-CIRCUIT: findFirst on an INFINITE stream terminates after few pulls")
    void shortCircuitTamesInfiniteStream() {
        AtomicInteger examined = new AtomicInteger();

        Optional<Integer> firstMultipleOf7 = Stream.iterate(1, n -> n + 1)
                .filter(n -> { examined.incrementAndGet(); return n % 7 == 0; })
                .findFirst();

        System.out.println("first multiple of 7 = " + firstMultipleOf7.get()
                + ", elements examined = " + examined.get());
        assertEquals(Integer.valueOf(7), firstMultipleOf7.get());
        assertEquals(7, examined.get(), "must stop pulling the moment the first hit is found");
        // The claim being proven: an infinite source + short-circuiting terminal = finite work.
    }

    @Test
    @DisplayName("SHORT-CIRCUIT: anyMatch stops pulling as soon as the answer is known")
    void anyMatchStopsEarly() {
        AtomicInteger examined = new AtomicInteger();

        boolean anyBigDeposit = statement.stream()
                .filter(t -> { examined.incrementAndGet(); return t.isDeposit(); })
                .anyMatch(t -> t.getAmount().compareTo(new BigDecimal("1000")) > 0);

        System.out.println("anyMatch(deposit > 1000) = " + anyBigDeposit
                + ", elements examined = " + examined.get() + " of " + statement.size());
        assertTrue(anyBigDeposit);
        // T-101 Payroll (2500, deposit) already satisfies it — no need to look at the rest.
        assertEquals(1, examined.get(), "anyMatch must cancel the pipeline after the first true");
    }

    @Test
    @DisplayName("ONE-SHOT: a consumed stream throws IllegalStateException on reuse")
    void streamIsOneShot() {
        Stream<Transaction> stream = statement.stream();
        long firstUse = stream.filter(Transaction::isDeposit).count();
        assertEquals(3, firstUse);

        IllegalStateException boom = assertThrows(IllegalStateException.class, stream::count);
        System.out.println("reuse threw: " + boom.getClass().getSimpleName() + ": \"" + boom.getMessage() + "\"");
        assertTrue(boom.getMessage().contains("already been operated upon or closed"),
                "the exact JDK message students will see in stack traces");

        // The fix: a fresh stream from the same source works — the LIST never wore out.
        long secondUse = statement.stream().filter(Transaction::isDeposit).count();
        System.out.println("fresh statement.stream() counts deposits again = " + secondUse);
        assertEquals(3, secondUse);
    }

    @Test
    @DisplayName("INTERMEDIATE ops return a NEW Stream and touch no data")
    void intermediateOpsBuildNewStages() {
        AtomicInteger predicateCalls = new AtomicInteger();
        Stream<Transaction> source = statement.stream();
        Stream<Transaction> filtered = source.filter(t -> { predicateCalls.incrementAndGet(); return t.isDeposit(); });

        assertNotSame(source, filtered, "filter must return a new stream object, not the source");
        assertEquals(0, predicateCalls.get(), "building the stage runs nothing");

        long deposits = filtered.count();
        System.out.println("filter stage built lazily; after count(): deposits=" + deposits
                + ", predicate ran " + predicateCalls.get() + " times");
        assertEquals(3, deposits);
        assertEquals(statement.size(), predicateCalls.get());
    }

    @Test
    @DisplayName("INTERNAL iteration computes the same answer as the external for-loop")
    void pipelineEqualsLoop() {
        BigDecimal withLoop = BigDecimal.ZERO;
        for (Transaction t : statement) {
            if (t.isDeposit()) {
                withLoop = withLoop.add(t.getAmount());
            }
        }

        BigDecimal withPipeline = statement.stream()
                .filter(Transaction::isDeposit)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("for-loop total = " + withLoop + " ; pipeline total = " + withPipeline);
        assertEquals(0, withLoop.compareTo(withPipeline), "same data, same answer — only the iteration style differs");
        assertEquals(0, new BigDecimal("2549.10").compareTo(withPipeline)); // payroll + interest + refund
    }

    @Test
    @DisplayName("limit() pulls exactly N elements from an infinite source")
    void limitTamesInfiniteSource() {
        AtomicInteger delivered = new AtomicInteger();

        // Counter sits in peek AFTER limit, so it counts exactly what the pipeline
        // is allowed to deliver downstream. (TRAP worth mentioning in class: counting
        // inside the iterate FUNCTION gives 5, not 6 — the seed never goes through f.)
        List<Integer> firstSixPowers = Stream.iterate(1, n -> n * 2)
                .limit(6)
                .peek(n -> delivered.incrementAndGet())
                .collect(Collectors.toList());

        System.out.println("iterate(1, n->n*2).limit(6) = " + firstSixPowers + ", delivered = " + delivered.get());
        assertEquals(6, firstSixPowers.size());
        assertEquals(6, delivered.get(), "limit must propagate upstream: no element beyond N is ever produced");
    }
}
