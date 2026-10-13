package com.evaitcs.java8.streams;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * SecureBank domain — a bank account owned by a customer.
 *
 * Java 8 only: no records, so an immutable class with getters.
 * Balance is BigDecimal — the same rule everywhere in this repo: money never touches double.
 */
public final class Account {

    private final String id;
    private final String customerId;        // FK-style link to Customer.getId()
    private final String type;              // CHECKING / SAVINGS
    private final BigDecimal balance;

    public Account(String id, String customerId, String type, BigDecimal balance) {
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.balance = balance;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getType() { return type; }
    public BigDecimal getBalance() { return balance; }

    @Override
    public String toString() {
        return String.format("%s owner %-4s %-8s balance %12s", id, customerId, type, balance.toPlainString());
    }

    /** A fixed set of accounts so every demo works with the same data. */
    public static List<Account> sampleAccounts() {
        return Arrays.asList(
                new Account("A-1001", "C-01", "CHECKING", new BigDecimal("4350.75")),
                new Account("A-1002", "C-01", "SAVINGS",  new BigDecimal("12000.00")),
                new Account("A-1003", "C-02", "CHECKING", new BigDecimal("890.10")),
                new Account("A-1004", "C-03", "SAVINGS",  new BigDecimal("5400.55")),
                new Account("A-1005", "C-04", "CHECKING", new BigDecimal("250.00")));
    }
}
