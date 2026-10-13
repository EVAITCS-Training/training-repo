package com.evaitcs.java8.collectors;

import java.math.BigDecimal;

/**
 * A SecureBank account. Money is ALWAYS BigDecimal with scale 2 — never double.
 * (Double stores 0.1 as an approximation; a bank that rounds wrong is a lawsuit —
 * and SummarizingDemo shows what that means for collectors: no summingBigDecimal
 * exists, so money sums go through mapping + reducing.)
 *
 * `type` is the INNER key of this module's multi-level groupingBy report:
 * accounts by city, THEN by type — a nested GROUP BY in one expression.
 */
public final class Account {

    /** The account type — a nested enum keeps the domain self-contained. */
    public enum Type { CHECKING, SAVINGS }

    private final String id;
    private final String ownerId;   // foreign key into Customer — GroupingByDemo "joins" via toMap
    private final Type type;
    private final BigDecimal balance;

    public Account(String id, String ownerId, Type type, BigDecimal balance) {
        this.id = id;
        this.ownerId = ownerId;
        this.type = type;
        this.balance = balance;
    }

    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public Type getType() { return type; }
    public BigDecimal getBalance() { return balance; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        return id.equals(((Account) o).id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return String.format("%s %-9s %-8s %12s", id, ownerId, type, balance.toPlainString());
    }
}
