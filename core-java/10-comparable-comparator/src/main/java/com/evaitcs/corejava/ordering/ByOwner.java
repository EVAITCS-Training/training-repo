package com.evaitcs.corejava.ordering;

import java.util.Comparator;

/**
 * Orders accounts by owner name — and CRASHES the moment an owner is null.
 *
 * This is not sloppiness, it is the lesson: a Comparator's compare() is called by
 * the sort machinery on data YOU do not control, and `null.compareTo(...)` /
 * `x.compareTo(null)` throws NullPointerException deep inside Collections.sort —
 * the stack trace points at java.util, not at the null field that caused it.
 * ReverseAndNullsDemo catches the NPE so the class can see exactly where it comes
 * from; ByOwnerNullSafe is the pre-Java-8 fix (an explicit null policy inside
 * compare). Java 8 packages the same policy as Comparator.nullsFirst/nullsLast.
 */
public class ByOwner implements Comparator<Account> {

    @Override
    public int compare(Account a1, Account a2) {
        return a1.getOwner().compareTo(a2.getOwner()); // NPE if either owner is null — on purpose
    }
}
