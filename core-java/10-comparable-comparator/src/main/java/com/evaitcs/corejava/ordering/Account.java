package com.evaitcs.corejava.ordering;

import java.math.BigDecimal;

/**
 * A SecureBank account — deliberately does NOT implement Comparable.
 *
 * WHY it stays unsorted-on-purpose:
 *   1. ReverseAndNullsDemo sorts accounts with Comparators defined OUTSIDE the class,
 *      proving that sorting with a comparator never requires Comparable.
 *   2. owner is nullable (think: a joint account not yet registered) — that null is
 *      the grenade the null demos throw, on purpose, where everyone can see it.
 */
public class Account {

    private final String id;
    private final String owner; // may be null — see ReverseAndNullsDemo
    private final BigDecimal balance;

    public Account(String id, String owner, BigDecimal balance) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    @Override
    public String toString() {
        return id + " | owner=" + owner + " | " + balance.toPlainString();
    }
}
