package com.evaitcs.collections.sets;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Demo 2 — ONE STATEMENT, THREE SETS: the same five transactions, three engines,
 * three different answers to "in what order do you hold them?".
 *
 * This is the README's engine diagram made runnable:
 *   HashSet       — a hash table of buckets; order depends on hashes, NOT insertion;
 *                   add/contains ~O(1). The workhorse.
 *   LinkedHashSet — the SAME hash table with a linked list threaded through the entries;
 *                   insertion order guaranteed; pays a little memory + bookkeeping per add.
 *   TreeSet       — a red-black tree ordered by compareTo (Transaction is Comparable by id);
 *                   sorted order; O(log n) per operation; equals() is NEVER consulted.
 *
 * All three are declared as Set<Transaction> — module 01's swap lesson, verbatim.
 */
public class ThreeSetsSameDataDemo {

    public static void main(String[] args) {
        List<Transaction> statement = Transaction.sampleStatement();

        banner("1. Load the SAME statement into three Set implementations");

        Set<Transaction> hashed = new HashSet<Transaction>();
        Set<Transaction> linked = new LinkedHashSet<Transaction>();
        Set<Transaction> sorted = new TreeSet<Transaction>();   // natural order: Transaction IS Comparable
        for (Transaction t : statement) {
            hashed.add(t);
            linked.add(t);
            sorted.add(t);
        }
        System.out.println("statement loaded into HashSet, LinkedHashSet, TreeSet — " + statement.size() + " ids each.");

        banner("2. Print all three — read the orders, then read the disclaimers");

        System.out.println("insertion order (the List) : " + idsOf(statement));
        System.out.println();
        System.out.println("HashSet                    : " + idsOf(hashed));
        System.out.println("  ^ DISCLAIMER: order depends on hashes, not insertion — do not memorize this line,");
        System.out.println("    it is an implementation detail that may differ between JVM versions. It is NOT random,");
        System.out.println("    it is just none of your business. Need order? Pick LinkedHashSet or TreeSet.");
        System.out.println();
        System.out.println("LinkedHashSet              : " + idsOf(linked));
        System.out.println("  ^ EXACTLY the insertion order — a hash table PLUS a linked list through the entries.");
        System.out.println();
        System.out.println("TreeSet                    : " + idsOf(sorted));
        System.out.println("  ^ sorted by the natural order — Transaction.compareTo, by id (core-java 10).");

        banner("3. Same contract, different questions: how each found the stranger");

        Transaction probe = new Transaction("T-1027", "whatever", new BigDecimal("0.00"), 0L);
        System.out.println("sizes: HashSet " + hashed.size() + ", LinkedHashSet " + linked.size() + ", TreeSet " + sorted.size());
        System.out.println("contains(equal stranger T-1027): "
                + hashed.contains(probe) + " / " + linked.contains(probe) + " / " + sorted.contains(probe));
        System.out.println("All three say true — but they ASKED differently:");
        System.out.println("  HashSet/LinkedHashSet: hashCode() -> bucket -> equals() down the chain  (core-java 08)");
        System.out.println("  TreeSet:               compareTo() down the tree — equals() NEVER called (core-java 10)");

        banner("4. What you pay, what you get — the table to photograph, spoken aloud");

        System.out.println("HashSet        add/contains ~O(1)      order: NONE           extra: just the bucket array");
        System.out.println("LinkedHashSet  add/contains ~O(1)      order: INSERTION      extra: a linked list through entries");
        System.out.println("TreeSet        add/contains  O(log n)   order: SORTED         extra: a red-black tree + an order to compare with");
        System.out.println("The code above declared Set<Transaction> and never changed — only the `new` line did.");
    }

    /** ids on one line — order is the whole point of this demo, so print exactly what iteration gives. */
    static String idsOf(Iterable<Transaction> transactions) {
        StringBuilder ids = new StringBuilder("[");
        for (Transaction t : transactions) {
            if (ids.length() > 1) {
                ids.append(", ");
            }
            ids.append(t.getId());
        }
        return ids.append("]").toString();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
