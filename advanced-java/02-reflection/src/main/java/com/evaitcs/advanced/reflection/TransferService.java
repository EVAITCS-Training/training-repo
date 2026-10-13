package com.evaitcs.advanced.reflection;

import java.math.BigDecimal;

/**
 * The interface DynamicProxyDemo wraps in an audit-logging proxy.
 *
 * WHY an interface and not a class: java.lang.reflect.Proxy can only proxy INTERFACES.
 * It fabricates a brand-new class at runtime that implements the interfaces you list and
 * forwards every call to your InvocationHandler. Proxying a concrete class needs bytecode
 * generation (CGLIB / ByteBuddy) — which is exactly what Spring falls back to when a bean
 * has no interface. Module 03 and stage-2 build on this distinction.
 */
public interface TransferService {

    /** Moves money and returns a human-readable receipt. */
    String transfer(String fromId, String toId, BigDecimal amount);

    /** Returns the balance of one account (scale 2). */
    BigDecimal balanceOf(String accountId);
}
