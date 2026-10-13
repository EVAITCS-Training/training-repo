package com.evaitcs.corejava.equality;

import java.math.BigDecimal;

/**
 * A bank transaction — the VALUE we store in the maps during this module
 * (the AccountKey is the KEY; the Transaction is what the key unlocks).
 *
 * amount is a BigDecimal, which carries the trap from module 02 into this one:
 *   new BigDecimal("2.0").equals(new BigDecimal("2.00"))    -> FALSE (scale is part of equals)
 *   new BigDecimal("2.0").compareTo(new BigDecimal("2.00")) -> 0     (numerically equal)
 *
 * So IF you ever give Transaction an equals(), you must DECIDE CONSCIOUSLY which one
 * you mean for the money field:
 *   - equals(amount)   : "2.0" and "2.00" are DIFFERENT transactions (exact value + scale)
 *   - compareTo == 0   : "2.0" and "2.00" are the SAME amount — but then hashCode must
 *                        ALSO ignore scale (e.g. stripTrailingZeros().hashCode()), or you
 *                        re-break the very contract this module is about.
 *
 * We deliberately do NOT override equals here: Transaction is used as a map VALUE, and
 * map values are looked up by reference, not by content. Only KEYS need the contract.
 * (CompareToConsistencyTeaserDemo uses the BigDecimal subtlety directly.)
 */
public class Transaction {

    private final String id;
    private final BigDecimal amount;

    public Transaction(String id, BigDecimal amount) {
        this.id = id;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return id + " (" + amount.toPlainString() + ")";
    }

    /** A couple of realistic transactions so the demos read like a bank statement. */
    public static Transaction payroll() {
        return new Transaction("T-1001", new BigDecimal("2450.00"));
    }

    public static Transaction rent() {
        return new Transaction("T-1002", new BigDecimal("1200.00"));
    }
}
