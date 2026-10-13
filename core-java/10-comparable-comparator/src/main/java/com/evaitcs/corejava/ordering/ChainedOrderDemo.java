package com.evaitcs.corejava.ordering;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Demo 6 — CHAINED orders, pre-Java-8: primary key, then tie-break, by hand.
 *
 * Real reports sort by several keys: "oldest first, and within the same timestamp,
 * smallest amount first". One comparator, one ladder of ifs — the idiom every
 * pre-Java-8 codebase is full of. Point at ByTimestampThenAmountThenId.java:
 * it is ten lines and each line is one business rule.
 */
public class ChainedOrderDemo {

    public static void main(String[] args) {
        banner("1. One key is not enough — ties happen in real data");

        // Same millisecond (a batch posting), inserted biggest-first on purpose.
        Transaction refund = new Transaction("T-1033", "Refund", new BigDecimal("45.99"), 1_700_000_200_000L);
        Transaction interest = new Transaction("T-1005", "Interest", new BigDecimal("3.11"), 1_700_000_200_000L);

        List<Transaction> oneKey = new ArrayList<Transaction>(Arrays.asList(refund, interest));
        Collections.sort(oneKey, new ByTimestamp());
        System.out.println("sorted by timestamp ONLY:");
        print(oneKey);
        System.out.println("compare() returned 0 — a TIE. The sort is stable, so the two kept");
        System.out.println("their INSERTION order. That is an accident of how the data arrived,");
        System.out.println("not a business rule. The statement order would change with the feed order.");

        banner("2. The hand-written ladder: timestamp, THEN amount, THEN id");

        // The whole "algorithm", as it reads in ByTimestampThenAmountThenId.compare():
        //   int byTime = Long.compare(t1.ts, t2.ts);      if (byTime != 0) return byTime;
        //   int byAmount = t1.amount.compareTo(t2.amount); if (byAmount != 0) return byAmount;
        //   return t1.id.compareTo(t2.id);
        // Each `if` is one business rule: "decide by X; only if X ties, look at Y."
        List<Transaction> chained = new ArrayList<Transaction>(Arrays.asList(refund, interest));
        Collections.sort(chained, new ByTimestampThenAmountThenId());
        System.out.println("sorted by the chained comparator:");
        print(chained);
        System.out.println("The tie is now broken BY AMOUNT (3.11 before 45.99) — deterministic,");
        System.out.println("no matter which order the data arrived in.");

        banner("3. Why the last rung is the unique key");

        List<Transaction> statement = Transaction.sampleStatement();
        Collections.sort(statement, new ByTimestampThenAmountThenId());
        print(statement);
        System.out.println("Ending the ladder with the UNIQUE id makes the order TOTAL: no two");
        System.out.println("different transactions can ever tie, so the sort is fully reproducible.");
        System.out.println();
        System.out.println("Java 8 collapses the whole ladder into ONE expression —");
        System.out.println("comparing(...).thenComparing(...).thenComparing(...) —");
        System.out.println("one glimpse of it in Java8PreviewDemo; the full story in ../java8-features.");
    }

    private static void print(List<Transaction> statement) {
        for (Transaction t : statement) {
            System.out.println("  " + t);
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
