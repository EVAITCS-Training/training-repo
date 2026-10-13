package com.evaitcs.java8.collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 06-collectors test
 * Each test proves ONE claim and prints the evidence it asserts, so the test run
 * doubles as a live demo. This class covers the machinery and the simple collectors;
 * GroupingByAndPartitioningTest covers the two report builders.
 */
class CollectorsBasicsTest {

    private final List<Customer> customers = SampleBank.customers();
    private final List<Transaction> transactions = SampleBank.transactions();

    @Test
    @DisplayName("toList keeps duplicates in encounter order; toSet keeps only distinct cities")
    void toListVsToSet() {
        List<String> list = customers.stream().map(Customer::getCity).collect(Collectors.toList());
        Set<String> set = customers.stream().map(Customer::getCity).collect(Collectors.toSet());

        System.out.println("toList : " + list);
        System.out.println("toSet  : " + set);

        assertEquals(Arrays.asList("Austin", "Austin", "Denver", "Austin", "Seattle", "Seattle"), list,
                "toList keeps EVERY element, in encounter order");
        assertEquals(3, set.size(), "Austin x3 and Seattle x2 collapse to one entry each");
        assertTrue(set.containsAll(Arrays.asList("Austin", "Denver", "Seattle")),
                "what survives is exactly the distinct cities");
    }

    @Test
    @DisplayName("toCollection names the container: TreeSet sorts, LinkedHashSet keeps first-seen order")
    void toCollectionGivesControl() {
        Set<String> sorted = customers.stream().map(Customer::getCity)
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> firstSeen = customers.stream().map(Customer::getCity)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        System.out.println("TreeSet       : " + sorted);
        System.out.println("LinkedHashSet : " + firstSeen);

        assertEquals(Arrays.asList("Austin", "Denver", "Seattle"), new ArrayList<>(sorted),
                "TreeSet iterates in natural (alphabetical) order");
        assertEquals(Arrays.asList("Austin", "Denver", "Seattle"), new ArrayList<>(firstSeen),
                "encounter order: Austin first seen at C1, Denver at C3, Seattle at C5");
    }

    @Test
    @DisplayName("THE TRAP: toMap with duplicate keys throws IllegalStateException")
    void toMapDuplicateKeyThrows() {
        IllegalStateException boom = assertThrows(IllegalStateException.class, () ->
                customers.stream().collect(Collectors.toMap(Customer::getCity, Customer::getName)));

        System.out.println("caught: " + boom.getClass().getSimpleName() + ": " + boom.getMessage());
        assertTrue(boom.getMessage().startsWith("Duplicate key"),
                "the message names the problem — three customers share the key Austin");
    }

    @Test
    @DisplayName("the fix: a merge function states which value wins (or how values combine)")
    void toMapMergeFunctionFixesTheTrap() {
        Map<String, String> firstWins = customers.stream()
                .collect(Collectors.toMap(Customer::getCity, Customer::getName,
                        (a, b) -> a, LinkedHashMap::new));
        Map<String, Integer> counts = customers.stream()
                .collect(Collectors.toMap(Customer::getCity, c -> 1,
                        Integer::sum, LinkedHashMap::new));

        System.out.println("(a,b)->a        : " + firstWins);
        System.out.println("c->1, sum merge : " + counts);

        assertEquals("Ada Lovelace", firstWins.get("Austin"), "the first Austin customer seen wins");
        assertEquals(Arrays.asList("Austin", "Denver", "Seattle"), new ArrayList<>(counts.keySet()),
                "LinkedHashMap keeps first-seen order — deterministic projector output");
        assertEquals(Integer.valueOf(3), counts.get("Austin"));
        assertEquals(Integer.valueOf(1), counts.get("Denver"));
        assertEquals(Integer.valueOf(2), counts.get("Seattle"));
    }

    @Test
    @DisplayName("toMap forbids null VALUES (Map.merge under the hood) — NullPointerException")
    void toMapRejectsNullValues() {
        NullPointerException boom = assertThrows(NullPointerException.class, () ->
                customers.stream().collect(Collectors.toMap(
                        Customer::getId,
                        c -> c.getAge() < 30 ? c.getName() : null)));

        System.out.println("caught: " + boom.getClass().getName());
        System.out.println("Ada is 36, so the very FIRST mapped value is null and merge() refuses it.");
        System.out.println("A plain HashMap would have accepted the null — toMap is stricter.");
    }

    @Test
    @DisplayName("summarizingInt agrees with counting/summingInt/averagingInt — one pass, five numbers")
    void summarizingAgreesWithIndividualCollectors() {
        IntSummaryStatistics stats = customers.stream()
                .collect(Collectors.summarizingInt(Customer::getAge));
        long count = customers.stream().collect(Collectors.counting());
        int sum = customers.stream().collect(Collectors.summingInt(Customer::getAge));
        double avg = customers.stream().collect(Collectors.averagingInt(Customer::getAge));

        System.out.println("stats: " + stats);

        assertEquals(count, stats.getCount(), "same count as counting()");
        assertEquals(sum, stats.getSum(), "same sum as summingInt()");
        assertEquals(avg, stats.getAverage(), 1e-9, "same average as averagingInt()");
        assertEquals(24, stats.getMin(), "Katherine is the youngest");
        assertEquals(55, stats.getMax(), "Linus is the oldest");
        assertEquals(213, sum, "hand-checked: 36+28+41+24+55+29");
        assertEquals(35.5, avg, 1e-9);
    }

    @Test
    @DisplayName("no summingBigDecimal in Java 8: mapping+reducing equals the plain accumulator loop")
    void bigDecimalSumNeedsMappingPlusReducing() {
        BigDecimal loop = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            loop = loop.add(t.getAmount());
        }
        BigDecimal streamed = transactions.stream()
                .collect(Collectors.mapping(Transaction::getAmount,
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)));

        System.out.println("accumulator loop   : " + loop);
        System.out.println("mapping + reducing : " + streamed);

        assertEquals(0, loop.compareTo(streamed), "compareTo — BigDecimal.equals would compare scale too");
        assertEquals(0, streamed.compareTo(new BigDecimal("41231.01")),
                "hand-checked total — the same 41231.01 module 05's TaskSum proved");
        assertEquals(2, streamed.scale(), "every amount had scale 2, so the exact sum does too");
    }

    @Test
    @DisplayName("joining builds the exact statement line for account A1")
    void joiningBuildsStatementLine() {
        String statement = transactions.stream()
                .filter(t -> t.getAccountId().equals("A1"))
                .map(t -> t.getDescription() + " " + (t.isDeposit() ? "+" : "-") + t.getAmount())
                .collect(Collectors.joining(" | ", "Statement A1 -> [", "]"));

        System.out.println(statement);

        assertEquals("Statement A1 -> [Payroll +2500.00 | Rent -1200.00 | Cash withdrawal -2600.00]",
                statement, "A1's three transactions, in stream order, glued with prefix and suffix");
    }

    @Test
    @DisplayName("collectingAndThen: a frozen list rejects add(); a count becomes a formatted String")
    void collectingAndThenFinishesTheJob() {
        List<String> frozen = customers.stream().map(Customer::getName)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        String summary = transactions.stream()
                .collect(Collectors.collectingAndThen(Collectors.counting(),
                        n -> n + " transactions processed"));

        System.out.println("frozen size: " + frozen.size() + "; summary: \"" + summary + "\"");

        assertEquals(6, frozen.size(), "the finisher changed the WRAPPER, not the content");
        assertThrows(UnsupportedOperationException.class, () -> frozen.add("nope"),
                "the Java 8 stand-in for toUnmodifiableList() really is unmodifiable");
        assertEquals("15 transactions processed", summary,
                "the finisher's return type becomes the collect() result type");
    }

    @Test
    @DisplayName("reduce and the reducing collector agree — same value, different machine")
    void reduceVsCollectAgree() {
        BigDecimal folded = transactions.stream().map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal collected = transactions.stream()
                .collect(Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add));

        String concat = transactions.stream().map(Transaction::getId)
                .reduce("", (acc, id) -> acc + id + " ");
        StringBuilder builder = transactions.stream().map(Transaction::getId)
                .collect(StringBuilder::new, (sb, id) -> sb.append(id).append(' '), StringBuilder::append);

        System.out.println("Stream.reduce       : " + folded);
        System.out.println("Collectors.reducing : " + collected);
        System.out.println("string reduce (+)   : " + concat.trim());
        System.out.println("StringBuilder       : " + builder.toString().trim());

        assertEquals(0, folded.compareTo(collected), "both fold to the same total");
        assertTrue(concat.contentEquals(builder),
                "same characters — but 15 fresh Strings vs 1 mutable buffer behind them");
    }
}
