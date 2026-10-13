package com.evaitcs.collections.sets;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A SecureBank transaction — the element this module puts into Sets.
 *
 * The class carries BOTH contracts this module cashes in, and they deliberately AGREE:
 *   - equals/hashCode by id (core-java 08) — the law for HashSet/LinkedHashSet;
 *   - compareTo by id (core-java 10)       — the law for TreeSet.
 * When the two disagree, a TreeSet and a HashSet holding the SAME objects disagree about
 * what a duplicate is. TreeSetOrderingDemo forces exactly that disagreement (a ByAmount
 * comparator) so students see TreeSet consult compareTo and NEVER equals.
 *
 * Field choices, deliberately "core":
 *   - amount is BigDecimal (module 02: money is never float/double) — and BigDecimal's
 *     own equals/compareTo split (scale vs value) is the star trap of this module.
 *   - timestampMillis is a plain long (epoch milliseconds): this course has not taught
 *     java.time yet — that arrives in java8-features/08-java-time.
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

    /**
     * THE NATURAL ORDER: by id — a statement has one canonical order, so it lives
     * inside the class (core-java 10). Only the SIGN is the contract, never -1/0/1.
     * Delegates to String.compareTo instead of subtracting: subtraction overflows.
     */
    @Override
    public int compareTo(Transaction other) {
        return id.compareTo(other.id);
    }

    /** Identity = id — and it AGREES with compareTo (compareTo() == 0 exactly when equals). */
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

    /** Equal objects MUST hash equal (core-java 08) — HashSet lands duplicates in one bucket with this. */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " | " + description + " | " + amount.toPlainString() + " | " + new Date(timestampMillis);
    }

    /** One realistic statement, deliberately NOT pre-sorted by anything — five DISTINCT ids. */
    public static List<Transaction> sampleStatement() {
        List<Transaction> statement = new ArrayList<Transaction>();
        statement.add(new Transaction("T-1040", "Payroll",   new BigDecimal("2450.00"), 1_700_000_400_000L));
        statement.add(new Transaction("T-1011", "Rent",      new BigDecimal("1200.00"), 1_700_000_100_000L));
        statement.add(new Transaction("T-1027", "Groceries", new BigDecimal("86.40"),   1_700_000_300_000L));
        statement.add(new Transaction("T-1005", "Interest",  new BigDecimal("3.11"),    1_700_000_200_000L));
        statement.add(new Transaction("T-1033", "Refund",    new BigDecimal("45.99"),   1_700_000_200_000L));
        return statement;
    }
}
