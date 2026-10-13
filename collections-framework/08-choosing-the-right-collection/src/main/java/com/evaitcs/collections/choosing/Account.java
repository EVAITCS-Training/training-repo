package com.evaitcs.collections.choosing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A SecureBank account — the payload this module pours into every container on the
 * Big-O table, and the data the encapsulation lesson protects.
 *
 * equals/hashCode by id (core-java 08, harvested by module 01): the HashMap scenario
 * below only finds an account by id because the KEY lookup runs on equals, and the
 * defensive-copy tests only prove what they prove because a stranger with the same id
 * IS the same account. Duplicated per module on purpose — this module stands alone.
 *
 * WHY balance is BigDecimal: money is never float/double (core-java module 02).
 * Note the fields are final, yet the encapsulation lesson still bites: the registry
 * holds the LIST, and leaking the list leaks every account in it, however immutable
 * the accounts themselves are. Encapsulation is about who holds the CONTAINER.
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

    /** Equal objects MUST hash equal (core-java 08) — the HashMap scenario lives on this. */
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
