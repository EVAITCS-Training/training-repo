package com.evaitcs.java8.collectors;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * joining — glue for readable output — and collectingAndThen, the finisher.
 *
 * THE ONE SENTENCE: joining concatenates the string elements of a stream (delimiter,
 * optional prefix/suffix), and collectingAndThen(collector, finisher) runs ONE LAST
 * function on the finished container — Java 8's way to freeze a result or re-shape it.
 */
public class JoiningDemo {

    public static void main(String[] args) {
        List<Transaction> transactions = SampleBank.transactions();

        banner("1. joining(delimiter) — the CSV one-liner");

        // joining only accepts CharSequence elements — map to strings FIRST.
        // (The old pre-Java-8 way was StringBuilder + a loop + trimming the last comma.)
        String ids = transactions.stream()
                .map(Transaction::getId)
                .collect(Collectors.joining(", "));
        System.out.println(ids);

        banner("2. prefix + suffix — a real statement line");

        // Account A1 has exactly three transactions in SampleBank: payroll in, rent out,
        // cash withdrawal out. One pipeline turns them into a printable statement.
        String statement = transactions.stream()
                .filter(t -> t.getAccountId().equals("A1"))
                .map(JoiningDemo::format)
                .collect(Collectors.joining(" | ", "Statement A1 -> [", "]"));
        System.out.println(statement);

        // As a DOWNSTREAM, joining formats every bucket at once — one statement per
        // account, whole bank, single collect(). This is mapping + joining nested inside
        // groupingBy, the same composition trick as mapping + reducing for money.
        Map<String, String> statementByAccount = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getAccountId,
                        Collectors.mapping(JoiningDemo::format, Collectors.joining("; "))));
        statementByAccount.forEach((id, line) -> System.out.println("   " + id + " : " + line));

        banner("3. collectingAndThen — collect, THEN one finishing move");

        // (a) Count, then format: the finisher turns the Long into display text —
        //     the collector's result type becomes the finisher's return type.
        String summary = transactions.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.counting(),
                        n -> n + " transactions processed"));
        System.out.println(summary);

        // (b) Freeze the result. Java 8 has NO toUnmodifiableList() (that's Java 10) —
        //     collectingAndThen(toList(), Collections::unmodifiableList) is the legal
        //     Java 8 equivalent: build a normal list, then hand out a frozen VIEW.
        List<String> frozen = SampleBank.customers().stream()
                .map(Customer::getName)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(), Collections::unmodifiableList));
        try {
            frozen.add("Not Allowed");
        } catch (UnsupportedOperationException e) {
            System.out.println("frozen.add(...) -> UnsupportedOperationException. Read-only.");
        }
        System.out.println("frozen still READS fine: " + frozen.size() + " names, first = " + frozen.get(0));
        System.out.println("The finisher runs ONCE, on the finished container — that's the 'then'.");
    }

    /** "Payroll +2500.00" / "Rent -1200.00" — the sign comes from the deposit FLAG, never from arithmetic. */
    private static String format(Transaction t) {
        return String.format("%s %s%s", t.getDescription(), t.isDeposit() ? "+" : "-", t.getAmount());
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
