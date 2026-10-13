package com.evaitcs.collections.maps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 05-map test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC. The golden rule this module preaches, this
 * test class obeys: NEVER assert a HashMap's iteration order (it depends on hashes, not
 * insertion) — order claims go through LinkedHashMap (insertion) or TreeMap (sorted), or
 * a HashMap is compared by CONTENT (Map.equals, which ignores order by definition).
 * The "Aa"/"BB" collision is arithmetic on specified String hashCodes, the mutable-key
 * lookups fail on chosen values, and no test touches timing.
 */
class MapContractTest {

    /** The registry used by most tests: id -> Account, built from the sample data. */
    private static Map<String, Account> registry() {
        Map<String, Account> registry = new HashMap<String, Account>();
        for (Account a : Account.sampleAccounts()) {
            registry.put(a.getId(), a);
        }
        return registry;
    }

    @Test
    @DisplayName("put returns the OLD value: a Map REPLACES on a duplicate key (a Set refuses)")
    void putReturnsOldValue() {
        Map<String, Account> registry = registry();

        assertNull(registry.put("ACC-1099", new Account("ACC-1099", "Cid", new BigDecimal("10.00"))),
                "a NEW key has no old value: put returns null");
        assertEquals(4, registry.size());

        Account old = registry.put("ACC-1005", new Account("ACC-1005", "Ana", new BigDecimal("90.00")));
        assertEquals(new BigDecimal("86.40"), old.getBalance(), "put handed back the value it REPLACED");
        assertEquals(4, registry.size(), "replace does not grow the map...");
        assertEquals(new BigDecimal("90.00"), registry.get("ACC-1005").getBalance(), "...get shows the new value");
        System.out.println("put(ACC-1005, 90.00) returned the old " + old.getBalance().toPlainString()
                + "; size stayed " + registry.size() + " — unlike a Set, a Map OVERWRITES (module 04)");
    }

    @Test
    @DisplayName("getOrDefault answers absence with the default; putIfAbsent refuses to overwrite")
    void getOrDefaultAndPutIfAbsentSemantics() {
        Map<String, Account> registry = registry();
        Account placeholder = new Account("ACC-0000", "Unknown", BigDecimal.ZERO);

        assertEquals(placeholder, registry.getOrDefault("ACC-7777", placeholder), "absent key -> the default");
        assertEquals("Ana", registry.getOrDefault("ACC-1005", placeholder).getOwner(), "present key -> the real value");

        Account intruder = new Account("ACC-1011", "Impostor Bo", new BigDecimal("0.01"));
        Account current = registry.putIfAbsent("ACC-1011", intruder);
        assertEquals("Bo", current.getOwner(), "putIfAbsent returns the CURRENT value when the key was there...");
        assertEquals("Bo", registry.get("ACC-1011").getOwner(), "...and does NOT store yours");

        assertNull(registry.putIfAbsent("ACC-2022", new Account("ACC-2022", "Dee", new BigDecimal("7.77"))),
                "a new key: null returned, value stored");
        assertEquals("Dee", registry.get("ACC-2022").getOwner());
        System.out.println("getOrDefault(absent) = placeholder; putIfAbsent(present) kept Bo; putIfAbsent(new) stored Dee");
    }

    @Test
    @DisplayName("HashMap allows a null key AND null values — and get()==null is ambiguous; containsKey is not")
    void nullKeyNullValueAndTheAmbiguity() {
        Map<String, String> map = new HashMap<String, String>();

        map.put("K-PRESENT", null);
        map.put(null, "ghost");   // one null KEY is legal in a HashMap (it hashes to bucket 0)

        assertNull(map.get("K-PRESENT"), "key present, value null");
        assertNull(map.get("K-ABSENT"), "key absent — the SAME answer: get()==null is ambiguous");
        assertTrue(map.containsKey("K-PRESENT"), "containsKey tells the two stories apart");
        assertFalse(map.containsKey("K-ABSENT"));
        assertEquals("ghost", map.get(null), "the null key is a real, findable key");
        assertEquals(2, map.size());
        System.out.println("get() said null twice for opposite reasons; containsKey = true/false. "
                + "ConcurrentHashMap (module 10) bans nulls outright to kill this ambiguity.");
    }

    @Test
    @DisplayName("the \"Aa\"/\"BB\" collision: equal hashCodes, SAME bucket, both entries retrievable")
    void stringCollisionSameBucketEqualsSavesTheDay() {
        assertEquals("Aa".hashCode(), "BB".hashCode(), "specified String.hashCode arithmetic: 'A'*31+'a' == 'B'*31+'B' == 2112");
        assertEquals(HashMapInternalsDemo.bucketOf("Aa"), HashMapInternalsDemo.bucketOf("BB"),
                "same spread hash -> same bucket index at capacity 16");

        Map<String, String> map = new HashMap<String, String>();
        assertNull(map.put("Aa", "Dana"));
        assertNull(map.put("BB", "Bo"), "the second put returned null: BB did NOT replace Aa — the keys are not equal");
        assertEquals(2, map.size(), "two entries sharing one bucket — a chain, walked with equals()");
        assertEquals("Dana", map.get("Aa"));
        assertEquals("Bo", map.get("BB"));

        assertEquals("Dana", map.remove("Aa"), "removing one chain link...");
        assertEquals("Bo", map.get("BB"), "...leaves its bucket-mate untouched");
        System.out.println("\"Aa\".hashCode() == \"BB\".hashCode() == " + "Aa".hashCode()
                + ", bucket " + HashMapInternalsDemo.bucketOf("Aa") + " for both — hashCode found the drawer, equals picked the paper");
    }

    @Test
    @DisplayName("the mutable-key trap: after mutating the key, get()==null but the entry is still inside")
    void mutableKeyUnreachableAfterMutation() {
        Map<MutableKeyTrapDemo.MutableKey, String> balances = new HashMap<MutableKeyTrapDemo.MutableKey, String>();
        MutableKeyTrapDemo.MutableKey key = new MutableKeyTrapDemo.MutableKey("K-1");
        balances.put(key, "1200.00");
        assertEquals("1200.00", balances.get(new MutableKeyTrapDemo.MutableKey("K-1")), "before: an equal stranger finds it");

        key.setId("K-2");   // <-- the one fatal line

        assertNull(balances.get(key), "the VERY key object used to put now returns null — it hashes to the wrong bucket");
        assertFalse(balances.containsKey(key));
        assertNull(balances.get(new MutableKeyTrapDemo.MutableKey("K-1")),
                "the old name finds the right bucket, but the mutated key's equals says 'I am K-2'");
        assertEquals(1, balances.size(), "the entry never LEFT — in the map, unreachable");
        assertNull(balances.remove(new MutableKeyTrapDemo.MutableKey("K-2")), "you cannot even remove it by the new name");

        // Proof it is still in there: entrySet walks buckets, it does not hash.
        boolean sawTheEntry = false;
        for (Map.Entry<MutableKeyTrapDemo.MutableKey, String> entry : balances.entrySet()) {
            sawTheEntry = sawTheEntry || "1200.00".equals(entry.getValue());
        }
        assertTrue(sawTheEntry, "iteration hands back what lookup cannot find");
        System.out.println("after setId(\"K-2\"): get(key)=null, size=" + balances.size() + ", entrySet still sees the entry");
    }

    @Test
    @DisplayName("LinkedHashMap keeps EXACTLY the insertion order — that is the guarantee you pay the list for")
    void linkedHashMapKeepsInsertionOrder() {
        Map<String, BigDecimal> insertionOrdered = new LinkedHashMap<String, BigDecimal>();
        for (Account a : Account.sampleAccounts()) {
            insertionOrdered.put(a.getId(), a.getBalance());
        }

        List<String> keys = new ArrayList<String>(insertionOrdered.keySet());
        assertEquals(Arrays.asList("ACC-1027", "ACC-1005", "ACC-1011"), keys,
                "sampleAccounts order — not sorted, not hashed: INSERTION");
        System.out.println("LinkedHashMap keySet() = " + keys);
    }

    @Test
    @DisplayName("TreeMap sorts by key and unlocks the NavigableMap vocabulary: firstKey/lastKey/headMap")
    void treeMapSortsAndNavigates() {
        TreeMap<String, Transaction> statement = new TreeMap<String, Transaction>();
        for (Transaction t : Transaction.sampleStatement()) {
            statement.put(t.getId(), t);
        }

        assertEquals(Arrays.asList("T-1005", "T-1011", "T-1027", "T-1033", "T-1040"),
                new ArrayList<String>(statement.keySet()), "sorted by String.compareTo (core-java 03/10)");
        assertEquals("T-1005", statement.firstKey());
        assertEquals("T-1040", statement.lastKey());
        assertEquals("T-1027", statement.floorKey("T-1028"), "closest <=");
        assertEquals("T-1033", statement.ceilingKey("T-1028"), "closest >=");
        assertEquals("T-1011", statement.lowerKey("T-1027"), "STRICTLY below");
        assertNull(statement.floorKey("T-1000"), "nothing that low: null, not an exception");

        assertEquals(Arrays.asList("T-1005", "T-1011"),
                new ArrayList<String>(statement.headMap("T-1027").keySet()), "headMap to-bound is EXCLUSIVE");
        assertEquals(Arrays.asList("T-1011", "T-1027", "T-1033"),
                new ArrayList<String>(statement.subMap("T-1011", "T-1040").keySet()), "subMap is half-open [from, to)");
        System.out.println("TreeMap keys " + statement.keySet() + "; headMap(T-1027) = " + statement.headMap("T-1027").keySet());
    }

    @Test
    @DisplayName("entrySet iteration covers every pair exactly once — and a Map itself is not Iterable")
    void entrySetCoversEveryPairExactlyOnce() {
        Map<String, Account> registry = registry();

        Map<String, Account> seen = new TreeMap<String, Account>();
        int pairs = 0;
        for (Map.Entry<String, Account> entry : registry.entrySet()) {
            assertNull(seen.put(entry.getKey(), entry.getValue()), "no key arrives twice — put would return the old value");
            pairs++;
        }
        assertEquals(3, pairs);
        assertEquals(new TreeMap<String, Account>(registry), seen, "collected pairs == the registry itself");
        assertFalse(registry instanceof Iterable, "you cannot for-each a Map directly — it is not Iterable");
        System.out.println("entrySet handed over " + pairs + " pairs, each exactly once; Map instanceof Iterable = false");
    }

    @Test
    @DisplayName("keySet + get produces the SAME pairs as entrySet — same result, double the bucket walks")
    void keySetPlusGetMatchesEntrySet() {
        Map<String, Account> registry = registry();

        Map<String, Account> viaEntries = new TreeMap<String, Account>();
        for (Map.Entry<String, Account> entry : registry.entrySet()) {
            viaEntries.put(entry.getKey(), entry.getValue());
        }

        Map<String, Account> viaKeys = new TreeMap<String, Account>();
        for (String id : registry.keySet()) {
            viaKeys.put(id, registry.get(id));      // <- the second bucket walk, per pair
        }

        assertEquals(viaEntries, viaKeys, "identical content — the idioms differ in COST, not result");
        assertEquals(registry.keySet().size(), viaKeys.size(), "every key produced a non-null value");
        System.out.println("both idioms -> " + viaKeys.keySet() + "; entrySet: 1 walk/pair, keySet+get: 2");
    }

    @Test
    @DisplayName("Map.equals compares ENTRIES, never order or implementation: different build order, still equal")
    void mapEqualsIsOrderInsensitive() {
        Map<String, BigDecimal> one = new LinkedHashMap<String, BigDecimal>();
        one.put("ACC-1027", new BigDecimal("2450.00"));
        one.put("ACC-1005", new BigDecimal("86.40"));
        one.put("ACC-1011", new BigDecimal("1200.00"));

        Map<String, BigDecimal> other = new LinkedHashMap<String, BigDecimal>();
        other.put("ACC-1011", new BigDecimal("1200.00"));    // inserted in the OPPOSITE order
        other.put("ACC-1005", new BigDecimal("86.40"));
        other.put("ACC-1027", new BigDecimal("2450.00"));

        assertEquals(one, other, "same entries, different insertion order: EQUAL");
        assertEquals(one, new TreeMap<String, BigDecimal>(one), "...and across implementations (module 01's Set-like rule)");
        assertFalse(new ArrayList<String>(one.keySet()).equals(new ArrayList<String>(other.keySet())),
                "their key LISTS differ — order-insensitivity belongs to Map.equals, not to the views");
        other.put("ACC-9999", new BigDecimal("1.00"));
        assertFalse(one.equals(other), "one extra entry and equality is gone — it really compares entries");
        System.out.println("LinkedHashMap(reversed) == LinkedHashMap == TreeMap: Map.equals ignores order and class");
    }

    @Test
    @DisplayName("containsValue scans every entry and compares with equals() — O(n), and it FINDS strangers by id")
    void containsValueUsesEqualsAndScansEverything() {
        Map<String, Account> registry = registry();

        assertTrue(registry.containsValue(new Account("ACC-1005", "Not Ana At All", BigDecimal.ZERO)),
                "Account.equals is by id — a stranger with the same id IS the same value");
        assertFalse(registry.containsValue(new Account("ACC-9999", "Ghost", BigDecimal.ZERO)),
                "an id nobody holds: not found — after walking ALL entries");
        assertFalse(registry.containsValue(null), "no null value was ever stored");
        System.out.println("containsValue(equal stranger) = true — found by equals, only after the O(n) scan "
                + "(values have no hash index, unlike keys)");
    }
}
