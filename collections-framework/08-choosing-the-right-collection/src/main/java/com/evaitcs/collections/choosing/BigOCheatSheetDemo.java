package com.evaitcs.collections.choosing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo 1 — THE BIG-O CHEAT SHEET: the table this whole course has been building toward.
 *
 * Seven modules of guarantees, compressed into nine rows. Read it the way you will read
 * it in real life: find the OPERATION you will do most (get by index? contains? add?
 * remove?), then read DOWN that column — the table tells you which containers can even
 * answer the question, and what each answer costs.
 *
 * The vocabulary, once:
 *   O(1)     constant — same cost at n=100 and n=100,000,000 (hashing: module 05)
 *   O(log n) doubling the size adds ONE step (halving search / balanced trees: modules 04, 05, 07)
 *   O(n)     linear — a scan; fine occasionally, fatal in a hot loop (module 02 measured it)
 *   "*"      AMORTIZED: usually O(1), occasionally one expensive grow-and-copy; the
 *            average over many adds is constant (ArrayList's 1.5x growth, module 02)
 *
 * The last column is not a footnote — iteration order is a GUARANTEE you choose like any
 * other (module 04): "unspecified" means HashSet may reshuffle between runs, "insertion"
 * means LinkedHashSet replays your add order, "sorted" means TreeSet/TreeMap impose their
 * comparator order. And PriorityQueue's "none" is module 06's trap in one cell: a heap is
 * NOT a sorted container, only peek()/poll() see the order.
 */
public class BigOCheatSheetDemo {

    public static void main(String[] args) {
        banner("1. The table — photograph it (the README renders the same thing in markdown)");

        // Fixed-width columns, built by hand: this table is read from the back of the room.
        final String row = "%-16s | %-22s | %-20s | %-12s | %-24s | %s%n";
        System.out.printf(row, "CLASS", "LOOKUP / get", "contains", "add", "remove", "ITERATION ORDER");
        System.out.println(dashes(16) + "-+-" + dashes(22) + "-+-" + dashes(20) + "-+-" + dashes(12) + "-+-" + dashes(24) + "-+-" + dashes(27));
        System.out.printf(row, "ArrayList", "get(i) O(1)", "O(n)", "O(1)* tail", "O(n) middle", "insertion order");
        System.out.printf(row, "LinkedList", "get(i) O(n)", "O(n)", "O(1) ends", "O(n) find, O(1) cut", "insertion order");
        System.out.printf(row, "ArrayDeque", "peek ends O(1)", "O(n)", "O(1)* ends", "O(1) ends", "FIFO / LIFO (module 06)");
        System.out.printf(row, "PriorityQueue", "peek() O(1)", "O(n)", "O(log n)", "poll O(log n)/obj O(n)", "NONE — heap order (06)");
        System.out.printf(row, "HashSet", "- no get(i) -", "O(1)", "O(1)", "O(1)", "unspecified (module 04)");
        System.out.printf(row, "LinkedHashSet", "- no get(i) -", "O(1)", "O(1)", "O(1)", "insertion order (module 04)");
        System.out.printf(row, "TreeSet", "floor/ceiling O(log n)", "O(log n)", "O(log n)", "O(log n)", "SORTED (module 04)");
        System.out.printf(row, "HashMap", "get(key) O(1)", "containsKey O(1)", "put O(1)", "O(1)", "unspecified (module 05)");
        System.out.printf(row, "TreeMap", "get(key) O(log n)", "containsKey O(log n)", "put O(log n)", "O(log n)", "SORTED by key (module 05)");
        System.out.println();
        System.out.println("* amortized: usually O(1), one grow-and-copy every so often; the average stays constant.");

        banner("2. How to read it — the operation you do MOST decides the row you pick");

        System.out.println("Asking 'contains?' in a hot loop rules OUT ArrayList/LinkedList (O(n) scans)");
        System.out.println("and rules IN the hash family (O(1)) — EmpiricalContainsDemo times the gap on 100k.");
        System.out.println("Asking 'get(7)?' rules out every Set and Map (no index at all) and rules IN ArrayList.");
        System.out.println("Asking 'smallest next?' rules out everything except PriorityQueue — and its price is");
        System.out.println("that for-each order is meaningless (module 06). Nothing is free; everything is a trade.");

        banner("3. Feeding the table — capacity hints keep the amortized O(1) cheap");

        // ArrayList starts at 10 slots and grows ~1.5x by COPYING (module 02). If you KNOW
        // the size, say so: one allocation, zero copies.
        List<Integer> sized = new ArrayList<Integer>(100_000);
        for (int i = 0; i < 100_000; i++) {
            sized.add(Integer.valueOf(i));
        }
        System.out.println("new ArrayList<Integer>(100_000): built with ONE array, no grow-and-copy.");

        // HashMap starts at 16 buckets and RESIZES (rehashes everything!) at load factor
        // 0.75 (module 05). The hint formula: (int)(expected / 0.75f) + 1 buckets up front,
        // so the resize never happens.
        Map<String, Account> byId = new HashMap<String, Account>((int) (3 / 0.75f) + 1);
        for (Account a : Account.sampleAccounts()) {
            byId.put(a.getId(), a);
        }
        System.out.println("new HashMap<String, Account>((int)(3 / 0.75f) + 1): sized = 5 buckets, no rehash for 3 entries.");
        System.out.println("Rule: if you know the size, pass the size. It is the cheapest performance win in Java.");
    }

    /** n dashes — Java 8 has no String.repeat, so the table separator is hand-rolled (core-java idiom). */
    private static String dashes(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append('-');
        }
        return sb.toString();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
