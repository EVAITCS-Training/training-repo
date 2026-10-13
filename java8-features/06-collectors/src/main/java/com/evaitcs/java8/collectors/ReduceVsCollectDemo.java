package com.evaitcs.java8.collectors;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * reduce vs collect — two reductions, one immutable, one mutable.
 *
 * THE ONE SENTENCE: reduce FOLDS elements into a new value step by step (nothing is ever
 * mutated — each step returns a fresh object), while collect ACCUMULATES into one mutable
 * container; use reduce for a single VALUE and collect for anything shaped like a
 * COLLECTION (lists, maps, strings, statistics).
 */
public class ReduceVsCollectDemo {

    public static void main(String[] args) {
        List<Transaction> transactions = SampleBank.transactions();

        banner("1. Same answer, two machines");

        // reduce: start at ZERO; for each amount, RETURN A NEW BigDecimal.
        // 15 transactions => 15 brand-new immutable objects, every one thrown away but the last.
        BigDecimal folded = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // collect: the reducing COLLECTOR does the same fold, packaged so it can live as
        // a downstream inside groupingBy/partitioningBy — you've already used it there.
        BigDecimal accumulated = transactions.stream()
                .collect(Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add));

        System.out.println("Stream.reduce       : " + folded);
        System.out.println("Collectors.reducing : " + accumulated);
        System.out.println("equal? " + (folded.compareTo(accumulated) == 0)
                + "   (compareTo, not equals — BigDecimal.equals compares SCALE too)");

        banner("2. WHY collect wins for containers: the concatenation proof");

        List<String> descriptions = transactions.stream()
                .map(Transaction::getDescription)
                .collect(Collectors.toList());

        // reduce + string concatenation: every step builds a WHOLE NEW String by copying
        // acc AND the next piece. n steps => O(n^2) characters copied. Correct, wasteful.
        String byReduce = descriptions.stream()
                .reduce("", (acc, d) -> acc + d + " ");

        // collect + StringBuilder: ONE mutable buffer; append is O(1) amortized. And the
        // three-arg collect is CollectAnatomyDemo's trio with zero ceremony:
        //   supplier    -> StringBuilder::new        (make the bucket)
        //   accumulator -> (sb, d) -> sb.append(..)  (drop one element in — MUTATES)
        //   combiner    -> StringBuilder::append     (merge buckets — parallel only, module 07)
        StringBuilder byCollect = descriptions.stream()
                .collect(StringBuilder::new,
                        (sb, d) -> sb.append(d).append(' '),
                        StringBuilder::append);

        System.out.println("reduce (+ at every step)   : " + byReduce.trim() + " ...");
        System.out.println("collect (one StringBuilder): " + byCollect.toString().trim() + " ...");
        System.out.println("identical? " + byReduce.contentEquals(byCollect));
        System.out.println("The strings match; the ALLOCATIONS don't: 15 fresh Strings vs 1 buffer.");
        System.out.println("(This is also why code review bans String + inside a loop.)");

        banner("3. The rule of thumb");

        System.out.println("reduce  : ONE immutable value (sum, min, combined config).");
        System.out.println("          Parallel-safe by construction — nothing shared, nothing mutated.");
        System.out.println("collect : a CONTAINER built by mutation (list, set, map, string, stats),");
        System.out.println("          and the ONLY way groupingBy-style structured results exist.");
        System.out.println("When in doubt: value -> reduce, collection -> collect.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
