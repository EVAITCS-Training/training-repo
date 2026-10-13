package com.evaitcs.corejava.oop;

import java.math.BigDecimal;

/**
 * The base class of our tiny bank model — and the running example for ALL FOUR pillars:
 *
 *   ABSTRACTION     — declares WHAT every account must do (describe, interestFor) and
 *                     refuses to say HOW: abstract methods have no body.
 *   ENCAPSULATION   — balance is PRIVATE; the only way in is deposit()/withdraw(),
 *                     which enforce the invariant "balance never drops below the floor".
 *   INHERITANCE     — SavingsAccount and CurrentAccount extend this class: they inherit
 *                     its fields and methods (NOT its constructors) and override the abstracts.
 *   POLYMORPHISM    — callers hold Account references; which describe()/withdraw() RUNS
 *                     is decided at runtime by the actual object behind the reference.
 *
 * Inheritance depth here is deliberately 2 (Account -> Savings/Current). Deep hierarchies
 * are where behavior goes to hide and where debugging goes to die.
 */
public abstract class Account {

    // --- State: PRIVATE. Not hidden for tidiness — hidden so every write goes through
    // --- a method that can say NO. That "can say NO" is the whole point of encapsulation.
    private final String id;
    private final String owner;
    private BigDecimal balance;   // reassigned by deposit/withdraw, but ALWAYS a BigDecimal:
                                  // money is never a double (module 02 explains exactly why)

    /**
     * Abstract classes CAN have constructors — this one runs via super(...) from every
     * subclass. What they CANNOT do is be used with `new` (see AbstractionDemo).
     * Constructors are never inherited: each subclass writes its own and chains here.
     */
    protected Account(String id, String owner, BigDecimal openingBalance) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("account id required");
        }
        if (owner == null || owner.trim().isEmpty()) {
            throw new IllegalArgumentException("account owner required");
        }
        if (openingBalance == null || openingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("opening balance must be >= 0, was: " + openingBalance);
        }
        this.id = id;
        this.owner = owner;
        this.balance = openingBalance;
    }

    /** The front door for money IN. Any positive amount is welcome; nothing else is. */
    public void deposit(BigDecimal amount) {
        requirePositive(amount, "deposit");
        balance = balance.add(amount);
    }

    /**
     * An OVERLOAD: same class, same name, different signature. Which one runs is decided
     * at COMPILE time by the static type of the argument — OverloadVsOverrideDemo proves it.
     * Convenience for callers holding the amount as text (a teller form, a parsed file).
     */
    public void deposit(String amountText) {
        deposit(new BigDecimal(amountText));
    }

    /** The front door for money OUT. The base invariant: never below zero. */
    public void withdraw(BigDecimal amount) {
        withdrawWithFloor(amount, BigDecimal.ZERO);
    }

    /**
     * Family-only (protected) helper so a subclass with a DIFFERENT floor — CurrentAccount
     * may dip to minus its overdraft limit — reuses the same check-then-act mechanism
     * instead of copy-pasting it. One copy of the rule = one place to fix it.
     * (The full access-modifier ladder — public/protected/private — is module 06.)
     */
    protected final void withdrawWithFloor(BigDecimal amount, BigDecimal floor) {
        requirePositive(amount, "withdraw");
        BigDecimal after = balance.subtract(amount);
        if (after.compareTo(floor) < 0) {
            // Throw, don't print-and-continue: a REFUSED transaction must leave no trace.
            // Check-then-act lives in ONE method, so the caller's story is atomic:
            // either the balance moved, or it didn't — never "half did".
            throw new IllegalArgumentException("Refused: withdrawing " + amount + " from " + id
                    + " would take the balance to " + after + ", below the floor " + floor
                    + " (current balance " + balance + ")");
        }
        balance = after;
    }

    // --- WHAT every account must do, declared but deliberately NOT implemented. ---
    // The compiler forces every concrete subclass to answer both. That contract is
    // what lets PolymorphismDemo loop over List<Account> and trust these calls.

    /** A human-readable summary: type, id, owner, balance — and whatever the subclass adds. */
    public abstract String describe();

    /** How much interest this account would pay on the given amount. Subclasses say HOW. */
    public abstract BigDecimal interestFor(BigDecimal amount);

    // --- Getters: read-only windows onto private state. Notice what does NOT exist:
    // --- setBalance(). A setter that accepts anything is a hole in the invariant
    // --- with a polite name on it.

    public String getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Shared validation — small, boring, and exactly where bugs DON'T live, because
     * there is only one copy of it. BigDecimal is compared with compareTo, never
     * equals: equals() also compares SCALE, so 1.0 "does not equal" 1.00 (module 02).
     */
    protected static void requirePositive(BigDecimal amount, String what) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(what + " amount must be > 0, was: " + amount);
        }
    }

    @Override
    public String toString() {
        // getClass() is itself polymorphic: it returns the RUNTIME class, so this one
        // line prints "SavingsAccount[...]" or "CurrentAccount[...]" with no override needed.
        return getClass().getSimpleName() + "[" + id + " " + owner + " balance=" + balance + "]";
    }
}
