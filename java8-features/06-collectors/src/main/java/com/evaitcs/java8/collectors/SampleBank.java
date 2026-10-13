package com.evaitcs.java8.collectors;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * The fixed sample world every demo and test in this module runs against.
 * Deliberately the SAME data as module 05's SampleBank — same six customers, same
 * eight accounts, same fifteen transactions.
 *
 * WHY THE SAME DATA: module 05's TaskCount hand-wrote the containsKey/get/put dance
 * for "how many customers per city" and teased that module 06 does it in ONE line.
 * Running groupingBy on the identical dataset makes that promise CHECKABLE — the test
 * asserts the one-liner returns exactly the counts the plumbing produced
 * (Austin 3, Denver 1, Seattle 2).
 *
 * The data is chosen so every collector has something interesting to bite on:
 *   - THREE customers in Austin           (toMap's duplicate-key explosion; groupingBy buckets)
 *   - unique customer ids                 (the SAFE toMap key — the id -> name lookup table)
 *   - three cities, uneven counts         (groupingBy: Austin 3, Denver 1, Seattle 2)
 *   - Denver owns only a SAVINGS account  (nested groupingBy: the inner map is MISSING a key)
 *   - no balance anywhere above 50,000    (partitioningBy: empty TRUE bucket — still present!)
 *   - balances around the 1000.00 line    (partitioningBy threshold split: 5 above, 3 below)
 *   - 7 deposits / 8 withdrawals          (partitioningBy money-flow demo)
 *   - amounts with cents, all scale 2     (BigDecimal summing via mapping + reducing: 41231.01)
 *   - account A1 has exactly 3 transactions (joining: a tidy one-line statement)
 *   - ages 24..55                         (summarizingInt: min 24, max 55, sum 213, avg 35.5)
 */
public final class SampleBank {

    private SampleBank() { } // static-only: nobody should ever instantiate the bank

    public static List<Customer> customers() {
        return Arrays.asList(
                new Customer("C1", "Ada Lovelace",      36, "Austin"),
                new Customer("C2", "Grace Hopper",      28, "Austin"),
                new Customer("C3", "Alan Turing",       41, "Denver"),
                new Customer("C4", "Katherine Johnson", 24, "Austin"),
                new Customer("C5", "Linus Pauling",     55, "Seattle"),
                new Customer("C6", "Barbara Liskov",    29, "Seattle"));
    }

    public static List<Account> accounts() {
        return Arrays.asList(
                new Account("A1", "C1", Account.Type.CHECKING, new BigDecimal("2500.00")),
                new Account("A2", "C1", Account.Type.SAVINGS,  new BigDecimal("12000.50")),
                new Account("A3", "C2", Account.Type.CHECKING, new BigDecimal("840.25")),
                new Account("A4", "C3", Account.Type.SAVINGS,  new BigDecimal("31000.00")),
                new Account("A5", "C4", Account.Type.CHECKING, new BigDecimal("150.75")),
                new Account("A6", "C5", Account.Type.CHECKING, new BigDecimal("4200.00")),
                new Account("A7", "C5", Account.Type.SAVINGS,  new BigDecimal("8750.25")),
                new Account("A8", "C6", Account.Type.CHECKING, new BigDecimal("990.10")));
    }

    public static List<Transaction> transactions() {
        return Arrays.asList(
                new Transaction("T01", "A1", "Payroll",              new BigDecimal("2500.00"), true),
                new Transaction("T02", "A1", "Rent",                 new BigDecimal("1200.00"), false),
                new Transaction("T03", "A3", "Groceries",            new BigDecimal("86.40"),   false),
                new Transaction("T04", "A3", "Interest",             new BigDecimal("3.11"),    true),
                new Transaction("T05", "A2", "Transfer to savings",  new BigDecimal("500.00"),  true),
                new Transaction("T06", "A4", "Certificate deposit",  new BigDecimal("25000.00"),true),
                new Transaction("T07", "A4", "ATM withdrawal",       new BigDecimal("300.00"),  false),
                new Transaction("T08", "A5", "Coffee",               new BigDecimal("4.75"),    false),
                new Transaction("T09", "A5", "Refund",               new BigDecimal("45.99"),   true),
                new Transaction("T10", "A6", "Wire received",        new BigDecimal("5400.00"), true),
                new Transaction("T11", "A6", "Utility bill",         new BigDecimal("210.33"),  false),
                new Transaction("T12", "A7", "Dividend",             new BigDecimal("120.44"),  true),
                new Transaction("T13", "A2", "Wire sent",            new BigDecimal("3200.00"), false),
                new Transaction("T14", "A8", "Online purchase",      new BigDecimal("59.99"),   false),
                new Transaction("T15", "A1", "Cash withdrawal",      new BigDecimal("2600.00"), false));
    }
}
