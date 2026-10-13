package com.evaitcs.collections.concurrent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A SecureBank account — the VALUE this module hands to concurrent maps.
 *
 * equals/hashCode by id (built in core-java module 08) does real work here, twice:
 *   - CopyOnWriteArraySet dedupes through equals — a re-imported feed row with the same
 *     id is the SAME account and is refused (ConcurrentTourDemo banner 3);
 *   - the putIfAbsent race proof must compare results by IDENTITY (assertSame in the
 *     test) precisely BECAUSE equals-by-id cannot tell the two racers' values apart —
 *     a nice demonstration that the equals contract you choose decides what your tests
 *     can even assert.
 *
 * Money is BigDecimal (scale 2) — never float/double (core-java module 02). Duplicated
 * per module on purpose (house style): every module stands alone on screen.
 */
public class Account {

    private final String id;
    private final String owner;
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

    /** Identity = id. Two Account objects with the same id ARE the same account. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Account)) {
            return false;
        }
        Account that = (Account) other;
        return id.equals(that.id);
    }

    /** Equal objects MUST hash equal (core-java 08) — ConcurrentHashMap runs on this. */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " | " + owner + " | " + balance.toPlainString();
    }

    /** The same three customers since module 01 — now shared between threads. */
    public static List<Account> sampleAccounts() {
        List<Account> accounts = new ArrayList<Account>();
        accounts.add(new Account("ACC-1027", "Dana", new BigDecimal("2450.00")));
        accounts.add(new Account("ACC-1005", "Ana", new BigDecimal("86.40")));
        accounts.add(new Account("ACC-1011", "Bo", new BigDecimal("1200.00")));
        return accounts;
    }
}
