package com.evaitcs.java8.collectors;

import java.math.BigDecimal;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The number-crunching collectors: counting, summing*, averaging*, summarizing*.
 *
 * THE ONE SENTENCE: these are SQL's COUNT / SUM / AVG as one-liners — and every one of
 * them can also run as a DOWNSTREAM inside groupingBy, which turns "one number for the
 * whole stream" into "one number PER BUCKET".
 *
 * Plus the money caveat: Java 8 has summingInt/Long/Double but NO summingBigDecimal
 * (still doesn't, even today). For money we compose mapping + reducing — shown in full
 * here because GroupingByDemo and PartitioningByDemo reuse that exact idiom.
 */
public class SummarizingDemo {

    public static void main(String[] args) {
        List<Customer> customers = SampleBank.customers();
        List<Transaction> transactions = SampleBank.transactions();

        banner("1. counting — COUNT(*)");

        long howMany = customers.stream().collect(Collectors.counting());
        long depositCount = transactions.stream()
                .filter(Transaction::isDeposit)
                .collect(Collectors.counting());
        System.out.println("customers                      : " + howMany);
        System.out.println("transactions that are deposits : " + depositCount + " of " + transactions.size());
        System.out.println("(stream.count() exists too — counting() earns its keep as a");
        System.out.println(" DOWNSTREAM of groupingBy: 'how many PER CITY'. GroupingByDemo.)");

        banner("2. summingInt / averagingInt — SUM and AVG");

        int totalAge = customers.stream().collect(Collectors.summingInt(Customer::getAge));
        double averageAge = customers.stream().collect(Collectors.averagingInt(Customer::getAge));
        System.out.println("summingInt(getAge)   : " + totalAge);
        System.out.println("averagingInt(getAge) : " + averageAge + "   <- a double");
        System.out.println("A double is fine for STATISTICS (an average age of 35.5 is honest).");
        System.out.println("For MONEY it re-introduces the binary-rounding bug — section 4.");

        banner("3. summarizingInt — all five numbers in ONE pass");

        IntSummaryStatistics stats = customers.stream()
                .collect(Collectors.summarizingInt(Customer::getAge));
        System.out.println("summarizingInt(getAge) : " + stats);
        System.out.println("count / sum / min / average / max — a getter for each, and");
        System.out.println("toString() prints the whole report for free. ONE traversal, FIVE");
        System.out.println("answers: the collector mutates a single statistics object as");
        System.out.println("elements go by — the mutable reduction of CollectAnatomyDemo.");

        banner("4. The BigDecimal caveat — money's sum is mapping + reducing");

        // There is no Collectors.summingBigDecimal — not in Java 8, not ever. And
        // summingDouble on money is how banks end up 0.01 off and auditors end up calling.
        // The Java 8 answer composes two collectors: mapping pulls the amount out of each
        // transaction, reducing folds the amounts with BigDecimal::add from ZERO.
        BigDecimal totalVolume = transactions.stream()
                .collect(Collectors.mapping(
                        Transaction::getAmount,
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)));
        System.out.println("total volume, all 15 transactions : " + totalVolume);

        BigDecimal depositVolume = transactions.stream()
                .filter(Transaction::isDeposit)
                .collect(Collectors.mapping(
                        Transaction::getAmount,
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)));
        System.out.println("money IN (deposits only)          : " + depositVolume);

        System.out.println("Read it as a sentence: 'MAP each transaction to its amount,");
        System.out.println("then REDUCE those amounts from ZERO with add.'");
        System.out.println("Scale survives: " + totalVolume.scale() + " decimal places — exact to the cent.");
        System.out.println("This exact pair, mapping(..., reducing(...)), is THE standard");
        System.out.println("downstream for MONEY inside groupingBy. Memorize the shape.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
