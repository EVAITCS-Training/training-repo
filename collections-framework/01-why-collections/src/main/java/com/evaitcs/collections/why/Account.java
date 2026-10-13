package com.evaitcs.collections.why;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A SecureBank account — the element this module puts INTO containers.
 *
 * WHY it overrides equals/hashCode (by id): half of the Collections Framework runs on
 * those two methods. contains(), remove(), List.indexOf(), every Set, every Map key —
 * they all ask elements "are you equal?" instead of "are you the same object?".
 * This is the harvest of core-java module 08; without it, the contains/remove proofs
 * in CollectionApiDemo would silently fail and students would learn the wrong lesson.
 *
 * WHY balance is BigDecimal: money is never float/double (core-java module 02).
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

    /** Equal objects MUST hash equal (core-java 08) — the Set/Map modules live on this. */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " | " + owner + " | " + balance.toPlainString();
    }

    /** Three customers, deliberately unsorted — order is a property of the CONTAINER, not the data. */
    public static List<Account> sampleAccounts() {
        List<Account> accounts = new ArrayList<Account>();
        accounts.add(new Account("ACC-1027", "Dana", new BigDecimal("2450.00")));
        accounts.add(new Account("ACC-1005", "Ana", new BigDecimal("86.40")));
        accounts.add(new Account("ACC-1011", "Bo", new BigDecimal("1200.00")));
        return accounts;
    }
}
