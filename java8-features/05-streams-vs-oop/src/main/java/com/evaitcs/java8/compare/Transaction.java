package com.evaitcs.java8.compare;

import java.math.BigDecimal;

/**
 * A single transaction on an account. Immutable like the rest of the domain.
 *
 * NOTE FOR CLASS: `deposit` is a boolean, not a sign on the amount. Keeping the
 * direction in its own field means the filter predicate in TaskFilter reads like
 * the business rule ("is a deposit AND over 100") instead of like arithmetic.
 */
public final class Transaction {

    private final String id;
    private final String accountId;
    private final String description;
    private final BigDecimal amount;
    private final boolean deposit; // true = money in, false = money out

    public Transaction(String id, String accountId, String description, BigDecimal amount, boolean deposit) {
        this.id = id;
        this.accountId = accountId;
        this.description = description;
        this.amount = amount;
        this.deposit = deposit;
    }

    public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public boolean isDeposit() { return deposit; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        return id.equals(((Transaction) o).id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return String.format("%s %-4s %-22s %10s %s",
                id, accountId, description, amount.toPlainString(), deposit ? "IN " : "OUT");
    }
}
