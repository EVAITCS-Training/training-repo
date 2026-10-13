package com.evaitcs.java8.upgrades;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Two short "quick hits" that round out the daily-driver tour:
 *   - Arrays.setAll / parallelSetAll: fill an array FROM A FUNCTION OF THE INDEX.
 *   - Comparator.comparing / thenComparing / nullsFirst: built in module 01, now used
 *     where they belong - inside List.sort.
 * (Deliberately skipped trivia: Number.longValueExact, Math.toIntExact etc. - nice, rare.)
 */
public class QuickHitsDemo {

    public static void main(String[] args) {
        System.out.println("=== Arrays.setAll - fill an array with IntFunction ===");
        // Java 7:  for (int i = 0; i < tiers.length; i++) tiers[i] = 100L * i;  // loop + index bookkeeping
        // Java 8: hand Arrays a function of the index; it runs the loop for you:
        long[] tiers = new long[5];
        Arrays.setAll(tiers, i -> 100L * i);   // tier i = i * 100
        System.out.println("   fee tiers (cents): " + Arrays.toString(tiers));
        // parallelSetAll(tiers, i -> ...) does the same fill across threads - worthwhile only
        // when each slot is EXPENSIVE to compute. When parallelism pays off: module 07.

        System.out.println("=== Comparator recap: comparing + thenComparing, now inside List.sort ===");
        List<Transaction> statement = new ArrayList<>(Transaction.sampleStatement());
        // Read it out loud: "sort by city, THEN by amount within each city."
        statement.sort(Comparator.comparing(Transaction::getCity)
                                 .thenComparing(Transaction::getAmount));
        for (Transaction t : statement) {
            System.out.println("   " + t);
        }

        System.out.println("=== nullsFirst - sorting data that CONTAINS nulls ===");
        // comparing() alone throws NullPointerException the moment a key is null.
        // nullsFirst/nullsLast wrap a comparator and decide where nulls land:
        List<String> citiesWithGaps = new ArrayList<>(Arrays.asList("Dallas", null, "Austin", null));
        citiesWithGaps.sort(Comparator.nullsFirst(Comparator.<String>naturalOrder()));
        System.out.println("   nulls first: " + citiesWithGaps);
        citiesWithGaps.sort(Comparator.nullsLast(Comparator.<String>naturalOrder()));
        System.out.println("   nulls last : " + citiesWithGaps);
    }
}
