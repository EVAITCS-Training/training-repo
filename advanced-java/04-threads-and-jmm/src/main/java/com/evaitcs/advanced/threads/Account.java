package com.evaitcs.advanced.threads;

import java.math.BigDecimal;

/**
 * A SecureBank account — this module's MUTABLE, plain-field shared object.
 *
 * WHY the fields are deliberately NOT volatile and NOT final:
 * Module 04 is about the Java Memory Model, and a mutable account with ordinary fields
 * is the object that makes the two central lessons visible:
 *   1. Without an edge, one thread's writes to these fields may never reach another
 *      (README "The Java Memory Model: cached copies"). This is the DANGER.
 *   2. A happens-before edge — here, Thread.join() — publishes ALL of them at once,
 *      so the reader sees a fully-written account (README "join() is an edge"). This is
 *      the FIX. HappensBeforeDemo writes these fields on a worker and reads them on main
 *      after join(); the test asserts every field came across.
 *
 * The balance is plain on purpose so the demo can show the edge doing the work. In real
 * banking code you would guard it with a lock or make it an atomic — that is module 05.
 *
 * Money is BigDecimal scale 2 (house rule); duplicated per module on purpose.
 */
public class Account {

    private final String id;
    private final String owner;

    /** Plain (non-volatile) mutable balance — the field the JMM lessons turn on. */
    private BigDecimal balance;

    /** A second plain field, so "join publishes ALL writes" shows more than one write. */
    private long version;

    public Account(String id, String owner, BigDecimal balance) {
        this.id = id;
        this.owner = owner;
        this.balance = balance.setScale(2);
        this.version = 0;
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

    public long getVersion() {
        return version;
    }

    /** A plain write with NO synchronization — visible to other threads only across an edge. */
    public void deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount).setScale(2);
        this.version = this.version + 1;
    }

    @Override
    public String toString() {
        return id + " | " + owner + " | " + balance.toPlainString() + " | v" + version;
    }
}
