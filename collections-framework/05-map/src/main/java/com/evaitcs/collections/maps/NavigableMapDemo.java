package com.evaitcs.collections.maps;

import java.math.BigDecimal;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Demo 6 — NavigableMap: the sorted-map vocabulary (module 04's NavigableSet, keyed).
 *
 * A TreeMap keeps its entries in a red-black tree ordered BY KEY — String's natural
 * order here, or any Comparator you hand the constructor (core-java 10). Because the
 * keys are ordered, the map can answer questions a HashMap cannot even parse: what is
 * the first/last key? what is the closest key at or below X? give me everything in
 * [from, to) — as a LIVE VIEW.
 *
 * Bound conventions are the ones from module 04, verbatim: headMap(to) EXCLUSIVE,
 * tailMap(from) INCLUSIVE, subMap(from, to) half-open [from, to). The views are windows
 * into the same tree: puts write through, and putting OUTSIDE a view's range throws
 * IllegalArgumentException.
 */
public class NavigableMapDemo {

    public static void main(String[] args) {
        NavigableMap<String, Transaction> statement = new TreeMap<String, Transaction>();
        for (Transaction t : Transaction.sampleStatement()) {
            statement.put(t.getId(), t);
        }

        banner("1. Sorted keys — the statement, ordered by id");

        System.out.println("insertion order was: T-1040, T-1011, T-1027, T-1005, T-1033 (sampleStatement)");
        System.out.println("TreeMap keySet()   : " + statement.keySet());
        System.out.println("descendingMap()    : " + statement.descendingMap().keySet() + "   <- a REVERSED VIEW, not a copy");

        banner("2. first/last, and the four 'closest key' questions");

        System.out.println("firstKey()          = " + statement.firstKey() + "        lastKey() = " + statement.lastKey());
        System.out.println("firstEntry()        = " + statement.firstEntry());
        System.out.println("floorKey(\"T-1028\")   = " + statement.floorKey("T-1028") + "   <- closest <= ('T-1028' itself is absent)");
        System.out.println("ceilingKey(\"T-1028\")= " + statement.ceilingKey("T-1028") + "   <- closest >=");
        System.out.println("lowerKey(\"T-1027\")   = " + statement.lowerKey("T-1027") + "   <- STRICTLY below (a tie does not count)");
        System.out.println("higherKey(\"T-1027\")  = " + statement.higherKey("T-1027") + "   <- strictly above");
        System.out.println("floorKey(\"T-1000\")   = " + statement.floorKey("T-1000") + "         <- nothing that low: null, not an exception");

        banner("3. headMap / tailMap / subMap — VIEWS, not copies");

        SortedMap<String, Transaction> head = statement.headMap("T-1027");
        System.out.println("headMap(\"T-1027\")          = " + head.keySet() + "   <- to-bound EXCLUSIVE");
        System.out.println("tailMap(\"T-1027\")          = " + statement.tailMap("T-1027").keySet() + "   <- from-bound INCLUSIVE");
        System.out.println("subMap(\"T-1011\", \"T-1040\") = " + statement.subMap("T-1011", "T-1040").keySet() + "   <- half-open [from, to)");

        head.put("T-1020", new Transaction("T-1020", "Pharmacy", new BigDecimal("12.30"), 1_700_000_250_000L));
        System.out.println("head.put(\"T-1020\", ...) — wrote THROUGH; full map now: " + statement.keySet());

        try {
            head.put("T-1099", new Transaction("T-1099", "Out of range", BigDecimal.ZERO, 0L));
        } catch (IllegalArgumentException e) {
            System.out.println("head.put(\"T-1099\", ...) -> CAUGHT IllegalArgumentException — outside the view's range.");
        }
        System.out.println("Use case in one sentence: 'every transaction of the last week' is tailMap(mondayId) —");
        System.out.println("no filter loop, no new map; the tree already knows where the range starts.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
