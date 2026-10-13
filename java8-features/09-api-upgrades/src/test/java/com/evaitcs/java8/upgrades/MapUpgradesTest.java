package com.evaitcs.java8.upgrades;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the Map claims in MapUpgradesDemo / ComputeIfAbsentStarDemo.
 *   .\mvnw.cmd -pl 09-api-upgrades test
 * Each test proves exactly ONE claim, prints the evidence, then asserts it.
 */
class MapUpgradesTest {

    private static final BigDecimal DEFAULT_FEE = new BigDecimal("5.00");

    private Map<String, BigDecimal> feeSchedule() {
        Map<String, BigDecimal> fees = new TreeMap<>();
        fees.put("Austin", new BigDecimal("2.50"));
        return fees;
    }

    @Test
    @DisplayName("getOrDefault: value when present, fallback when absent - ONE lookup")
    void getOrDefaultFallsBack() {
        Map<String, BigDecimal> fees = feeSchedule();
        BigDecimal present = fees.getOrDefault("Austin", DEFAULT_FEE);
        BigDecimal absent = fees.getOrDefault("Houston", DEFAULT_FEE);
        System.out.println("present Austin -> " + present + " ; absent Houston -> " + absent);
        assertEquals(new BigDecimal("2.50"), present);
        assertEquals(DEFAULT_FEE, absent);
    }

    @Test
    @DisplayName("putIfAbsent: the FIRST value wins, later puts are ignored")
    void putIfAbsentKeepsFirst() {
        Map<String, String> firstTxnByCity = new TreeMap<>();
        for (Transaction t : Transaction.sampleStatement()) {
            firstTxnByCity.putIfAbsent(t.getCity(), t.getId());
        }
        System.out.println("first txn per city = " + firstTxnByCity);
        assertEquals("T-101", firstTxnByCity.get("Austin")); // T-106 came later and lost
        assertEquals("T-103", firstTxnByCity.get("Dallas"));
        assertEquals("T-105", firstTxnByCity.get("Houston"));
    }

    @Test
    @DisplayName("computeIfAbsent: mapping function is NOT called when the key is present")
    void computeIfAbsentSkipsMappingWhenPresent() {
        final int[] creations = { 0 };
        Map<String, List<Transaction>> byCity = new TreeMap<>();

        byCity.computeIfAbsent("Austin", k -> { creations[0]++; return new ArrayList<>(); });
        assertEquals(1, creations[0], "first call must run the mapping function");
        List<Transaction> firstList = byCity.get("Austin");

        byCity.computeIfAbsent("Austin", k -> { creations[0]++; return new ArrayList<>(); });
        System.out.println("after 2 computeIfAbsent calls on the SAME key: creations=" + creations[0]);
        assertEquals(1, creations[0], "second call must NOT run the mapping function");
        assertSame(firstList, byCity.get("Austin"), "must return the EXISTING list, not a new one");
    }

    @Test
    @DisplayName("computeIfAbsent grouping: 6 transactions land in 3 city lists")
    void computeIfAbsentGroupsByCity() {
        Map<String, List<Transaction>> byCity = new TreeMap<>();
        for (Transaction t : Transaction.sampleStatement()) {
            byCity.computeIfAbsent(t.getCity(), k -> new ArrayList<>()).add(t);
        }
        System.out.println("Austin=" + byCity.get("Austin").size()
                + " Dallas=" + byCity.get("Dallas").size()
                + " Houston=" + byCity.get("Houston").size());
        assertEquals(3, byCity.get("Austin").size());
        assertEquals(2, byCity.get("Dallas").size());
        assertEquals(1, byCity.get("Houston").size());
    }

    @Test
    @DisplayName("merge with BigDecimal::add sums per-key fees (old get/put dance == one line)")
    void mergeSumsFees() {
        BigDecimal perTxnFee = new BigDecimal("1.50");

        Map<String, BigDecimal> viaMerge = new TreeMap<>();
        Map<String, BigDecimal> viaOldDance = new TreeMap<>();
        for (Transaction t : Transaction.sampleStatement()) {
            viaMerge.merge(t.getCity(), perTxnFee, BigDecimal::add);
            // the pre-Java-8 idiom, kept here as the reference implementation:
            BigDecimal soFar = viaOldDance.get(t.getCity());
            viaOldDance.put(t.getCity(), soFar == null ? perTxnFee : soFar.add(perTxnFee));
        }
        System.out.println("merge result  = " + viaMerge);
        System.out.println("old get/put   = " + viaOldDance);
        assertEquals(viaOldDance, viaMerge, "merge must produce exactly what the old dance did");
        assertEquals(new BigDecimal("4.50"), viaMerge.get("Austin")); // 3 txns x 1.50
    }

    @Test
    @DisplayName("merge REMOVES the key when the remapping function returns null")
    void mergeNullResultRemovesKey() {
        Map<String, BigDecimal> fees = new TreeMap<>();
        fees.put("Dallas", new BigDecimal("3.00"));
        // full waiver: 3.00 - 3.00 == 0 -> remapping returns null -> key disappears
        BigDecimal result = fees.merge("Dallas", new BigDecimal("3.00"),
                (oldFee, credit) -> {
                    BigDecimal remaining = oldFee.subtract(credit);
                    return remaining.signum() == 0 ? null : remaining;
                });
        System.out.println("merge returned " + result + " ; map now = " + fees);
        assertNull(result, "merge returns the new value - null means the key was removed");
        assertFalse(fees.containsKey("Dallas"), "Dallas must be GONE from the map");
        assertTrue(fees.isEmpty());
    }

    @Test
    @DisplayName("replaceAll transforms every value IN PLACE - same map instance")
    void replaceAllTransformsInPlace() {
        Map<String, BigDecimal> fees = feeSchedule();
        fees.put("Dallas", new BigDecimal("3.00"));
        Map<String, BigDecimal> sameReference = fees;

        fees.replaceAll((city, oldFee) -> oldFee.multiply(new BigDecimal("2")).setScale(2));
        System.out.println("after doubling: " + fees);
        assertSame(sameReference, fees, "replaceAll mutates the SAME map, it does not return a new one");
        assertEquals(new BigDecimal("5.00"), fees.get("Austin"));
        assertEquals(new BigDecimal("6.00"), fees.get("Dallas"));
    }

    @Test
    @DisplayName("Map.forEach visits EVERY entry exactly once")
    void forEachVisitsEveryEntry() {
        Map<String, BigDecimal> fees = new TreeMap<>();
        fees.put("Austin", new BigDecimal("2.50"));
        fees.put("Dallas", new BigDecimal("3.00"));
        fees.put("Houston", new BigDecimal("1.50"));

        final int[] visited = { 0 };
        final BigDecimal[] sum = { BigDecimal.ZERO };
        fees.forEach((city, fee) -> { visited[0]++; sum[0] = sum[0].add(fee); });
        System.out.println("visited=" + visited[0] + " sum=" + sum[0]);
        assertEquals(3, visited[0]);
        assertEquals(new BigDecimal("7.00"), sum[0]);
    }
}
