package com.evaitcs.java8.compare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF — this is the module's core claim, made checkable:
 *
 *   For EVERY task, the imperative (loops, the code students already know) and the
 *   declarative (streams) implementations return EQUAL results.
 *
 * Run with:
 *   .\mvnw.cmd -pl 05-streams-vs-oop test
 *
 * Each test prints BOTH results before asserting equality, so the test run doubles
 * as a live side-by-side demo on the projector. If a student ever asks "but do they
 * REALLY do the same thing?", this class is the answer.
 */
class StreamsVsOopTest {

    private final List<Customer> customers = SampleBank.customers();
    private final List<Account> accounts = SampleBank.accounts();
    private final List<Transaction> transactions = SampleBank.transactions();

    @Test
    @DisplayName("TaskFilter: for+if+add == filter+collect (deposits over 100)")
    void filterResultsAreEqual() {
        List<Transaction> imperative = TaskFilter.imperative(transactions);
        List<Transaction> streams = TaskFilter.withStreams(transactions);

        System.out.println("TaskFilter — deposits over 100:");
        System.out.println("  IMPERATIVE: " + ids(imperative));
        System.out.println("  STREAMS   : " + ids(streams));

        assertEquals(imperative, streams, "both must return the same transactions, same order");
        assertEquals(5, streams.size(), "sample data has 5 deposits over 100 (T01, T05, T06, T10, T12)");
    }

    @Test
    @DisplayName("TaskTransform: for+add == map+collect (names uppercased)")
    void transformResultsAreEqual() {
        List<String> imperative = TaskTransform.imperative(customers);
        List<String> streams = TaskTransform.withStreams(customers);

        System.out.println("TaskTransform — names uppercased:");
        System.out.println("  IMPERATIVE: " + imperative);
        System.out.println("  STREAMS   : " + streams);

        assertEquals(imperative, streams);
        assertEquals("ADA LOVELACE", streams.get(0), "map transformed the FIRST customer's name");
    }

    @Test
    @DisplayName("TaskSum: accumulator loop == reduce(ZERO, ::add) (total of all amounts)")
    void sumResultsAreEqual() {
        BigDecimal imperative = TaskSum.imperative(transactions);
        BigDecimal streams = TaskSum.withStreams(transactions);

        System.out.println("TaskSum — total of all transaction amounts:");
        System.out.println("  IMPERATIVE: " + imperative.toPlainString());
        System.out.println("  STREAMS   : " + streams.toPlainString());

        // compareTo, not equals: BigDecimal.equals compares SCALE too, so 5 vs 5.00
        // would be "unequal". For money, compareTo == 0 is the correct notion of equal.
        assertEquals(0, imperative.compareTo(streams), "both must compute the same total");
        assertEquals(0, streams.compareTo(new BigDecimal("41231.01")), "hand-checked expected total");
    }

    @Test
    @DisplayName("TaskMax: bestSoFar loop == max(comparing) on a NON-EMPTY list")
    void maxResultsAreEqual() {
        Account imperative = TaskMax.imperative(accounts);
        Optional<Account> streams = TaskMax.withStreams(accounts);

        System.out.println("TaskMax — largest balance:");
        System.out.println("  IMPERATIVE: " + imperative);
        System.out.println("  STREAMS   : " + streams);

        assertTrue(streams.isPresent(), "non-empty list must produce a winner");
        assertEquals(imperative, streams.get(), "both must pick the same account");
        assertEquals("A4", streams.get().getId(), "A4 (31000.00) is the largest in the sample bank");
    }

    @Test
    @DisplayName("TaskMax edge case: null vs Optional.empty() — same answer, safer type")
    void maxHandlesEmptyList() {
        Account imperative = TaskMax.imperative(Collections.<Account>emptyList());
        Optional<Account> streams = TaskMax.withStreams(Collections.<Account>emptyList());

        System.out.println("TaskMax — EMPTY account list:");
        System.out.println("  IMPERATIVE returned: " + imperative + "   (a null the signature never mentions)");
        System.out.println("  STREAMS    returned: " + streams + "  (the type SAYS it may be absent)");

        assertNull(imperative, "the loop's 'no winner' is null");
        assertFalse(streams.isPresent(), "the stream's 'no winner' is Optional.empty()");
        // The bridge between the two worlds — how a test proves null == empty here:
        assertEquals(Optional.ofNullable(imperative), streams,
                "Optional.ofNullable(null) IS Optional.empty(): same meaning, better type");
    }

    @Test
    @DisplayName("TaskCount: containsKey/get/put dance == stream collector (customers per city)")
    void countResultsAreEqual() {
        Map<String, Integer> imperative = TaskCount.imperative(customers);
        Map<String, Integer> getOrDefault = TaskCount.imperativeWithGetOrDefault(customers);
        Map<String, Integer> streams = TaskCount.withStreams(customers);

        System.out.println("TaskCount — customers per city:");
        System.out.println("  IMPERATIVE (containsKey/get/put): " + imperative);
        System.out.println("  IMPERATIVE (getOrDefault variant):" + getOrDefault);
        System.out.println("  STREAMS    (toMap + merge):       " + streams);

        assertEquals(imperative, streams, "Map.equals ignores order — same keys, same counts");
        assertEquals(imperative, getOrDefault, "the modernized loop must agree with the classic dance");
        assertEquals(Integer.valueOf(3), streams.get("Austin"), "Ada + Grace + Katherine live in Austin");
        assertEquals(Integer.valueOf(1), streams.get("Denver"), "only Alan lives in Denver");
        assertEquals(Integer.valueOf(2), streams.get("Seattle"), "Linus + Barbara live in Seattle");
        assertEquals(3, streams.size(), "exactly three cities");
    }

    @Test
    @DisplayName("TaskChain: nested loops + join == filter.map.flatMap.map.reduce (under-30 balances)")
    void chainResultsAreEqual() {
        BigDecimal imperative = TaskChain.imperative(customers, accounts);
        BigDecimal streams = TaskChain.withStreams(customers, accounts);

        System.out.println("TaskChain — total balance of accounts owned by customers under 30:");
        System.out.println("  IMPERATIVE (nested loops): " + imperative.toPlainString());
        System.out.println("  STREAMS    (flatMap):      " + streams.toPlainString());

        assertEquals(0, imperative.compareTo(streams), "both must compute the same total");
        // Grace(A3 840.25) + Katherine(A5 150.75) + Barbara(A8 990.10) = 1981.10
        assertEquals(0, streams.compareTo(new BigDecimal("1981.10")), "hand-checked expected total");
    }

    @Test
    @DisplayName("TaskAnyMatch: early-return loop == anyMatch (suspicious > 2000)")
    void anyMatchResultsAreEqual() {
        boolean imperative = TaskAnyMatch.imperative(transactions);
        boolean streams = TaskAnyMatch.withStreams(transactions);

        System.out.println("TaskAnyMatch — any transaction over 2000?");
        System.out.println("  IMPERATIVE (loop + early return): " + imperative);
        System.out.println("  STREAMS    (anyMatch):            " + streams);

        assertEquals(imperative, streams);
        assertTrue(streams, "T06 (25000), T13 (3200) and T15 (2600) are all over 2000");
    }

    @Test
    @DisplayName("TaskAnyMatch on quiet data: both say false — and streams short-circuit like the loop")
    void anyMatchAgreesOnNegativeCase() {
        // A list with NO transaction over 2000 — proves equality in the negative case too,
        // which is the case where a broken flag variable would silently differ.
        List<Transaction> quiet = Collections.singletonList(
                new Transaction("T99", "A1", "Coffee", new BigDecimal("4.75"), false));

        boolean imperative = TaskAnyMatch.imperative(quiet);
        boolean streams = TaskAnyMatch.withStreams(quiet);

        System.out.println("TaskAnyMatch — quiet list (single 4.75 coffee):");
        System.out.println("  IMPERATIVE: " + imperative);
        System.out.println("  STREAMS   : " + streams);

        assertEquals(imperative, streams);
        assertFalse(streams, "nothing over 2000, so the answer is false");
    }

    /** Small print helper: [T01, T05, ...] reads better on the projector than full toStrings. */
    private static String ids(List<Transaction> ts) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ts.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(ts.get(i).getId());
        }
        return sb.append("]").toString();
    }
}
