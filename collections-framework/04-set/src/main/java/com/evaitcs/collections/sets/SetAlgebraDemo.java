package com.evaitcs.collections.sets;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Demo 4 — SET ALGEBRA: the bulk operations ARE the Venn diagram.
 *
 *   addAll(c)     — UNION: everything in this OR in c
 *   retainAll(c)  — INTERSECTION: only what is in this AND in c
 *   removeAll(c)  — DIFFERENCE: this minus c
 *   containsAll(c)— SUPERSET test
 *
 * The one warning that matters, printed at every step: ALL of these MUTATE the receiver.
 * There is no non-destructive union in the classic API — you COPY first (constructor
 * takes any Collection), then mutate the copy. Forgetting that copy is how production
 * code quietly eats its own input data.
 *
 * Results are collected into TreeSets so the printed order is SORTED and deterministic —
 * never print a HashSet and call it "the answer" (its order is nobody's business).
 * The Stream versions of all of this — Collectors.toSet(), flatMap, groupingBy — wait in
 * java8-features 04/06.
 */
public class SetAlgebraDemo {

    public static void main(String[] args) {
        Set<String> october = new LinkedHashSet<String>();
        october.add("ACC-1005");
        october.add("ACC-1011");
        october.add("ACC-1027");
        october.add("ACC-1033");

        Set<String> november = new LinkedHashSet<String>();
        november.add("ACC-1027");
        november.add("ACC-1033");
        november.add("ACC-1040");
        november.add("ACC-1046");

        banner("1. Two months of fraud-alert account ids");

        System.out.println("october  : " + october);
        System.out.println("november : " + november);

        banner("2. UNION — addAll: flagged in EITHER month");

        Set<String> union = new TreeSet<String>(october);   // COPY first — addAll mutates the receiver!
        boolean changed = union.addAll(november);
        System.out.println("union     : " + union + "   (addAll returned " + changed + ")");
        System.out.println("october   : " + october + "   <- untouched, because we mutated a copy.");

        banner("3. INTERSECTION — retainAll: flagged in BOTH months");

        Set<String> intersection = new TreeSet<String>(october);
        changed = intersection.retainAll(november);
        System.out.println("both      : " + intersection + "   <- the repeat offenders (retainAll returned " + changed + ")");

        banner("4. DIFFERENCE — removeAll: October's alerts that November cleared");

        Set<String> onlyOctober = new TreeSet<String>(october);
        changed = onlyOctober.removeAll(november);
        System.out.println("oct - nov : " + onlyOctober + "   (removeAll returned " + changed + ")");

        banner("5. containsAll — the superset question");

        System.out.println("october.containsAll(november)     = " + october.containsAll(november)
                + "    <- november has ids October never saw");
        Set<String> repeatOffenders = new TreeSet<String>(intersection);
        System.out.println("october.containsAll(intersection) = " + october.containsAll(repeatOffenders)
                + "     <- by definition: the intersection lives in both");
        System.out.println("Bulk operations return boolean = 'did this call change the Set?' — same honesty as add().");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
