package com.evaitcs.corejava.oop;

import java.math.BigDecimal;

/**
 * A one-method interface — abstraction in its purest form: a SHAPE with no state
 * and no implementation. Callers depend on the shape; any implementation can be
 * plugged in later without touching the calling code (BankDayStoryDemo does exactly that).
 *
 * In this module we implement it with an ANONYMOUS INNER CLASS — the classic idiom this
 * course teaches on purpose. Forward pointer: java8-features/01-lambdas turns that
 * 4-line ceremony into a single arrow — amount -> amount.multiply(RATE) — same interface,
 * same idea, less noise. Interface-vs-abstract-class strategy is module 07.
 */
public interface FeePolicy {

    /** The fee this policy charges for a transaction of the given amount. */
    BigDecimal feeFor(BigDecimal amount);
}
