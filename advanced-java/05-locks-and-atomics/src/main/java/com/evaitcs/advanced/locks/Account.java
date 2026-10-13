package com.evaitcs.advanced.locks;

import java.math.BigDecimal;

/**
 * A SecureBank account — the shared, contended resource this module's threads fight over.
 *
 * WHY the balance methods are NOT synchronized here:
 * Module 05's whole thesis is that java.util.concurrent.locks sells you the pieces of a
 * monitor a la carte. So the LOCKING lives in the demos (ReentrantLock, ReadWriteLock),
 * not baked into this value object. That separation is what lets DeadlockDemo show a
 * two-lock deadlock and then FIX it by ordering these accounts — which is why Account is
 * Comparable by id: the fix acquires locks in a single global order (README "Fix 1").
 *
 * Money is BigDecimal scale 2 (house rule); duplicated per module on purpose so this
 * module stands alone on screen.
 */
public class Account implements Comparable<Account> {

    private final String id;
    private final String owner;
    private BigDecimal balance;   // guarded EXTERNALLY by whatever lock the caller chooses

    public Account(String id, String owner, BigDecimal balance) {
        this.id = id;
        this.owner = owner;
        this.balance = balance.setScale(2);
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

    /** Caller must hold this account's lock — the demos make that explicit. */
    public void deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount).setScale(2);
    }

    /** Caller must hold this account's lock — the demos make that explicit. */
    public void withdraw(BigDecimal amount) {
        this.balance = this.balance.subtract(amount).setScale(2);
    }

    /** A single global order by id — the key to the lock-ordering deadlock fix. */
    @Override
    public int compareTo(Account other) {
        return id.compareTo(other.id);
    }

    @Override
    public String toString() {
        return id + " | " + owner + " | " + balance.toPlainString();
    }
}
