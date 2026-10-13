package com.evaitcs.collections.maps;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A SecureBank account — the VALUE class of this module's registries (the KEY is the
 * account id, a String). Duplicated per module on purpose, so this module stands alone
 * on screen; never import across modules.
 *
 * WHY it overrides equals/hashCode (by id): a Map is two contracts in one. The KEY side
 * runs on hashCode + equals (core-java 08) — that is what HashMapInternalsDemo walks
 * through, bucket by bucket. The VALUE side runs on equals ALONE — containsValue() has no
 * hash to consult and must scan every entry comparing with equals(), which is why it is
 * O(n). MapBasicsDemo shows both sides.
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

    /** Equal objects MUST hash equal (core-java 08) — every Map key lookup lives on this. */
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
