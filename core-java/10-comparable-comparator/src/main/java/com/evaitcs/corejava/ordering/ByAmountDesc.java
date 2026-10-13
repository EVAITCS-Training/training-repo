package com.evaitcs.corejava.ordering;

import java.util.Comparator;

/**
 * CUSTOM ORDER #1 — biggest amount first, defined OUTSIDE Transaction.
 *
 * This is the classic pre-Java-8 idiom: a named top-level class implementing
 * Comparator. WHY a separate class: Transaction already spent its ONE natural-order
 * slot (by id). "Biggest first" is a view, and views live outside the data — that
 * is the whole Comparable-vs-Comparator split.
 *
 * java8-features/01-lambdas: Comparator.comparing(Transaction::getAmount).reversed() replaces this
 */
public class ByAmountDesc implements Comparator<Transaction> {

    @Override
    public int compare(Transaction t1, Transaction t2) {
        // Descending by SWAPPING THE ARGUMENTS, not by negating the result:
        // `-c` looks clever but negating Integer.MIN_VALUE stays Integer.MIN_VALUE
        // (overflow!), so negation can break the sign contract. Swapping never can.
        //
        // BigDecimal.compareTo does the numeric work (module 02: for money, compareTo
        // is the numeric truth — equals also compares scale, 2.0 != 2.00).
        return t2.getAmount().compareTo(t1.getAmount());
    }
}
