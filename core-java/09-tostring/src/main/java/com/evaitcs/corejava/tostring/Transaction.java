package com.evaitcs.corejava.tostring;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * A bank transaction — the element used by CollectionsToStringDemo.
 *
 * Its toString is deliberately COMPACT ("Transaction[T-2001 Payroll +2450.00]"):
 * when a collection prints, every element's toString runs and the results are glued
 * into one line — a verbose element toString turns a 5-element list into a wall of
 * text. One line per object is the right résumé length when objects travel in lists.
 *
 * The +/- sign comes from the `deposit` flag so a statement reads like a statement.
 * amount.toPlainString() keeps money free of scientific notation (module 02).
 */
public class Transaction {

    private final String id;
    private final String description;
    private final BigDecimal amount; // money is ALWAYS BigDecimal, never double
    private final boolean deposit;   // true = money in, false = money out

    public Transaction(String id, String description, BigDecimal amount, boolean deposit) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.deposit = deposit;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public boolean isDeposit() { return deposit; }

    @Override
    public String toString() {
        return "Transaction[" + id + " " + description + " "
                + (deposit ? "+" : "-") + amount.toPlainString() + "]";
    }

    /** A fixed sample statement so every demo and test works with the same data. */
    public static List<Transaction> sampleStatement() {
        return Arrays.asList(
                new Transaction("T-2001", "Payroll",   new BigDecimal("2450.00"), true),
                new Transaction("T-2002", "Rent",      new BigDecimal("1200.00"), false),
                new Transaction("T-2003", "Groceries", new BigDecimal("86.40"),   false),
                new Transaction("T-2004", "Interest",  new BigDecimal("3.11"),    true));
    }
}
