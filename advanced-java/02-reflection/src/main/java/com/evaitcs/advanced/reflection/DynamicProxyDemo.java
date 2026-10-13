package com.evaitcs.advanced.reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo 4 — DYNAMIC PROXIES: a class fabricated at runtime. (README section "Dynamic
 * proxies" + its walkthrough.)
 *
 * A dynamic proxy is an object that implements YOUR interface but runs YOUR handler for
 * every call. java.lang.reflect.Proxy.newProxyInstance invents a brand-new class at
 * runtime — its name looks like jdk.proxy1.$Proxy0 — that implements the interfaces you
 * list and forwards each method to an InvocationHandler. The handler decides what to do:
 * log, time, check security, start a transaction... then (usually) delegate to the real object.
 *
 * This is EXACTLY how Spring @Transactional wraps your bean: the object in your context is
 * not your class, it is a proxy that opens a transaction, calls your method, then commits.
 */
public class DynamicProxyDemo {

    public static void main(String[] args) {
        banner("1. The proxy is a fabricated class that implements your interface");

        List<String> auditLog = new ArrayList<>();
        TransferService real = new SimpleLedger();
        TransferService proxy = auditProxy(real, auditLog);

        System.out.println("step 1: proxy.getClass().getName()   = " + proxy.getClass().getName());
        System.out.println("step 2: proxy instanceof TransferService = " + (proxy instanceof TransferService));
        System.out.println("step 3: proxy instanceof SimpleLedger    = " + (proxy instanceof SimpleLedger));
        System.out.println("step 4: interfaces the proxy class implements = "
                + Arrays.toString(proxy.getClass().getInterfaces()));
        System.out.println("verdict: a JDK proxy IS-A TransferService (the interface) but is NOT the real class.");

        banner("2. Every call is logged, THEN delegated");

        String receipt1 = proxy.transfer("A", "B", new BigDecimal("30.00"));
        BigDecimal balanceA = proxy.balanceOf("A");
        String receipt2 = proxy.transfer("B", "A", new BigDecimal("10.00"));

        System.out.println("step 1: transfer A->B 30.00 returned : " + receipt1);
        System.out.println("step 2: balanceOf(A)   returned      : " + balanceA.toPlainString());
        System.out.println("step 3: transfer B->A 10.00 returned : " + receipt2);
        System.out.println("step 4: audit log, in call order      : " + auditLog);
        System.out.println("verdict: the handler logged each call and returned the REAL service's value.");

        banner("3. This is how Spring @Transactional works");

        System.out.println("Swap 'log the call' for 'begin a transaction' and 'commit / roll back',");
        System.out.println("and you have @Transactional. Your @Service bean is wrapped in a proxy that");
        System.out.println("runs around your method. That is why calling one @Transactional method from");
        System.out.println("ANOTHER in the same bean skips the proxy — 'this' is the raw object, not the wrapper.");

        banner("4. In one sentence");
        System.out.println("A dynamic proxy is a runtime-fabricated implementation of your interface whose");
        System.out.println("handler runs before, after, or instead of the real method — the seam frameworks inject behavior through.");
    }

    /**
     * Builds an audit-logging proxy around {@code target}. Every interface call appends its
     * method name to {@code auditLog}, then delegates and returns the real result.
     *
     * Object's own methods (toString/hashCode/equals) ALSO arrive at the handler; we pass
     * them straight through without auditing them, so the log records only real service calls.
     */
    static TransferService auditProxy(TransferService target, List<String> auditLog) {
        InvocationHandler handler = (proxy, method, methodArgs) -> {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(target, methodArgs);   // don't audit toString/hashCode/equals
            }
            auditLog.add(method.getName());                 // step 1: LOG the call
            Object result = method.invoke(target, methodArgs); // step 2: DELEGATE to the real object
            return result;                                  // step 3: return its value
        };
        return (TransferService) Proxy.newProxyInstance(
                TransferService.class.getClassLoader(),
                new Class<?>[] { TransferService.class },
                handler);
    }

    /** The real service the proxy delegates to. Fixed starting balances keep it deterministic. */
    static final class SimpleLedger implements TransferService {

        private final Map<String, BigDecimal> balances = new HashMap<>();

        SimpleLedger() {
            balances.put("A", new BigDecimal("100.00"));
            balances.put("B", new BigDecimal("50.00"));
        }

        @Override
        public String transfer(String fromId, String toId, BigDecimal amount) {
            balances.merge(fromId, amount.negate(), BigDecimal::add);
            balances.merge(toId, amount, BigDecimal::add);
            return "OK " + fromId + "->" + toId + " "
                    + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }

        @Override
        public BigDecimal balanceOf(String accountId) {
            return balances.getOrDefault(accountId, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
