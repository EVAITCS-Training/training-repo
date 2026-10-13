package com.evaitcs.corejava.equality;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Demo 8 — consistency with compareTo: a teaser for module 10 (Comparable/Comparator).
 *
 * HashMap/HashSet decide equality with equals()+hashCode(). SORTED collections
 * (TreeSet, TreeMap) decide it with compareTo() instead — they never call equals at all.
 * So a class can be "equal" by one measure and "different" by the other, and the two
 * families of collections will disagree about how many elements you have.
 *
 * BigDecimal is the ready-made example (module 02 callback): 2.0 and 2.00 are
 *   equals()  -> FALSE (different scale)
 *   compareTo -> 0     (numerically equal)
 * Put both in a HashSet (uses equals): size 2. Put both in a TreeSet (uses compareTo):
 * size 1 — the TreeSet "loses" one, because compareTo says they are the same.
 *
 * THE FLAG FOR MODULE 10: if you implement Comparable, make compareTo CONSISTENT WITH
 * equals — a.compareTo(b) == 0 exactly when a.equals(b). Sorted collections assume it.
 */
public class CompareToConsistencyTeaserDemo {

    public static void main(String[] args) {

        banner("1. equals vs compareTo on the SAME two BigDecimals");

        BigDecimal twoOh = new BigDecimal("2.0");
        BigDecimal twoOhOh = new BigDecimal("2.00");
        System.out.println("2.0.equals(2.00)          : " + twoOh.equals(twoOhOh) + "  <- scale differs");
        System.out.println("2.0.compareTo(2.00)       : " + twoOh.compareTo(twoOhOh) + "     <- 0 means 'equal' to compareTo");

        banner("2. HashSet (uses equals) vs TreeSet (uses compareTo)");

        Set<BigDecimal> hashSet = new HashSet<BigDecimal>();
        hashSet.add(twoOh);
        hashSet.add(twoOhOh);

        Set<BigDecimal> treeSet = new TreeSet<BigDecimal>();
        treeSet.add(twoOh);
        treeSet.add(twoOhOh);

        System.out.println("HashSet size (equals-based)   : " + hashSet.size() + "   <- keeps BOTH (not equals)");
        System.out.println("TreeSet size (compareTo-based): " + treeSet.size() + "   <- keeps ONE (compareTo says equal)");
        System.out.println("=> Same two objects, two Set implementations, two different sizes.");

        banner("3. Why it matters");
        System.out.println("A TreeSet literally never calls equals to decide membership — it trusts compareTo.");
        System.out.println("If compareTo and equals disagree, 'contains' and 'size' lie to you depending on the Set.");
        System.out.println("=> Module 10: implement Comparable so compareTo == 0 IFF equals is true. Plant the flag.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
