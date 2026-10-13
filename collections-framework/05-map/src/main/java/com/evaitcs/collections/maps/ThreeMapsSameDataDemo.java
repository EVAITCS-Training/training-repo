package com.evaitcs.collections.maps;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Demo 4 — ONE REGISTRY, THREE MAPS: module 04's ThreeSetsSameDataDemo, keyed.
 *
 * The engines are the same engines — a Map is a Set of entries with a value glued to each:
 *   HashMap       — hash table of buckets; iteration order depends on hashes, not insertion;
 *                   get/put ~O(1). The default choice.
 *   LinkedHashMap — the same hash table PLUS a linked list through the entries:
 *                   ITERATION order by default (here), ACCESS order on request — and
 *                   access-order + removeEldestEntry is an LRU cache in ten lines (banner 4).
 *   TreeMap       — red-black tree sorted by key (String's compareTo, or a Comparator —
 *                   core-java 10); O(log n); unlocks the NavigableMap vocabulary (NavigableMapDemo).
 *
 * And the dirty secret worth saying out loud in class: HashSet IS a HashMap underneath —
 * every element stored as a KEY, next to one shared dummy value. Module 04 and module 05
 * are the same machine seen from two sides.
 */
public class ThreeMapsSameDataDemo {

    public static void main(String[] args) {
        banner("1. Load the same id -> balance registry into three Map implementations");

        Map<String, BigDecimal> hashed = new HashMap<String, BigDecimal>();
        Map<String, BigDecimal> insertionOrdered = new LinkedHashMap<String, BigDecimal>();
        Map<String, BigDecimal> sorted = new TreeMap<String, BigDecimal>();
        for (Account a : Account.sampleAccounts()) {
            hashed.put(a.getId(), a.getBalance());
            insertionOrdered.put(a.getId(), a.getBalance());
            sorted.put(a.getId(), a.getBalance());
        }
        System.out.println("registry loaded: " + hashed.size() + " entries in each map.");

        banner("2. Print all three — read the orders, then read the disclaimers");

        System.out.println("insertion order (the List) : " + Account.sampleAccounts());
        System.out.println();
        System.out.println("HashMap keySet()           : " + hashed.keySet());
        System.out.println("  ^ DISCLAIMER: order depends on hashes, not insertion — do not memorize this line,");
        System.out.println("    it is an implementation detail. Need order? LinkedHashMap or TreeMap.");
        System.out.println();
        System.out.println("LinkedHashMap keySet()     : " + insertionOrdered.keySet());
        System.out.println("  ^ EXACTLY the insertion order — hash table + linked list through the entries.");
        System.out.println();
        System.out.println("TreeMap keySet()           : " + sorted.keySet());
        System.out.println("  ^ sorted by the keys' natural order (String.compareTo — lexicographic, core-java 03/10).");

        banner("3. Same answers where order does not matter — the Map contract holds in all three");

        System.out.println("get(\"ACC-1005\") : " + hashed.get("ACC-1005") + " / " + insertionOrdered.get("ACC-1005") + " / " + sorted.get("ACC-1005"));
        System.out.println("size()          : " + hashed.size() + " / " + insertionOrdered.size() + " / " + sorted.size());
        System.out.println("equals?         : " + hashed.equals(sorted) + " — Map.equals compares ENTRIES, never order (module 01's Set-like rule).");
        System.out.println("Declared as Map<String, BigDecimal> throughout — only the `new` line changed (module 01's swap lesson).");

        banner("4. LinkedHashMap's second personality — ACCESS order (the LRU flavor)");

        // accessOrder=true: every get() moves the entry to the END of the linked list —
        // "least recently used" floats to the front. Pair that with a subclass overriding
        // removeEldestEntry() and you have a size-capped LRU cache in ten lines:
        final int cap = 2;
        Map<String, BigDecimal> lru = new LinkedHashMap<String, BigDecimal>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, BigDecimal> eldest) {
                return size() > cap;      // anonymous subclass — the classic idiom, no lambdas needed
            }
        };
        lru.put("ACC-1005", new BigDecimal("86.40"));
        lru.put("ACC-1011", new BigDecimal("1200.00"));
        lru.get("ACC-1005");                                  // <- touches 1005: 1011 becomes the eldest
        lru.put("ACC-1027", new BigDecimal("2450.00"));       // <- evicts the eldest (1011)
        System.out.println("after put/put/get(1005)/put, capped at 2: " + lru.keySet());
        System.out.println("ACC-1011 was evicted even though ACC-1005 was inserted FIRST — access order, not insertion.");
        System.out.println("Default LinkedHashMap (accessOrder=false) never reorders on get — that's the one in banner 2.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
