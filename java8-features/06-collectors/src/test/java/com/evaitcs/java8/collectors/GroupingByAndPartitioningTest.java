package com.evaitcs.java8.collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the two report builders. Run with:
 *   .\mvnw.cmd -pl 06-collectors test
 *
 * The first test is the module's raison d'etre: module 05's TaskCount made the class
 * write the containsKey/get/put dance and promised "module 06 does this in ONE line".
 * Here, the one-liner is asserted EQUAL to the dance, on the identical dataset.
 */
class GroupingByAndPartitioningTest {

    private final List<Customer> customers = SampleBank.customers();
    private final List<Account> accounts = SampleBank.accounts();
    private final List<Transaction> transactions = SampleBank.transactions();

    @Test
    @DisplayName("PROMISE KEPT: groupingBy(getCity, counting()) == module 05's containsKey/get/put loop")
    void groupingByIsTheTaskCountOneLiner() {
        // The plumbing TaskCount made the class write (module 05), copied as the baseline:
        Map<String, Integer> plumbing = new LinkedHashMap<>();
        for (Customer c : customers) {
            if (plumbing.containsKey(c.getCity())) {
                plumbing.put(c.getCity(), plumbing.get(c.getCity()) + 1);
            } else {
                plumbing.put(c.getCity(), 1);
            }
        }

        // The one line module 05 teased:
        Map<String, Long> oneLiner = customers.stream()
                .collect(Collectors.groupingBy(Customer::getCity, Collectors.counting()));

        System.out.println("8 lines of plumbing : " + plumbing);
        System.out.println("1 line of groupingBy: " + oneLiner);

        assertEquals(plumbing.keySet(), oneLiner.keySet(), "same cities discovered");
        for (String city : plumbing.keySet()) {
            assertEquals(plumbing.get(city).longValue(), oneLiner.get(city).longValue(),
                    "same count for " + city);
        }
        assertEquals(Long.valueOf(3), oneLiner.get("Austin"));
        assertEquals(Long.valueOf(1), oneLiner.get("Denver"));
        assertEquals(Long.valueOf(2), oneLiner.get("Seattle"));
    }

    @Test
    @DisplayName("groupingBy creates ONLY keys it sees — no empty buckets appear")
    void groupingByMaterializesOnlyNonEmptyBuckets() {
        Map<String, List<Customer>> byCity = customers.stream()
                .collect(Collectors.groupingBy(Customer::getCity));

        System.out.println("cities present: " + byCity.keySet());

        assertEquals(3, byCity.size(), "exactly the three cities the data contains");
        assertFalse(byCity.containsKey("Phoenix"), "a city with no customers has NO entry at all");
        assertEquals(3, byCity.get("Austin").size());
    }

    @Test
    @DisplayName("downstream mapping(getName, toList()) keeps encounter order inside each bucket")
    void downstreamMappingCollectsNames() {
        Map<String, List<String>> namesByCity = customers.stream()
                .collect(Collectors.groupingBy(Customer::getCity,
                        Collectors.mapping(Customer::getName, Collectors.toList())));

        namesByCity.forEach((city, names) -> System.out.println(city + " -> " + names));

        assertEquals(Arrays.asList("Ada Lovelace", "Grace Hopper", "Katherine Johnson"),
                namesByCity.get("Austin"), "stream order is preserved WITHIN a bucket");
        assertEquals(Arrays.asList("Linus Pauling", "Barbara Liskov"), namesByCity.get("Seattle"));
    }

    @Test
    @DisplayName("MULTI-LEVEL: accounts by city then by type — nested maps, exact counts")
    void multiLevelGroupingBy() {
        // The join, built with toMap: ids are unique, so this is the SAFE toMap.
        Map<String, String> cityOfOwner = customers.stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getCity));

        Map<String, Map<Account.Type, Long>> byCityThenType = accounts.stream()
                .collect(Collectors.groupingBy(
                        (Account a) -> cityOfOwner.get(a.getOwnerId()),
                        Collectors.groupingBy(Account::getType, Collectors.counting())));

        byCityThenType.forEach((city, perType) -> System.out.println(city + " -> " + perType));

        assertEquals(Long.valueOf(3), byCityThenType.get("Austin").get(Account.Type.CHECKING),
                "A1, A3, A5 are Austin checking accounts");
        assertEquals(Long.valueOf(1), byCityThenType.get("Austin").get(Account.Type.SAVINGS), "A2");
        assertEquals(1, byCityThenType.get("Denver").size(),
                "Denver owns only A4 (SAVINGS) — the inner map has a single entry");
        assertFalse(byCityThenType.get("Denver").containsKey(Account.Type.CHECKING),
                "the INNER groupingBy follows the same rule: no key for an empty bucket");
        assertEquals(Long.valueOf(2), byCityThenType.get("Seattle").get(Account.Type.CHECKING), "A6, A8");
        assertEquals(Long.valueOf(1), byCityThenType.get("Seattle").get(Account.Type.SAVINGS), "A7");

        long total = byCityThenType.values().stream()
                .flatMap(m -> m.values().stream())
                .reduce(0L, Long::sum);
        assertEquals(8, total, "every account counted exactly once across the whole structure");
    }

    @Test
    @DisplayName("TreeMap supplier: same grouping, alphabetically sorted keys")
    void treeMapSupplierSortsKeys() {
        Map<String, List<Customer>> sorted = customers.stream()
                .collect(Collectors.groupingBy(Customer::getCity, TreeMap::new, Collectors.toList()));

        System.out.println("keySet with TreeMap::new : " + sorted.keySet());

        assertEquals(Arrays.asList("Austin", "Denver", "Seattle"), new ArrayList<>(sorted.keySet()),
                "the map factory decides key ORDER; the downstream still decides bucket content");
    }

    @Test
    @DisplayName("partitioningBy splits deposits/withdrawals in ONE pass — counts and money totals")
    void partitioningSplitsMoneyFlow() {
        Map<Boolean, List<Transaction>> split = transactions.stream()
                .collect(Collectors.partitioningBy(Transaction::isDeposit));
        Map<Boolean, BigDecimal> totals = transactions.stream()
                .collect(Collectors.partitioningBy(Transaction::isDeposit,
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        System.out.println("IN  : " + split.get(true).size() + " transactions totalling " + totals.get(true));
        System.out.println("OUT : " + split.get(false).size() + " transactions totalling " + totals.get(false));

        assertEquals(7, split.get(true).size(), "T01, T04, T05, T06, T09, T10, T12");
        assertEquals(8, split.get(false).size());
        assertEquals(15, split.get(true).size() + split.get(false).size(),
                "nothing lost, nothing duplicated — a partition covers the whole stream");
        assertEquals(0, totals.get(true).compareTo(new BigDecimal("33569.54")), "hand-checked money IN");
        assertEquals(0, totals.get(false).compareTo(new BigDecimal("7661.47")), "hand-checked money OUT");
    }

    @Test
    @DisplayName("THE GUARANTEE: partitioningBy ships BOTH keys even when a bucket is empty; groupingBy doesn't")
    void partitioningAlwaysHasTwoBuckets() {
        BigDecimal huge = new BigDecimal("50000.00"); // no account in SampleBank comes close

        Map<Boolean, List<Account>> partitioned = accounts.stream()
                .collect(Collectors.partitioningBy(a -> a.getBalance().compareTo(huge) > 0));
        Map<Boolean, List<Account>> grouped = accounts.stream()
                .collect(Collectors.groupingBy(a -> a.getBalance().compareTo(huge) > 0));

        System.out.println("partitioningBy : keys=" + partitioned.keySet() + ", get(true)=" + partitioned.get(true));
        System.out.println("groupingBy     : keys=" + grouped.keySet() + ", get(true)=" + grouped.get(true));

        assertEquals(2, partitioned.size(), "both keys, always");
        assertTrue(partitioned.containsKey(Boolean.TRUE), "the empty bucket still HAS its key");
        assertTrue(partitioned.get(true).isEmpty(), "present AND empty — never null");
        assertEquals(8, partitioned.get(false).size());

        assertEquals(1, grouped.size(), "groupingBy materialized only the bucket that caught something");
        assertFalse(grouped.containsKey(Boolean.TRUE));
        assertNull(grouped.get(true), "this null is exactly what partitioningBy guarantees away");
    }
}
