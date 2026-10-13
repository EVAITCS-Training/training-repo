package com.evaitcs.corejava.statics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The module's model class: ONE class carrying BOTH kinds of members, so every
 * demo can point at the same code.
 *
 *   static   (class-level) : BANK_NAME, accountCount - ONE slot in the class area,
 *                            shared by every BankAccount object that ever exists.
 *   instance (per-object)  : id, owner, balance      - ONE slot PER object on the heap.
 *
 * Board picture (README diagram 1): the class is a box with two slots; every
 * `new BankAccount(...)` adds ANOTHER box with three slots of its own, and all the
 * object boxes SHARE - not copy - the slots in the class box.
 */
public class BankAccount {

    // ---- CLASS-level members: one copy, no matter how many objects exist ----

    /** A CONSTANT: static final + UPPER_SNAKE_CASE. Read it as BankAccount.BANK_NAME. */
    public static final String BANK_NAME = "SecureBank";

    /**
     * The account counter - classic LEGITIMATE static state: the class itself tracks
     * how many instances it has handed out. Still private with a static getter,
     * because a static field deserves the same encapsulation as an instance one
     * (modules 05/06). Named in camelCase, NOT UPPER_SNAKE: it is mutable state,
     * not a constant, and the naming should not pretend otherwise.
     */
    private static int accountCount = 0;

    /** Static getter: called as BankAccount.getAccountCount(), with zero objects around. */
    public static int getAccountCount() {
        // No `this` in here! A static method belongs to the CLASS - there is no
        // instance in scope, which is exactly why it cannot read `balance` below.
        return accountCount;
    }

    // ---- INSTANCE-level members: one copy PER object ----

    private final String id;
    private final String owner;
    private BigDecimal balance; // money is ALWAYS BigDecimal, scale 2 (module 02: double loses cents)

    public BankAccount(String owner, BigDecimal openingBalance) {
        this.owner = owner;
        this.balance = openingBalance.setScale(2, RoundingMode.HALF_UP);
        accountCount++;                       // the ONE shared slot ticks once per object built
        this.id = "SB-" + (1000 + accountCount);
    }

    /**
     * STATIC FACTORY - the headline pattern of StaticMethodsAndFactoriesDemo.
     * It runs with no `this`, can be called before ANY object exists, and its NAME
     * says what it builds (a constructor can only ever be called "BankAccount").
     */
    public static BankAccount createSavings(String owner) {
        return new BankAccount(owner, BigDecimal.ZERO);
    }

    public void deposit(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("deposit must be positive, got " + amount);
        }
        this.balance = this.balance.add(amount).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getOwner() {
        return owner;
    }

    public String getId() {
        return id;
    }

    /**
     * An INSTANCE method reading a STATIC field - always legal: every object knows
     * its class. The reverse (a static method reading `balance`) is a COMPILE ERROR;
     * StaticMethodsAndFactoriesDemo carries the exact javac message.
     */
    public String describe() {
        return BANK_NAME + " " + id + " owner=" + owner + " balance=" + balance
                + " (accounts so far: " + getAccountCount() + ")";
    }
}
