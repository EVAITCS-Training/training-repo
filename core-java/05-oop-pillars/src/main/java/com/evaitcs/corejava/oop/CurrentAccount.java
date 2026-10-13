package com.evaitcs.corejava.oop;

import java.math.BigDecimal;

/**
 * A current account (US banks call the same thing a CHECKING account): money in
 * constant motion, no interest, but the bank extends a small line of credit —
 * the overdraft limit — so the balance may go negative down to -limit.
 *
 * Same shape as SavingsAccount, different RULES. That "same shape, different rules"
 * is what polymorphism cashes in on: a caller writing account.withdraw(x) never
 * needs to know which kind of account is behind the reference.
 */
public class CurrentAccount extends Account {

    private final BigDecimal overdraftLimit;   // how far below zero this account may go

    public CurrentAccount(String id, String owner, BigDecimal openingBalance, BigDecimal overdraftLimit) {
        super(id, owner, openingBalance);   // first line, always — parent half first
        if (overdraftLimit == null || overdraftLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("overdraft limit must be >= 0, was: " + overdraftLimit);
        }
        this.overdraftLimit = overdraftLimit;
    }

    /**
     * OVERRIDE to change the invariant's FLOOR, not to escape the invariant.
     * Account.withdraw says "never below 0"; we say "never below -overdraftLimit".
     * The check-then-act machinery (withdrawWithFloor) is reused, not copy-pasted:
     * one copy of the mechanism, two policies.
     */
    @Override
    public void withdraw(BigDecimal amount) {
        withdrawWithFloor(amount, overdraftLimit.negate());
    }

    @Override
    public String describe() {
        return "Current " + getId() + " for " + getOwner()
                + ", balance " + getBalance()
                + ", overdraft up to " + overdraftLimit
                + ", earns no interest";
    }

    /** Current accounts pay no interest — returning ZERO is a perfectly good HOW. */
    @Override
    public BigDecimal interestFor(BigDecimal amount) {
        return BigDecimal.ZERO;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
}
