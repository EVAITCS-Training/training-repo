package com.evaitcs.advanced.exceptions;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A SecureBank account — this module's thrower, catcher and context all revolve around it.
 *
 * WHY A RECORD (README "Custom exceptions the RIGHT way"):
 * A record is immutable and its components (id, balance) are typed accessors, not a
 * formatted string. That is exactly the property we want the banking exceptions to have
 * too, so the domain object models the lesson: structured fields beat message parsing.
 *
 * Money is BigDecimal at scale 2 (repo-wide convention). The compact constructor pins the
 * scale so every Account in this module carries two decimal places, no surprises.
 *
 * A record cannot be mutated, so "moving money" RETURNS A NEW Account (plus/minus). This
 * keeps TransferService honest: it must rebuild both sides, which is why a failed transfer
 * naturally throws BEFORE any new Account is published.
 */
public record Account(String id, BigDecimal balance) {

    /** Compact constructor: normalize money to scale 2 on the way in (repo convention). */
    public Account {
        balance = balance.setScale(2, RoundingMode.HALF_UP);
    }

    /** A copy of this account with amount ADDED (scale 2). Immutable: returns a new record. */
    public Account plus(BigDecimal amount) {
        return new Account(id, balance.add(amount));
    }

    /** A copy of this account with amount SUBTRACTED (scale 2). Immutable: returns a new record. */
    public Account minus(BigDecimal amount) {
        return new Account(id, balance.subtract(amount));
    }

    /** True when this account holds at least amount — the guard TransferService enforces. */
    public boolean canCover(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }
}
