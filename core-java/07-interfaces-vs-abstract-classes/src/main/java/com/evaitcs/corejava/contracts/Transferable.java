package com.evaitcs.corejava.contracts;

import java.math.BigDecimal;

/**
 * A CAN-DO capability: "money can move through me".
 *
 * Deliberately an INTERFACE, not an abstract class: bank accounts and prepaid
 * cards are from two UNRELATED class families, yet both can-do this. An
 * abstract class could never serve both — a class gets exactly ONE superclass.
 *
 * PRE-JAVA-8 INTERFACE RULES, all visible in these few lines:
 *   1. Every method is IMPLICITLY `public abstract` — write the modifiers or not,
 *      the compiler adds them. (InterfaceAnatomyDemo proves it with reflection.)
 *   2. Every field is IMPLICITLY `public static final` — an interface can hold
 *      CONSTANTS, never state. Constants are ONE shared copy, not per-object.
 *   3. Method BODIES are forbidden — javac, word for word:
 *        error: interface abstract methods cannot have body
 *      (Java 8 relaxes this with `default` methods — see the teaser at the end
 *       of this module and java8-features/02-interface-evolution.)
 */
public interface Transferable {

    /** Written without a single modifier — yet it IS public static final. The trap:
     *  this looks like a class field but there is no per-object copy; every
     *  implementing class shares this one constant. */
    int MAX_TRANSFERS_PER_DAY = 3;

    /** The receiving half of a transfer. Note: NO body, NO braces — signature only. */
    void deposit(BigDecimal amount);

    /** The sending half. Returns false (instead of throwing) when policy blocks it,
     *  so callers must check the result — a contract detail worth stating in a
     *  real API's javadoc. */
    boolean transferTo(Transferable target, BigDecimal amount);
}
