package com.evaitcs.java8.streams;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * THE OPERATIONS TOUR — the map of the territory, not the territory.
 *
 * Every common intermediate and terminal op, once, with printed results, so students
 * recognize the vocabulary. The deep dives come later: reduce and collect get whole
 * modules (05 and 06). Rule of thumb repeated in class:
 *   intermediate = lazy, Stream -> Stream     |     terminal = eager, Stream -> result
 */
public class OperationsTourDemo {

    public static void main(String[] args) {

        List<Transaction> statement = Transaction.sampleStatement();
        List<Account> accounts = Account.sampleAccounts();

        banner("INTERMEDIATE ops — each returns a new Stream, all lazy");

        // filter — keep what matches a Predicate.
        System.out.println("filter(isDeposit)      : " + statement.stream()
                .filter(Transaction::isDeposit).map(Transaction::getId)
                .collect(Collectors.joining(", ")));

        // map — transform each element with a Function.
        System.out.println("map(getDescription)    : " + statement.stream()
                .map(Transaction::getDescription).collect(Collectors.joining(", ")));

        // sorted — reorder; needs a Comparator for non-natural types (module 01!).
        System.out.println("sorted(by balance desc): " + accounts.stream()
                .sorted(Comparator.comparing(Account::getBalance).reversed())
                .map(Account::getId).collect(Collectors.joining(", ")));

        // distinct — drop duplicates, using equals().
        System.out.println("distinct() account types: " + accounts.stream()
                .map(Account::getType).distinct().collect(Collectors.joining(", ")));

        // limit / skip — take the first N / throw away the first N.
        System.out.println("limit(2)               : " + statement.stream()
                .limit(2).map(Transaction::getId).collect(Collectors.joining(", ")));
        System.out.println("skip(4)                : " + statement.stream()
                .skip(4).map(Transaction::getId).collect(Collectors.joining(", ")));

        // peek — run a side effect on every element WITHOUT changing it.
        // WARNING to give out loud: peek is for DEBUGGING (like the counters in the
        // laziness demo). Business logic in peek is a pipeline that only works when
        // someone is watching — the moment the terminal op changes, behavior shifts.
        System.out.print("peek(log each id)      : ");
        statement.stream().limit(3)
                .peek(t -> System.out.print("[peek " + t.getId() + "] "))
                .forEach(t -> System.out.print(t.getDescription() + " "));
        System.out.println();

        banner("TERMINAL ops — each consumes the stream, each returns a non-Stream");

        // forEach — do something per element, return nothing.
        System.out.println("forEach(print top deposit):");
        statement.stream().filter(Transaction::isDeposit).limit(1)
                .forEach(t -> System.out.println("  " + t));

        // count — how many survive to the end.
        System.out.println("count(outs)            : " + statement.stream()
                .filter(t -> !t.isDeposit()).count());

        // collect — gather into a collection. Just a preview: module 06 is ALL about this.
        System.out.println("collect(toList)        : " + statement.stream()
                .map(Transaction::getId).collect(Collectors.toList()));

        // reduce — fold elements into one value. Preview only; module 05 goes deep.
        BigDecimal totalOut = statement.stream()
                .filter(t -> !t.isDeposit())
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("reduce(sum of OUTs)    : " + totalOut);

        // min / max — the extremes, according to a Comparator. Both return Optional!
        Optional<Account> richest = accounts.stream()
                .max(Comparator.comparing(Account::getBalance));
        System.out.println("max(by balance)        : " + richest.map(Account::getId).orElse("(none)")
                + "   <- Optional because the stream might be EMPTY");

        // anyMatch / allMatch / noneMatch — boolean questions, all short-circuiting.
        boolean anyBig = statement.stream().anyMatch(t -> t.getAmount().compareTo(new BigDecimal("1000")) > 0);
        boolean allPositive = statement.stream().allMatch(t -> t.getAmount().signum() > 0);
        boolean noneHuge = statement.stream().noneMatch(t -> t.getAmount().compareTo(new BigDecimal("100000")) > 0);
        System.out.println("anyMatch(> 1000)       : " + anyBig);
        System.out.println("allMatch(amount > 0)   : " + allPositive);
        System.out.println("noneMatch(> 100000)    : " + noneHuge);

        // findFirst — the first survivor, or an empty Optional.
        Optional<Transaction> firstOut = statement.stream()
                .filter(t -> !t.isDeposit())
                .findFirst();
        System.out.println("findFirst(!deposit)    : " + firstOut.map(Transaction::getId).orElse("(none)"));
        System.out.println("(There is also findAny — it matters for parallel streams, module 07.)");
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
