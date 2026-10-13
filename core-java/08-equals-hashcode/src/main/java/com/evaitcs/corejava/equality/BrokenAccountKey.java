package com.evaitcs.corejava.equality;

import java.util.Objects;

/**
 * The BROKEN version — equals overridden, hashCode LEFT ALONE. Kept side by side with
 * the fixed {@link AccountKey} so the damage is printable, not theoretical.
 *
 * WHY THIS IS A BUG (the whole module in one sentence): HashMap picks the BUCKET with
 * hashCode and only THEN walks that bucket calling equals. This class inherits
 * Object.hashCode(), which is derived from the object's IDENTITY — so two keys with
 * identical fields usually hash to DIFFERENT buckets, land in different places, and
 * equals is never even called. get(new BrokenAccountKey(same fields)) returns null
 * even though an "equal" key is sitting right there in the map.
 *
 * Note the class is NOT final and hashCode is deliberately absent — this is exactly
 * what a half-finished IDE "generate equals()" looks like in the wild. Students will
 * write this by accident; the fix is to treat the pair as one unit.
 *
 * THE RULE, to say out loud: override equals, and you MUST override hashCode too —
 * from the SAME fields. There is no version of "equals only" that is correct.
 */
public class BrokenAccountKey {

    private final String branchCode;
    private final String accountNumber;

    public BrokenAccountKey(String branchCode, String accountNumber) {
        this.branchCode = branchCode;
        this.accountNumber = accountNumber;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    // equals is CORRECT here. That is exactly the trap: it looks finished, it passes
    // a naive a.equals(b) test, and it still breaks every HashMap/HashSet it touches.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        BrokenAccountKey that = (BrokenAccountKey) other;
        return Objects.equals(branchCode, that.branchCode)
                && Objects.equals(accountNumber, that.accountNumber);
    }

    // hashCode() intentionally NOT overridden -> Object.hashCode() (identity-based).
    // Equal objects WILL get unequal hash codes -> the contract is violated.

    @Override
    public String toString() {
        return branchCode + "/" + accountNumber;
    }
}
