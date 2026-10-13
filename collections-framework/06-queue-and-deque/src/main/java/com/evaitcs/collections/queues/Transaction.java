package com.evaitcs.collections.queues;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A SecureBank statement line — the element this module puts INTO queues.
 *
 * WHY it implements Comparable (by id): a PriorityQueue with no comparator falls back to
 * the elements' NATURAL order — exactly the contract core-java module 10 built. The fee
 * demos need a DIFFERENT order (by amount), which is why they hand the queue an explicit
 * Comparator instead; same class, two orders, one line of difference.
 *
 * WHY equals/hashCode are by id: Queue.remove(Object) and contains() locate elements with
 * equals(), never with == (module 01). Duplicated per module on purpose — this module
 * stands alone on screen.
 *
 * WHY amount is BigDecimal and timestamps are long millis: money is never float/double
 * (core-java module 02), and java.time arrives later, in java8-features/08-java-time.
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

    /** Equal objects MUST hash equal (core-java 08) — contains/remove(Object) live on this. */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " | " + description + " | " + amount.toPlainString();
    }

    /**
     * A morning's worth of statement lines, deliberately in arrival order — FIFO demos
     * need an order to preserve, and a queue is exactly "keep the arrival order".
     */
    public static List<Transaction> sampleStatement() {
        List<Transaction> statement = new ArrayList<Transaction>();
        statement.add(new Transaction("T-1041", "Payroll deposit", new BigDecimal("3500.00"), 1000L));
        statement.add(new Transaction("T-1042", "Rent transfer", new BigDecimal("1200.00"), 2000L));
        statement.add(new Transaction("T-1043", "Card payment", new BigDecimal("86.40"), 3000L));
        statement.add(new Transaction("T-1044", "ATM withdrawal", new BigDecimal("60.00"), 4000L));
        return statement;
    }

    /**
     * The bank's fee schedule as transactions. The ORDER of this list is part of the
     * lesson: offering these four amounts into a PriorityQueue builds a heap whose
     * array layout is NOT sorted — PriorityQueueDemo prints it as evidence.
     */
    public static List<Transaction> sampleFees() {
        List<Transaction> fees = new ArrayList<Transaction>();
        fees.add(new Transaction("T-2001", "ATM fee", new BigDecimal("8.00"), 5000L));
        fees.add(new Transaction("T-2002", "Card replacement fee", new BigDecimal("3.75"), 6000L));
        fees.add(new Transaction("T-2003", "Wire fee", new BigDecimal("12.50"), 7000L));
        fees.add(new Transaction("T-2004", "Paper statement fee", new BigDecimal("2.00"), 8000L));
        return fees;
    }
}
