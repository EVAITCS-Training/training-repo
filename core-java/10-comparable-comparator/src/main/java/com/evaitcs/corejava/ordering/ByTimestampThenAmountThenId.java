package com.evaitcs.corejava.ordering;

import java.util.Comparator;

/**
 * CUSTOM ORDER #3 — oldest first, ties broken by amount, ties broken by id.
 *
 * WHY a ladder: one key is rarely enough. Two transactions can share a timestamp
 * (same millisecond, batch posting); if the comparator returns 0 for them, their
 * relative order is whatever the sort algorithm's stability happens to preserve —
 * i.e. insertion order, which means nothing to a customer reading a statement.
 * Tie-break until you reach a key that CANNOT tie (id is unique), and the order
 * becomes total and reproducible.
 *
 * This hand-written if-ladder is the pre-Java-8 idiom. Java 8 collapses it into
 * Comparator.comparing(...).thenComparing(...).thenComparing(...) — one expression,
 * shown once at the very end of this module (Java8PreviewDemo) and taught properly
 * in ../java8-features.
 */
public class ByTimestampThenAmountThenId implements Comparator<Transaction> {

    @Override
    public int compare(Transaction t1, Transaction t2) {
        // PRIMARY KEY: when did it happen?
        int byTime = Long.compare(t1.getTimestampMillis(), t2.getTimestampMillis());
        if (byTime != 0) {
            return byTime;               // decided — never look at the other keys
        }
        // TIE-BREAK 1: same millisecond -> smaller amount first.
        int byAmount = t1.getAmount().compareTo(t2.getAmount());
        if (byAmount != 0) {
            return byAmount;
        }
        // TIE-BREAK 2 (last resort): ids are unique, so this can never return 0
        // for two DIFFERENT transactions — the order is now total.
        return t1.getId().compareTo(t2.getId());
    }
}
