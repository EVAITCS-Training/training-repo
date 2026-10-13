package com.evaitcs.collections.sets;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A SecureBank account — duplicated per module on purpose, so this module stands alone
 * on screen (same class as module 01's Account; never import across modules).
 *
 * WHY it overrides equals/hashCode (by id): a HashSet asks EVERY element it stores
 * "does your hashCode land in this bucket, and if so, are you equal?" — the contract
 * from core-java 08 is the engine under add/contains/remove here. Without the override,
 * no two Accounts would ever be duplicates and the whole module would teach nothing.
 *
 * WHY it does NOT implement Comparable: a TreeSet demands an order, and Account
 * deliberately has none — TreeSetOrderingDemo catches the ClassCastException that
 * proves the demand (core-java 10). Transaction, the class this module really sorts,
 * carries the Comparable.
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

    /** Equal objects MUST hash equal (core-java 08) — the hash Sets bucket with this. */
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
