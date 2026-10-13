package com.evaitcs.collections.choosing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A SecureBank statement line — used by the decision-flowchart scenarios that need a
 * sequence payload (statement lines, task queue, fee heap).
 *
 * Comparable and equals/hashCode BY ID, exactly as built in core-java 08/10 and repeated
 * in every module of this course (duplicated per module on purpose — each module stands
 * alone on screen). The scenarios in this module care about CONTAINER choice, not about
 * ordering subtleties: the fee scenario hands a PriorityQueue an anonymous comparator by
 * amount, which is precisely why the natural order stays by id.
 *
 * WHY amount is BigDecimal: money is never float/double (core-java module 02).
 * WHY timestamps are long millis: java.time arrives in java8-features/08-java-time.
 */
public class Transaction implements Comparable<Transaction> {

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

    /** Natural order = by id (core-java 10). Zero-padded ids keep lex order == numeric order. */
    @Override
    public int compareTo(Transaction other) {
        return id.compareTo(other.id);
    }

    /** Identity = id. Two Transaction objects with the same id ARE the same statement line. */
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

    /** Equal objects MUST hash equal (core-java 08). */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " | " + description + " | " + amount.toPlainString();
    }

    /** A tiny morning statement, in arrival order — chronological, duplicates allowed. */
    public static List<Transaction> sampleStatement() {
        List<Transaction> statement = new ArrayList<Transaction>();
        statement.add(new Transaction("T-1001", "Bus ticket", new BigDecimal("2.75"), 1000L));
        statement.add(new Transaction("T-1002", "Bus ticket", new BigDecimal("2.75"), 2000L));
        statement.add(new Transaction("T-1003", "Coffee", new BigDecimal("4.50"), 3000L));
        return statement;
    }
}
