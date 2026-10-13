package com.evaitcs.collections.sets;

import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Demo 5 — NULLS, and the SortedSet/NavigableSet vocabulary.
 *
 * Two unrelated-looking facts that belong in one demo because both come from the ENGINE:
 *
 *   - null: the hash Sets store one null happily (null hashes to bucket 0 by convention
 *     and "equals itself"), but a natural-order TreeSet must COMPARE every element —
 *     and comparing null throws NullPointerException. Not a policy choice: a consequence
 *     of the red-black tree needing an order.
 *
 *   - Because a TreeSet keeps everything ordered, it can offer the SortedSet/NavigableSet
 *     vocabulary a hash Set never could: first/last, floor/ceiling, lower/higher, and the
 *     range views headSet/tailSet/subSet.
 *
 * The range methods return VIEWS, not copies — live windows into the same tree, exactly
 * like Arrays.asList in module 01 and unmodifiableList in module 07. Writes go THROUGH,
 * and adding outside the view's range throws IllegalArgumentException.
 *
 * Bound conventions to memorize: headSet(to) EXCLUSIVE, tailSet(from) INCLUSIVE,
 * subSet(from, to) is the half-open interval [from, to) — like a List's index range.
 */
public class NullsAndNavigableDemo {

    public static void main(String[] args) {
        banner("1. null — legal in the hash Sets, fatal in a natural-order TreeSet");

        Set<String> hashed = new HashSet<String>();
        System.out.println("HashSet.add(null)       -> " + hashed.add(null));
        System.out.println("HashSet.add(null) again -> " + hashed.add(null) + "    <- at most ONE null: the second is a 'duplicate'");
        System.out.println("HashSet.contains(null)  -> " + hashed.contains(null) + "     <- and it is findable (LinkedHashSet behaves identically)");

        Set<String> sorted = new TreeSet<String>();
        try {
            sorted.add(null);
        } catch (NullPointerException e) {
            System.out.println("TreeSet.add(null)       -> CAUGHT NullPointerException — the tree must COMPARE null, and cannot.");
        }
        System.out.println("Even an EMPTY TreeSet refuses: the first add self-compares as a type check.");

        banner("2. The NavigableSet vocabulary — only an ORDERED set can answer these");

        NavigableSet<String> ids = statementIds();
        System.out.println("ids = " + ids + "   <- TreeSet: sorted, so every question below is deterministic");

        System.out.println("first()           = " + ids.first() + "        last() = " + ids.last());
        System.out.println("floor(\"T-1028\")    = " + ids.floor("T-1028") + "   <- closest <= ... ('T-1028' itself is absent)");
        System.out.println("ceiling(\"T-1028\") = " + ids.ceiling("T-1028") + "   <- closest >= ...");
        System.out.println("lower(\"T-1027\")   = " + ids.lower("T-1027") + "   <- STRICTLY below (a tie does not count)");
        System.out.println("higher(\"T-1027\")  = " + ids.higher("T-1027") + "   <- strictly above");
        System.out.println("floor(\"T-1000\")   = " + ids.floor("T-1000") + "         <- nothing that low: null, not an exception");

        banner("3. headSet / tailSet / subSet — VIEWS, not copies");

        SortedSet<String> head = ids.headSet("T-1027");
        System.out.println("headSet(\"T-1027\")          = " + head + "   <- to-bound is EXCLUSIVE");
        System.out.println("tailSet(\"T-1027\")          = " + ids.tailSet("T-1027") + "   <- from-bound is INCLUSIVE");
        System.out.println("subSet(\"T-1011\", \"T-1040\") = " + ids.subSet("T-1011", "T-1040") + "   <- half-open [from, to)");

        head.add("T-1020");
        System.out.println("head.add(\"T-1020\") — the write went THROUGH; the original now: " + ids);
        ids.add("T-1001");
        System.out.println("ids.add(\"T-1001\")  — and the view grew with it:          " + head);

        try {
            head.add("T-1099");
        } catch (IllegalArgumentException e) {
            System.out.println("head.add(\"T-1099\") -> CAUGHT IllegalArgumentException — outside the view's range.");
        }
        System.out.println("Same lesson shape as Arrays.asList (module 01): a view is a live window, not a snapshot.");
    }

    /** The statement's five ids in a TreeSet — setup kept out of the way so the vocabulary reads top-to-bottom. */
    static NavigableSet<String> statementIds() {
        NavigableSet<String> ids = new TreeSet<String>();
        for (Transaction t : Transaction.sampleStatement()) {
            ids.add(t.getId());
        }
        return ids;
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
