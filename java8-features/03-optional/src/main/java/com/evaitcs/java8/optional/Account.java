package com.evaitcs.java8.optional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A SecureBank account — the tiny domain every demo in this module shares.
 *
 * NOTE FOR CLASS: Java 8, so no records — plain immutable class with getters.
 * Money is ALWAYS BigDecimal at scale 2, never double.
 *
 * DESIGN POINT (rule #1 of the README): the creditLimit FIELD is a plain, nullable
 * BigDecimal — Optional must never be a field (it is not Serializable and adds a wrapper
 * object to every instance). The GETTER returns Optional&lt;BigDecimal&gt;: it is the
 * *return type* that tells callers "this may legally be absent".
 */
public final class Account {

    private final String id;
    private final String owner;
    private final BigDecimal balance;     // money is ALWAYS BigDecimal, scale 2
    private final BigDecimal creditLimit; // nullable: debit/savings accounts legally have none

    public Account(String id, String owner, BigDecimal balance, BigDecimal creditLimit) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
        this.creditLimit = creditLimit;
    }

    public String getId() { return id; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }

    /** The recommended shape: an Optional return type makes "may be absent" part of the signature. */
    public Optional<BigDecimal> getCreditLimit() {
        return Optional.ofNullable(creditLimit);
    }

    /**
     * Raw nullable accessor — kept ONLY so TheNullProblemDemo can show what pre-Java-8
     * callers lived with. Do not write getters like this in new code; the whole point of
     * the module is that the type should say "may be null", and this one cannot.
     */
    public BigDecimal getNullableCreditLimit() { return creditLimit; }

    @Override
    public String toString() {
        return String.format("%s %-13s balance=%9s limit=%s",
                id, owner, balance.toPlainString(),
                creditLimit == null ? "none" : creditLimit.toPlainString());
    }

    /** Fixed seed data so every demo (and test) works against the same three accounts. */
    public static List<Account> seedAccounts() {
        return Arrays.asList(
                new Account("ACC-1001", "Alice Nguyen", new BigDecimal("2500.00"), new BigDecimal("5000.00")),
                new Account("ACC-1002", "Bob Rivera",   new BigDecimal("40.17"),   null), // debit: no credit line
                new Account("ACC-1003", "Carol Okafor", new BigDecimal("-120.00"), new BigDecimal("1500.00")));
    }
}
