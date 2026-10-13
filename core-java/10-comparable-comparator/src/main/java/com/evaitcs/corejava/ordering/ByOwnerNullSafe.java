package com.evaitcs.corejava.ordering;

import java.util.Comparator;

/**
 * ByOwner, grown-up: an explicit NULL POLICY decided by us, not by the JVM crash.
 *
 * The pre-Java-8 fix for nullable fields is four boring lines inside compare():
 * decide WHERE nulls live (here: first — "incomplete records surface at the top of
 * the report, where a human will see them"), and make the decision SYMMETRIC, or
 * you break the sign contract from CompareToContractDemo.
 *
 * Java 8 wraps this exact policy: Comparator.nullsFirst(Comparator.naturalOrder()).
 * Until then, this is the idiom to recognize in any pre-8 codebase.
 */
public class ByOwnerNullSafe implements Comparator<Account> {

    @Override
    public int compare(Account a1, Account a2) {
        String o1 = a1.getOwner();
        String o2 = a2.getOwner();
        if (o1 == null && o2 == null) {
            return 0;                    // both missing: a tie, not a crash
        }
        if (o1 == null) {
            return -1;                   // POLICY: nulls first ...
        }
        if (o2 == null) {
            return 1;                    // ... and symmetric: -compare(a2, a1)
        }
        return o1.compareTo(o2);         // both present: normal String order
    }
}
