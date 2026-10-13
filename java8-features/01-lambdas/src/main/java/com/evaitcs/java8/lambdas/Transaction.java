package com.evaitcs.java8.lambdas;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * The tiny banking domain used across the demos in this module.
 *
 * NOTE FOR CLASS: this is Java 8, so no records — a plain immutable class with getters.
 * (Records arrive in Java 16. We will write this same class as a record on the last day
 * and let students feel the difference.)
 */
public final class Transaction {

    private final String id;
    private final String description;
    private final BigDecimal amount; // money is ALWAYS BigDecimal, never double
    private final boolean deposit;   // true = money in, false = money out

    public Transaction(String id, String description, BigDecimal amount, boolean deposit) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.deposit = deposit;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public boolean isDeposit() { return deposit; }

    @Override
    public String toString() {
        return String.format("%s %-22s %10s %s",
                id, description, amount.toPlainString(), deposit ? "IN " : "OUT");
    }

    /** A fixed sample statement so every demo works with the same data. */
    public static List<Transaction> sampleStatement() {
        return Arrays.asList(
                new Transaction("T-101", "Payroll",          new BigDecimal("2500.00"), true),
                new Transaction("T-102", "Rent",             new BigDecimal("1200.00"), false),
                new Transaction("T-103", "Groceries",        new BigDecimal("86.40"),   false),
                new Transaction("T-104", "Interest",         new BigDecimal("3.11"),    true),
                new Transaction("T-105", "Cash withdrawal",  new BigDecimal("300.00"),  false),
                new Transaction("T-106", "Refund",           new BigDecimal("45.99"),   true));
    }
}
