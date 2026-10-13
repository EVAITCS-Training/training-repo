package com.evaitcs.collections.sorting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A SecureBank account — the element this module hands out through WRAPPERS.
 *
 * The view demos (UnmodifiableIsAViewDemo) need something worth protecting: an account
 * list a caller must be able to READ but never MUTATE. equals/hashCode by id, exactly as
 * built in core-java module 08 and used in module 01 of this course. Duplicated per
 * module on purpose — this module stands alone on screen.
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

    /** Equal objects MUST hash equal (core-java 08). */
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
