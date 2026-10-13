package com.evaitcs.corejava.contracts;

import java.math.BigDecimal;

/**
 * A CALLBACK contract: code that accepts a FeeCalculator agrees to CALL it —
 * when, how often, and with what amount is the CALLEE's business. This is how
 * pre-Java-8 Java passed *behavior* as an argument: wrap the behavior in an
 * interface, hand over an object, get called back later.
 *
 * The whole java8-features/01-lambdas module exists because this pattern was
 * so common and so ceremonious to implement (see AnonymousCallbackDemo).
 */
public interface FeeCalculator {

    BigDecimal feeFor(BigDecimal amount);
}
