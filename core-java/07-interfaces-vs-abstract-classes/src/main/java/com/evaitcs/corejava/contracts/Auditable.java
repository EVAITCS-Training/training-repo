package com.evaitcs.corejava.contracts;

/**
 * A CAN-DO capability: "every action leaves a trail". Compliance demands it on
 * accounts AND on prepaid cards — two unrelated families, one contract. That
 * cross-cutting reach is the signature use of an interface.
 *
 * Both methods below are implicitly `public abstract`. Implementing classes MUST
 * provide every body — or be declared abstract themselves and pass the debt down.
 * Skip one in a concrete class and javac says, word for word:
 *   error: CardPayment is not abstract and does not override abstract method
 *          auditTrail() in Auditable
 */
public interface Auditable {

    /** public static final implicitly — and INHERITED: implementing classes may
     *  write AUDIT_PREFIX bare, with no qualification (constants are the one
     *  interface member that inherits into classes; Java 8 static METHODS do
     *  not — java8-features/02-interface-evolution). */
    String AUDIT_PREFIX = "[AUDIT]";

    void recordAudit(String event);

    String auditTrail();
}
