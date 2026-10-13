package com.evaitcs.corejava.ordering;

import java.util.Comparator;

/**
 * CUSTOM ORDER #2 — oldest transaction first, defined OUTSIDE Transaction.
 *
 * Note what this class does NOT do: it does not subtract the two timestamps.
 * `(int)(t1.getTimestampMillis() - t2.getTimestampMillis())` compiles, passes small
 * tests, and then flips its sign on real epoch-millis values — ByTimestampSubtractionBroken
 * is that mistake, preserved as evidence. Long.compare (Java 7+) returns the sign of
 * the difference WITHOUT computing a difference that has to fit in an int.
 *
 * java8-features/01-lambdas: Comparator.comparing(Transaction::getTimestampMillis) replaces this
 */
public class ByTimestamp implements Comparator<Transaction> {

    @Override
    public int compare(Transaction t1, Transaction t2) {
        return Long.compare(t1.getTimestampMillis(), t2.getTimestampMillis());
    }
}
