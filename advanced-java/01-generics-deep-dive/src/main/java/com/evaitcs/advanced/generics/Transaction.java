package com.evaitcs.advanced.generics;

import java.math.BigDecimal;

/**
 * A SecureBank transaction — this module's "element type" for every generic container.
 *
 * WHY it implements Comparable<Transaction>:
 * BridgeMethodsDemo needs a class whose generic interface implementation forces the
 * compiler to generate a BRIDGE METHOD — the cleanest proof that erasure really happens.
 * The order (by id) is the natural order built in core-java module 10.
 *
 * equals/hashCode by id (core-java module 08): the collections modules downstream of
 * this course all run on that contract.
 */
public class Transaction implements Comparable<Transaction> {

    private final String id;
    private final String description;
    private final BigDecimal amount;

    public Transaction(String id, String description, BigDecimal amount) {
        this.id = id;
        this.description = description;
        this.amount = amount;
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

    /** The natural order: by id (core-java 10). Only the SIGN of the result is contract. */
    @Override
    public int compareTo(Transaction other) {
        return id.compareTo(other.id);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Transaction that)) {   // Java 21 pattern matching for instanceof
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " | " + description + " | " + amount.toPlainString();
    }
}
