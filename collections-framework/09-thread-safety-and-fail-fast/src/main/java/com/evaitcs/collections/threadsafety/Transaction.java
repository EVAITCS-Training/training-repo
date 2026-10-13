package com.evaitcs.collections.threadsafety;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A SecureBank transaction — the ELEMENT this module iterates, removes and replaces.
 *
 * equals/hashCode by id (core-java module 08) and Comparable by id — the natural order
 * (core-java module 10). Both contracts are dormant in this module's demos (nothing here
 * sorts or dedupes), but the house rule is that Transaction always carries them, exactly
 * as built in core-java — and CheckThenActDemo's map lookups lean on equals-by-id.
 *
 * Money is BigDecimal (scale 2); the timestamp is long millis — java.time arrives in
 * java8-features/08-java-time.
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

    /**
     * Four transactions with ONE marked "Reversed" — the removal target of every
     * iteration demo below. Its position matters enormously: index 1 of 4 trips the
     * fail-fast detector reliably (ModCountAnatomyDemo banner 2), while a THREE-element
     * list with the target in the middle slips past it (banner 4 — the escape).
     */
    public static List<Transaction> sampleTransactions() {
        List<Transaction> transactions = new ArrayList<Transaction>();
        transactions.add(new Transaction("T-1001", "Salary", new BigDecimal("3100.00"), 1700000000000L));
        transactions.add(new Transaction("T-1002", "Reversed transfer", new BigDecimal("45.50"), 1700003600000L));
        transactions.add(new Transaction("T-1003", "Card payment", new BigDecimal("86.40"), 1700007200000L));
        transactions.add(new Transaction("T-1004", "Interest", new BigDecimal("12.34"), 1700010800000L));
        return transactions;
    }
}
