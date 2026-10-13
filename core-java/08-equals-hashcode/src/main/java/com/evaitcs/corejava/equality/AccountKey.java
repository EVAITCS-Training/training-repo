package com.evaitcs.corejava.equality;

import java.util.Objects;

/**
 * The FIXED value class: equals AND hashCode overridden TOGETHER, from the SAME fields.
 *
 * SecureBank identifies an account by branchCode + accountNumber. That is a VALUE
 * concept — two key objects holding "BR-01" / "1001" ARE the same account, no matter
 * how many times they were `new`ed (typed by a teller, read from a file, sent over
 * the wire). Identity (==) asks the wrong question; content is the right one.
 *
 * Design decisions worth pointing at on the projector:
 *
 * 1. `final` class + `final` fields => IMMUTABLE. Immutable keys can never "move
 *    buckets" while sitting inside a HashMap (see MutableKeyTrapDemo). This is why
 *    String is the most-used HashMap key in existence (module 03).
 *
 * 2. equals uses getClass(), NOT instanceof. The one-block version of the debate:
 *      - instanceof: "a subclass instance MAY equal a superclass instance" — allows
 *        cross-type equality, but symmetry (p.equals(e) vs e.equals(p)) then depends
 *        on BOTH classes cooperating. One careless subclass breaks it (SymmetryTrapDemo).
 *      - getClass(): only objects of the EXACT same class can be equal — symmetry is
 *        guaranteed by construction, at the price of never equaling a subclass instance.
 *    For value classes like this one, getClass() is the safe default. For classes
 *    designed for extension with a "canonical form" (e.g. numeric towers), instanceof
 *    can be right — but then you MUST document and police symmetry.
 *
 * 3. hashCode uses Objects.hash(same fields). THE RULE: every field that participates
 *    in equals MUST participate in hashCode, and no others. Equal objects must produce
 *    equal hash codes — HashMap finds the bucket by hash FIRST and only then runs
 *    equals; if equal keys hash differently, equals never even gets a chance.
 */
public final class AccountKey {

    private final String branchCode;
    private final String accountNumber;

    public AccountKey(String branchCode, String accountNumber) {
        this.branchCode = branchCode;
        this.accountNumber = accountNumber;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    /**
     * Read this method TOP TO BOTTOM in class — every line is one rule:
     */
    @Override
    public boolean equals(Object other) {
        // (1) Identity shortcut: the same object is trivially equal to itself.
        //     Also makes the REFLEXIVE contract property free.
        if (this == other) {
            return true;
        }
        // (2) Contract: x.equals(null) must be false — never throw. (Calling a method
        //     on null would NPE; this guard is why equals must handle it itself.)
        if (other == null) {
            return false;
        }
        // (3) Type check: comparing an account key to a String is a category error.
        //     getClass() also gives us SYMMETRY for free (see class comment).
        if (getClass() != other.getClass()) {
            return false;
        }
        // (4) Now — and ONLY now — the cast is provably safe.
        AccountKey that = (AccountKey) other;
        // (5) Field by field, NULL-SAFE: Objects.equals(a, b) is
        //     `a == b || (a != null && a.equals(b))` — a plain
        //     `branchCode.equals(...)` would NPE when branchCode is null.
        //     (Fields could be primitives here, but SecureBank data is dirty.)
        return Objects.equals(branchCode, that.branchCode)
                && Objects.equals(accountNumber, that.accountNumber);
    }

    @Override
    public int hashCode() {
        // MUST use exactly the fields equals uses. Objects.hash is the two-field
        // shorthand for the classic 31*result+field loop (HashCodeFixDemo prints both).
        return Objects.hash(branchCode, accountNumber);
    }

    @Override
    public String toString() {
        // Not required by the contract, but a key that prints as
        // "com.evaitcs...AccountKey@1b6d3586" in a log is undebuggable (module 09).
        return branchCode + "/" + accountNumber;
    }
}
