package com.evaitcs.collections.sorting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A SecureBank statement line — the element this module sorts, searches, and shuffles.
 *
 * WHY it implements Comparable (by id): Collections.sort(list) with no comparator calls
 * compareTo on the ELEMENTS — the natural order from core-java module 10. This module
 * does not re-teach Comparable vs Comparator (or the subtraction trap — see core-java
 * 10's SubtractionTrapDemo); it CASHES them: every sort here is either the natural order
 * or an explicit Comparator handed in at the call site.
 *
 * WHY equals/hashCode are by id: Collections.frequency, indexOf, contains — the whole
 * toolbox — locate elements with equals(), never with == (module 01).
 *
 * WHY the sample statement contains a TIMESTAMP TIE: T-1001 and T-1004 share ts 2000.
 * Sorting by timestamp is only observable as STABLE if some elements tie — the tie is
 * the experiment, chosen on purpose. Duplicated per module; this module stands alone.
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

    /** Equal objects MUST hash equal (core-java 08) — frequency/indexOf live on this. */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " | " + description + " | " + amount.toPlainString() + " | ts=" + timestampMillis;
    }

    /**
     * Five statement lines in ARRIVAL order. Deliberately unsorted by every key except
     * id, and with a timestamp TIE (T-1001 and T-1004 both at ts 2000): a stable sort by
     * timestamp must leave T-1001 BEFORE T-1004 — that pair is StableSortDemo's proof.
     */
    public static List<Transaction> sampleStatement() {
        List<Transaction> statement = new ArrayList<Transaction>();
        statement.add(new Transaction("T-1001", "Bus ticket", new BigDecimal("2.75"), 2000L));
        statement.add(new Transaction("T-1002", "Coffee", new BigDecimal("4.50"), 1000L));
        statement.add(new Transaction("T-1003", "Groceries", new BigDecimal("86.40"), 3000L));
        statement.add(new Transaction("T-1004", "Rent", new BigDecimal("1200.00"), 2000L));
        statement.add(new Transaction("T-1005", "Salary", new BigDecimal("3500.00"), 500L));
        return statement;
    }
}
