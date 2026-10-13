package com.evaitcs.corejava.contracts;

import java.math.BigDecimal;

/**
 * The other side of the family: no interest, a monthly keeping fee, and an
 * overdraft. Note the EXTRA field — a subclass may add its OWN state on top of
 * the inherited state. (Contrast: an interface can add nothing per-object,
 * ever — its constants are one shared copy for everyone.)
 */
public class CurrentAccount extends Account implements Transferable {

    private final BigDecimal overdraftLimit;    // subclass-private state, scale 2

    public CurrentAccount(String id, BigDecimal openingBalance, BigDecimal overdraftLimit) {
        super(id, openingBalance);
        this.overdraftLimit = overdraftLimit.setScale(2);
    }

    @Override
    public BigDecimal interestRate() {
        return BigDecimal.ZERO.setScale(2);     // BLANK 1: current accounts earn nothing
    }

    @Override
    protected BigDecimal monthlyFee() {
        return new BigDecimal("2.00");          // BLANK 2: the keeping fee
    }

    @Override
    public boolean transferTo(Transferable target, BigDecimal amount) {
        BigDecimal floor = overdraftLimit.negate();          // e.g. -200.00
        if (getBalance().subtract(amount).compareTo(floor) < 0) {
            recordAudit("BLOCKED transfer of " + amount + " — below overdraft floor");
            return false;
        }
        withdraw(amount);
        target.deposit(amount);
        recordAudit("transferred " + amount + " to " + target);
        return true;
    }

    @Override
    public String toString() {
        return "CurrentAccount " + getId() + " balance " + getBalance()
                + " (overdraft " + overdraftLimit + ")";
    }
}
