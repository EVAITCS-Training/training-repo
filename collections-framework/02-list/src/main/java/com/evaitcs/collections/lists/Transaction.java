package com.evaitcs.collections.lists;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A SecureBank transaction — the element every List in this module holds.
 *
 * WHY equals/hashCode BY ID (the contract core-java 08 built): half of the List API locates
 * elements by EQUALITY, not identity — contains(t), indexOf(t), remove(Object). ListSurgeryDemo
 * depends on it: a freshly constructed Transaction with the same id finds (and removes) the one
 * sitting in the statement. Module 01 proved the engine for contains; here it drives the whole
 * search API.
 *
 * WHY amount is BigDecimal at scale 2: money is never float/double (core-java 02). Comparisons
 * use compareTo, never equals — "3.75" and "3.750" are equals()-different but compareTo-equal
 * (the consistency lesson from core-java 10).
 *
 * WHY timestampMillis is a plain long: no java.time in this course yet — that arrives in
 * java8-features/08-java-time.
 *
 * Duplicated per module on purpose (like module 01's Account): each module stands alone on
 * screen and never imports from a sibling.
 */
public class Transaction {

    private final String id;
    private final String description;
    private final BigDecimal amount;
    private final long timestampMillis;

    public Transaction(String id, String description, BigDecimal amount, long timestampMillis) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.timestampMillis = timestampMillis;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    /** Identity = id. Two Transaction objects with the same id ARE the same transaction. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Transaction)) {
            return false;
        }
        Transaction that = (Transaction) other;
        return id.equals(that.id);
    }

    /** Equal objects MUST hash equal (core-java 08) — List.hashCode lives on this too. */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " | " + description + " | " + amount.toPlainString();
    }

    /**
     * A week of statement, chronologically ordered, as a FRESH mutable ArrayList — so every
     * demo can perform surgery on its own copy without disturbing the others. The timestamps
     * are fixed epoch millis (never System.currentTimeMillis()): all output stays deterministic.
     */
    public static List<Transaction> sampleStatement() {
        long day = 86_400_000L;
        long base = 1_700_000_000_000L;
        List<Transaction> statement = new ArrayList<Transaction>();
        statement.add(new Transaction("TXN-1001", "Payroll — SecureBank", new BigDecimal("2500.00"), base));
        statement.add(new Transaction("TXN-1002", "Rent", new BigDecimal("900.00"), base + day));
        statement.add(new Transaction("TXN-1003", "Groceries", new BigDecimal("86.40"), base + 2 * day));
        statement.add(new Transaction("TXN-1004", "ATM withdrawal", new BigDecimal("120.00"), base + 3 * day));
        statement.add(new Transaction("TXN-1005", "Interest", new BigDecimal("3.75"), base + 4 * day));
        statement.add(new Transaction("TXN-1006", "Insurance", new BigDecimal("210.50"), base + 5 * day));
        return statement;
    }
}
