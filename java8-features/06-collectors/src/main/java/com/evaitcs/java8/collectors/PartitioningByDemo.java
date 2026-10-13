package com.evaitcs.java8.collectors;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * partitioningBy — groupingBy's boolean special case, WITH A GUARANTEE.
 *
 * THE ONE SENTENCE: partitioningBy(predicate) splits the stream into exactly TWO buckets —
 * true and false — and BOTH keys are present in the result even when a bucket is EMPTY,
 * so result.get(true) is never null and needs no containsKey dance.
 *
 * That guarantee is the whole reason it exists separately from groupingBy(predicate):
 * groupingBy materializes only keys that caught something; partitioningBy always ships
 * the pair. Section 2 runs the SAME classifier through both and prints the difference.
 */
public class PartitioningByDemo {

    public static void main(String[] args) {
        List<Account> accounts = SampleBank.accounts();
        List<Transaction> transactions = SampleBank.transactions();

        banner("1. The natural split: deposits vs withdrawals");

        Map<Boolean, List<Transaction>> moneyFlow = transactions.stream()
                .collect(Collectors.partitioningBy(Transaction::isDeposit));
        System.out.println("IN  (true)  : " + moneyFlow.get(true).size() + " transactions");
        moneyFlow.get(true).forEach(t -> System.out.println("      " + t));
        System.out.println("OUT (false) : " + moneyFlow.get(false).size() + " transactions");
        System.out.println("No filter() run twice, no two lists to keep in sync — ONE pass,");
        System.out.println("both sides, already separated.");

        banner("2. The guarantee: ALWAYS two buckets, even an empty one");

        // No account in SampleBank is over 50,000 — the TRUE bucket will be empty.
        BigDecimal huge = new BigDecimal("50000.00");
        Map<Boolean, List<Account>> partitioned = accounts.stream()
                .collect(Collectors.partitioningBy(a -> a.getBalance().compareTo(huge) > 0));
        Map<Boolean, List<Account>> grouped = accounts.stream()
                .collect(Collectors.groupingBy(a -> a.getBalance().compareTo(huge) > 0));

        System.out.println("partitioningBy : " + sizes(partitioned) + "   <- key true EXISTS, bucket empty");
        System.out.println("groupingBy     : " + sizes(grouped) + "   <- key true DOESN'T EXIST at all");
        System.out.println("partitioned.get(true).isEmpty() is SAFE — no null check needed.");
        System.out.println("grouped.get(true) would be NULL. That one line is the difference,");
        System.out.println("and it's why 'which side won?' reports use partitioningBy.");

        banner("3. A threshold split: balances above/below 1000.00");

        BigDecimal threshold = new BigDecimal("1000.00");
        Map<Boolean, List<Account>> byThreshold = accounts.stream()
                .collect(Collectors.partitioningBy(a -> a.getBalance().compareTo(threshold) >= 0));
        System.out.println("AT OR ABOVE 1000.00 : " + byThreshold.get(true));
        System.out.println("BELOW 1000.00       : " + byThreshold.get(false));
        System.out.println("Exactly TWO groups — always. 'Small / medium / large' is NOT a");
        System.out.println("partition; three or more buckets is groupingBy territory.");

        banner("4. Downstream collectors work here too");

        // Same trick as groupingBy: a second argument replaces the default toList().
        Map<Boolean, Long> howMany = transactions.stream()
                .collect(Collectors.partitioningBy(Transaction::isDeposit, Collectors.counting()));
        Map<Boolean, BigDecimal> totals = transactions.stream()
                .collect(Collectors.partitioningBy(
                        Transaction::isDeposit,
                        Collectors.mapping(Transaction::getAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        System.out.println("count per side : " + howMany);
        System.out.println("money per side : IN " + totals.get(true) + "   OUT " + totals.get(false));
        System.out.println("A whole mini income-statement in one collect().");
    }

    /** Prints just the bucket sizes — full account lists would drown the point. */
    private static String sizes(Map<Boolean, List<Account>> m) {
        return m.keySet().stream().sorted()
                .map(k -> k + "=" + m.get(k).size())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
