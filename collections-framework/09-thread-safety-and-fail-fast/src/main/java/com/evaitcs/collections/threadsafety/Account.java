package com.evaitcs.collections.threadsafety;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A SecureBank account — the VALUE this module's maps carry into battle.
 *
 * WHY it overrides equals/hashCode (by id, exactly as built in core-java module 08):
 * every containsKey()/get() in the wrapper and check-then-act demos runs on that
 * contract — a teller looking up "ACC-1027" with a freshly-built Account object must
 * find the stored one. And WHY balance is BigDecimal: money is never float/double
 * (core-java module 02) — the lost updates this module demos are LOST MONEY, and they
 * must print as exact cents, not binary noise.
 *
 * Duplicated per module on purpose (house style): every module stands alone on screen.
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

    /** Equal objects MUST hash equal (core-java 08) — containsKey lives on this. */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " | " + owner + " | " + balance.toPlainString();
    }

    /** The same three customers as module 01 — old friends, new (threaded) trouble. */
    public static List<Account> sampleAccounts() {
        List<Account> accounts = new ArrayList<Account>();
        accounts.add(new Account("ACC-1027", "Dana", new BigDecimal("2450.00")));
        accounts.add(new Account("ACC-1005", "Ana", new BigDecimal("86.40")));
        accounts.add(new Account("ACC-1011", "Bo", new BigDecimal("1200.00")));
        return accounts;
    }
}
