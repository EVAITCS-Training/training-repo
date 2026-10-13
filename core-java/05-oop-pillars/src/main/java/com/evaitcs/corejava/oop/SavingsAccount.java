package com.evaitcs.corejava.oop;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A savings account: money that sits still and earns interest.
 *
 * Inheritance depth 2 and it already pays off: id, owner, balance, deposit(),
 * withdraw(), getBalance() all come from Account — this file adds the ONE thing
 * a savings account knows that the base class doesn't: the interest rate.
 */
public class SavingsAccount extends Account {

    private final BigDecimal annualInterestRate;   // e.g. 0.04 means 4% per year

    /**
     * Convenience constructor — delegates with this(...) to the full one, so validation
     * and assignment are written ONCE. Chain rule: this(...) must be the first line,
     * exactly like super(...). (Both are "let someone else build part of this object first".)
     */
    public SavingsAccount(String id, String owner) {
        this(id, owner, BigDecimal.ZERO, new BigDecimal("0.04"));
    }

    public SavingsAccount(String id, String owner, BigDecimal openingBalance, BigDecimal annualInterestRate) {
        super(id, owner, openingBalance);   // MUST be the first line: the parent builds
                                            // its half of the object before the child's body runs.
        if (annualInterestRate == null || annualInterestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("interest rate must be >= 0, was: " + annualInterestRate);
        }
        this.annualInterestRate = annualInterestRate;
    }

    /**
     * @Override is NOT decoration. Without it, a typo'd signature (wrong param type,
     * wrong return type) silently becomes an OVERLOAD that nobody calls — it compiles,
     * it just never runs. With it, the compiler turns that runtime surprise into a
     * build failure. InheritanceDemo stages that exact crime.
     */
    @Override
    public String describe() {
        return "Savings " + getId() + " for " + getOwner()
                + ", balance " + getBalance()
                + ", earns " + annualInterestRate.movePointRight(2) + "% interest";
    }

    /** The HOW that Account only declared as WHAT. Money math: scale 2, banker-visible rounding. */
    @Override
    public BigDecimal interestFor(BigDecimal amount) {
        return amount.multiply(annualInterestRate).setScale(2, RoundingMode.HALF_UP);
    }

    /** Pays this account's own interest — built purely from inherited/own public API. */
    public void applyInterest() {
        deposit(interestFor(getBalance()));
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }
}
