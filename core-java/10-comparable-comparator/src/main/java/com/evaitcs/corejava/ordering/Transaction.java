package com.evaitcs.corejava.ordering;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A SecureBank transaction — the class this whole module sorts.
 *
 * WHY Transaction implements Comparable<Transaction>:
 * a bank statement has ONE canonical order (by id), so it earns a NATURAL order —
 * the order the class carries with itself, everywhere, forever. Every other order
 * (by amount, by date, ...) is a VIEW, and views live OUTSIDE the class as Comparators.
 *
 * Field choices, deliberately "core":
 *   - amount is BigDecimal (module 02: money is never float/double).
 *   - timestampMillis is a plain long (epoch milliseconds) instead of java.util.Date or
 *     java.time: this course has not taught java.time yet, and a long is exactly the
 *     kind of big number that exposes the subtraction overflow trap (SubtractionTrapDemo).
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
     * THE NATURAL ORDER: by id.
     *
     * The contract in one line: return a NEGATIVE int if this < other, ZERO if they tie,
     * a POSITIVE int if this > other. Only the SIGN is part of the contract — never
     * promise -1/0/1 (CompareToContractDemo prints real values like -4).
     *
     * WHY we delegate to String.compareTo (module 03: lexicographic, char by char)
     * instead of subtracting: subtraction overflows and flips the sign on big numbers.
     * See SubtractionTrapDemo for the wreckage.
     *
     * Module 08 cross-ref: we deliberately do NOT override equals here, so
     * compareTo() == 0 does NOT imply equals(). Sorted collections (TreeSet/TreeMap)
     * consult compareTo ONLY — CompareToContractDemo shows a TreeSet "losing" one of
     * two same-id transactions as a consequence. In production, if the id IS the
     * identity, give the class an id-based equals/hashCode so the two agree.
     */
    @Override
    public int compareTo(Transaction other) {
        return id.compareTo(other.id);
    }

    @Override
    public String toString() {
        return id + " | " + description + " | " + amount.toPlainString() + " | " + new Date(timestampMillis);
    }

    /**
     * One realistic statement, deliberately NOT pre-sorted by anything.
     * Interest and Refund share a timestamp on purpose — that tie is what
     * ChainedOrderDemo breaks with a second key.
     */
    public static List<Transaction> sampleStatement() {
        List<Transaction> statement = new ArrayList<Transaction>();
        statement.add(new Transaction("T-1040", "Payroll",  new BigDecimal("2450.00"), 1_700_000_400_000L));
        statement.add(new Transaction("T-1011", "Rent",     new BigDecimal("1200.00"), 1_700_000_100_000L));
        statement.add(new Transaction("T-1027", "Groceries", new BigDecimal("86.40"),  1_700_000_300_000L));
        statement.add(new Transaction("T-1005", "Interest", new BigDecimal("3.11"),    1_700_000_200_000L));
        statement.add(new Transaction("T-1033", "Refund",   new BigDecimal("45.99"),   1_700_000_200_000L));
        return statement;
    }
}
