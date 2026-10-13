package com.evaitcs.evolution.spring;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * A bean that RECORDS its own lifecycle so demos can print it and tests can assert it.
 * (README section "Beans, scopes, lifecycle".)
 *
 * @PostConstruct fires ONCE, after construction AND after injection — the place for
 * "warm-up" work that needs the dependencies to be there (a constructor cannot assume
 * that when the container populates fields/setters; with constructor injection the two
 * moments coincide, but the callback stays the conventional hook).
 * @PreDestroy fires on context.close() — the try-with-resources moment every demo uses.
 *
 * Both annotations come from jakarta.annotation-api: they LEFT the JDK in Java 11 with
 * the rest of Java EE, which is why the pom declares that dependency explicitly.
 * (stage-2: same annotations, same callbacks — Boot changes nothing here.)
 *
 * The flags are static on purpose: the tests must read them AFTER ctx.close(), when the
 * instance itself is already released; SpringApp's per-method fresh world resets them.
 */
public class LifecycleProbe {

    public static boolean postConstructRan;
    public static boolean preDestroyRan;

    /** Called by demos/tests BEFORE building a context — determinism: no stale flags. */
    public static void resetFlags() {
        postConstructRan = false;
        preDestroyRan = false;
    }

    @PostConstruct
    void onStartup() {
        postConstructRan = true;
        System.out.println("[probe] @PostConstruct fired — built, injected, ready");
    }

    @PreDestroy
    void onShutdown() {
        preDestroyRan = true;
        System.out.println("[probe] @PreDestroy fired — context.close() is shutting the singletons down");
    }
}
