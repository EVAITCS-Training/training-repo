package com.evaitcs.corejava.contracts;

import java.math.BigDecimal;

/**
 * The smallest possible interface: ONE abstract method. The point of the demo
 * is that it spans families — the abstract Account hierarchy implements it AND
 * the totally unrelated CardPayment implements it. Put both in a
 * List&lt;PaymentMethod&gt; and the loop cannot tell (or care) which is which.
 * That is "programming to an interface".
 *
 * A SINGLE-abstract-method interface like this is exactly what Java 8 later
 * calls a FUNCTIONAL INTERFACE — the target type for a lambda. Here, pre-8,
 * the only way to implement one on the spot is an anonymous inner class
 * (AnonymousCallbackDemo does precisely that, twice).
 */
public interface PaymentMethod {

    void pay(BigDecimal amount);
}
