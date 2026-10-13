package com.evaitcs.evolution.j2ee;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Service Locator — J2EE's answer to "where do I get my dependencies?"
 * (README section "Wiring by hand: the Service Locator".)
 *
 * Servlets are created by the CONTAINER, not by you — so a servlet cannot receive its
 * dependencies through a constructor. The era's workaround: a static registry everyone
 * looks things up in (in full J2EE, that registry was JNDI, the container's naming
 * service, and you looked up DataSources and EJB homes through pages of ceremony).
 *
 * Why this is an ANTI-PATTERN (module 10 of advanced-java names it):
 *   1. Dependencies are INVISIBLE — read the servlet's constructor and you learn nothing;
 *      the real dependencies are buried in method bodies as string keys.
 *   2. Untyped — lookup returns Object; the cast can fail at RUNTIME.
 *   3. Untestable in isolation — the static registry is global state shared by every test.
 * Spring's container is the same Map<String, Object> idea with those three defects
 * fixed: dependencies appear in constructors (visible, typed), and each test can build
 * its own context. Module 03 builds exactly that.
 */
public final class ServiceLocator {

    /** The registry: literally a concurrent Map<String, Object>. JNDI was this, with naming conventions. */
    private static final Map<String, Object> REGISTRY = new ConcurrentHashMap<>();

    private ServiceLocator() {
    }

    /** Someone who KNOWS the concrete class registers an instance under a name. */
    public static void register(String name, Object service) {
        REGISTRY.put(name, service);
        System.out.println("[ServiceLocator] registered '" + name + "' -> " + service.getClass().getSimpleName());
    }

    /**
     * Everyone else looks it up by string and CASTS — the two failure points Spring
     * injection removes (typo'd key = null at runtime; wrong cast = ClassCastException).
     */
    @SuppressWarnings("unchecked")
    public static <T> T lookup(String name) {
        Object service = REGISTRY.get(name);
        if (service == null) {
            throw new IllegalStateException("nothing registered under '" + name
                    + "' — in 2001 this was a JNDI NameNotFoundException at 2 a.m.");
        }
        return (T) service;
    }

    /** Tests call this to reset the global state between runs — defect #3, felt directly. */
    public static void clear() {
        REGISTRY.clear();
    }
}
