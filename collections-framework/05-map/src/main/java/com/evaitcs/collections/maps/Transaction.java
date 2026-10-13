package com.evaitcs.collections.maps;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A SecureBank transaction — the value class behind NavigableMapDemo's dated statement
 * (the TreeMap there is keyed by the transaction id, a String). Duplicated per module on
 * purpose; never import across modules.
 *
 * It carries BOTH contracts, deliberately in agreement (core-java 08 + 10):
 *   - equals/hashCode by id — the law for HashMap keys and for containsValue;
 *   - compareTo by id       — the law for TreeMap keys and TreeSet elements.
 * A TreeMap key with compareTo() == 0 but equals() false would "replace the value" —
 * module 04 (TreeSetOrderingDemo) showed that collapse on Sets; the Map version is the
 * same tree wearing a different hat.
 *
 * Field choices, deliberately "core": amount is BigDecimal (module 02), timestampMillis
 * is a plain long — no java.time until java8-features/08-java-time.
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
     * THE NATURAL ORDER: by id — only the SIGN is the contract, never -1/0/1.
     * Delegates to String.compareTo instead of subtracting: subtraction overflows
     * (core-java 10, SubtractionTrapDemo).
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

    /** Equal objects MUST hash equal (core-java 08) — HashMap lands keys in buckets with this. */
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
