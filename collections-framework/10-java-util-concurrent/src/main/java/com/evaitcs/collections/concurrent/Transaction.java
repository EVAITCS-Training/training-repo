package com.evaitcs.collections.concurrent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A SecureBank transaction — the ELEMENT this module puts under concurrent readers.
 *
 * equals/hashCode by id (core-java module 08) and Comparable by id — the natural order
 * (core-java module 10). Both contracts get cashed in this module: the natural order is
 * what a no-Comparator ConcurrentSkipListMap would sort by, while PriorityBlockingQueue
 * deliberately ignores it and takes a ByAmount Comparator instead — core-java 10's
 * "natural vs custom" lesson on a concurrent heap.
 *
 * Money is BigDecimal (scale 2); the timestamp is long millis — java.time arrives in
 * java8-features/08-java-time. Duplicated per module on purpose (house style).
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

    /** Identity = id: a re-imported feed row with the same id IS the same transaction. */
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

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Natural order = by id (core-java 10). Ids are zero-padded so String order IS numeric order. */
    @Override
    public int compareTo(Transaction other) {
        return id.compareTo(other.id);
    }

    @Override
    public String toString() {
        return id + " | " + description + " | " + amount.toPlainString();
    }

    /** Three alerts for the copy-on-write demos: written rarely, read on every event. */
    public static List<Transaction> sampleTransactions() {
        List<Transaction> transactions = new ArrayList<Transaction>();
        transactions.add(new Transaction("T-1001", "Large withdrawal alert", new BigDecimal("3100.00"), 1700000000000L));
        transactions.add(new Transaction("T-1002", "Foreign transfer alert", new BigDecimal("45.50"), 1700003600000L));
        transactions.add(new Transaction("T-1003", "Card payment alert", new BigDecimal("86.40"), 1700007200000L));
        return transactions;
    }
}
