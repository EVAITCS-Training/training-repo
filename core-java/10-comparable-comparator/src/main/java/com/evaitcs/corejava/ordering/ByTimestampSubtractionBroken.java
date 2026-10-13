package com.evaitcs.corejava.ordering;

import java.util.Comparator;

/**
 * THE TRAP, preserved as evidence — DO NOT COPY THIS PATTERN.
 *
 * "Just subtract, the sign is all compareTo wants" — true, until the subtraction
 * itself overflows. Two epoch-millis timestamps ~24.9 days apart differ by more than
 * Integer.MAX_VALUE (2_147_483_647); the long difference is computed correctly and
 * then the (int) cast THROWS AWAY the high bits, sign included. The comparator then
 * returns the wrong sign, and the sort silently produces a wrong order — no exception,
 * just bad data. (SubtractionTrapDemo prints the exact numbers.)
 *
 * A comparator that lies about the sign also breaks the compare CONTRACT
 * (a "before" b must mean b "after" a consistently), and TimSort can even throw
 * "Comparison method violates its general contract!" on bigger lists.
 *
 * The fixes: Long.compare(a, b), or a < b ? -1 : (a == b ? 0 : 1), or compareTo on
 * a type that already knows how to order itself (String, BigDecimal, ...).
 */
public class ByTimestampSubtractionBroken implements Comparator<Transaction> {

    @Override
    public int compare(Transaction t1, Transaction t2) {
        return (int) (t1.getTimestampMillis() - t2.getTimestampMillis()); // WRONG: overflows
    }
}
