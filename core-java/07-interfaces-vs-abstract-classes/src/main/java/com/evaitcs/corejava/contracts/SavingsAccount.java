package com.evaitcs.corejava.contracts;

import java.math.BigDecimal;

/**
 * Fills the family's blanks the "savings" way: earns interest, pays no fee.
 *
 * extends ONE class (Account) — and implements ONE interface on top of it.
 * The extends slot was already spent on Account; without interfaces there is
 * no second slot. That single-class limit is WHY Transferable is an interface:
 * it can be added to ANY family, including CardPayment's.
 */
public class SavingsAccount extends Account implements Transferable {

    public SavingsAccount(String id, BigDecimal openingBalance) {
        super(id, openingBalance);   // the abstract class's constructor, reached via super —
        // Teaching println: runs AFTER the one in Account, proving super() executes first.
        System.out.println("   [SavingsAccount ctor] super(...) already ran; adding nothing of its own");
        // Forget that super call (or skip a blank) and javac says, word for word:
        //   error: SavingsAccount is not abstract and does not override abstract method interestRate() in Account
    }

    @Override
    public BigDecimal interestRate() {
        return new BigDecimal("0.03");            // BLANK 1 filled: 3% yearly
    }

    @Override
    protected BigDecimal monthlyFee() {
        return BigDecimal.ZERO.setScale(2);       // BLANK 2 filled: savings are fee-free
    }

    /**
     * The ONLY Transferable method written here. `deposit` was NOT written —
     * the concrete `deposit` inherited from Account already matches the
     * interface signature, so it satisfies the contract automatically.
     * Inherited plumbing counts. (Trap: if Account's deposit were protected,
     * it would NOT satisfy the public interface method.)
     */
    @Override
    public boolean transferTo(Transferable target, BigDecimal amount) {
        if (amount.compareTo(getBalance()) > 0) {
            recordAudit("BLOCKED transfer of " + amount + " — insufficient funds");
            return false;
        }
        // A real bank would also enforce Transferable.MAX_TRANSFERS_PER_DAY here —
        // the interface constant is inherited bare, no qualification needed. Counting
        // today's transfers would need more STATE... which is exactly why the family
        // logic lives in an abstract class and not in the interface.
        withdraw(amount);            // protected plumbing from Account — reachable from the family
        target.deposit(amount);      // programming to the INTERFACE: we never ask what `target` really is
        recordAudit("transferred " + amount + " to " + target);
        return true;
    }

    @Override
    public String toString() {
        return "SavingsAccount " + getId() + " balance " + getBalance();
    }
}
