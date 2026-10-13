package com.evaitcs.evolution.app.era1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ERA 1 — the Service Locator, J2EE's answer to "where do I get my dependencies?"
 * Copied from module 01 (condensed; the full anti-pattern analysis lives there).
 *
 * Servlets are created by the CONTAINER, so they cannot receive dependencies through
 * a constructor. The era's workaround: a static registry everyone looks things up in
 * (in full J2EE that registry was JNDI). The three defects — invisible, untyped,
 * globally shared — are why module 03's container wins, and why era 3 in this very
 * module does the same job with constructor injection.
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

    /** Everyone else looks it up by string and CASTS — typo'd key = runtime explosion. */
    @SuppressWarnings("unchecked")
    public static <T> T lookup(String name) {
        Object service = REGISTRY.get(name);
        if (service == null) {
            throw new IllegalStateException("nothing registered under '" + name
                    + "' — in 2001 this was a JNDI NameNotFoundException at 2 a.m.");
        }
        return (T) service;
    }

    /** EvolutionServer.close() calls this to reset the global state between runs — defect #3, felt directly. */
    public static void clear() {
        REGISTRY.clear();
    }
}
