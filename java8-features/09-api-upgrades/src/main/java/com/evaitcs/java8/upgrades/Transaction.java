package com.evaitcs.java8.upgrades;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * The tiny banking domain for this module - same spirit as module 01, but every
 * Transaction now carries a CITY, because the star of this module is grouping
 * "customers by city" with the upgraded Map API.
 *
 * NOTE FOR CLASS: this is Java 8 - no records, just an immutable class with getters.
 * Money is ALWAYS BigDecimal, never double.
 */
public final class Transaction {

    private final String id;
    private final String description;
    private final BigDecimal amount;
    private final boolean deposit;   // true = money in, false = money out
    private final String city;

    public Transaction(String id, String description, BigDecimal amount, boolean deposit, String city) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.deposit = deposit;
        this.city = city;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public boolean isDeposit() { return deposit; }
    public String getCity() { return city; }

    @Override
    public String toString() {
        return String.format("%s %-8s %-18s %10s %s",
                id, city, description, amount.toPlainString(), deposit ? "IN " : "OUT");
    }

    /** A fixed sample statement so every demo works with the same data. */
    public static List<Transaction> sampleStatement() {
        return Arrays.asList(
                new Transaction("T-101", "Payroll",         new BigDecimal("2500.00"), true,  "Austin"),
                new Transaction("T-102", "Rent",            new BigDecimal("1200.00"), false, "Austin"),
                new Transaction("T-103", "Groceries",       new BigDecimal("86.40"),   false, "Dallas"),
                new Transaction("T-104", "Interest",        new BigDecimal("3.11"),    true,  "Dallas"),
                new Transaction("T-105", "Cash withdrawal", new BigDecimal("300.00"),  false, "Houston"),
                new Transaction("T-106", "Refund",          new BigDecimal("45.99"),   true,  "Austin"));
    }
}
