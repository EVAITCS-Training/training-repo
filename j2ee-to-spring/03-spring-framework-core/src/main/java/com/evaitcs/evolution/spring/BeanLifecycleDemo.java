package com.evaitcs.evolution.spring;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Demo 3 — BEANS, SCOPES, LIFECYCLE, and the failure-time upgrade.
 * (README section "Beans, scopes, lifecycle".)
 *
 * Four proofs, in order:
 *   banner 1  singleton scope   — getBean twice, SAME instance (the Map caches; assertSame in tests).
 *   banner 2  prototype scope   — getBean twice, DIFFERENT instances (the Map declines to cache).
 *   banner 3  lifecycle hooks   — @PostConstruct already fired at refresh; @PreDestroy fires at close().
 *   banner 4  field injection   — the anti-pattern exhibit: it works, until you need to see,
 *                                   test, or refactor the dependency (house rule: constructors).
 *   banner 5  failure at STARTUP — an unsatisfied dependency aborts refresh(): the exact
 *                                   upgrade over module 01's ServiceLocator, which compiled,
 *                                   deployed, and then failed on the first request at 2 a.m.
 */
public class BeanLifecycleDemo {

    public static void main(String[] args) {
        String db = DemoDb.freshDatabase();
        LifecycleProbe.resetFlags();                    // determinism: no stale flags from an earlier demo

        // NOT try-with-resources on purpose: banner 3 must print the @PreDestroy flag
        // AFTER close() has run, so the close happens explicitly, then the flag check.
        AnnotationConfigApplicationContext ctx = SpringApp.start(db);

        banner("1. Singleton scope (the default): one instance per context, cached in the Map");
        TransferService first = ctx.getBean(TransferService.class);
        TransferService second = ctx.getBean(TransferService.class);
        System.out.println("step 1: getBean #1 -> " + name(first));
        System.out.println("step 2: getBean #2 -> " + name(second));
        System.out.println("step 3: first == second ? " + (first == second) + "  (the singleton cache hit — refresh() step 6)");
        System.out.println("verdict: singleton is a SCOPE, not a GoF pattern — 'one per context', and every");
        System.out.println("         collaborator shares it. That is why beans must be stateless/thread-safe.");

        banner("2. Prototype scope: a FRESH instance per getBean — factory, not cache");
        Receipt r1 = ctx.getBean(Receipt.class);
        Receipt r2 = ctx.getBean(Receipt.class);
        System.out.println("step 1: getBean #1 -> " + r1.id());
        System.out.println("step 2: getBean #2 -> " + r2.id());
        System.out.println("step 3: same instance? " + (r1 == r2) + " — the container mints a new one every lookup");
        System.out.println("verdict: @Scope(\"prototype\") for per-use objects; note the container stops tracking");
        System.out.println("         them after hand-off (no @PreDestroy for prototypes — the caller owns them).");

        banner("3. Lifecycle callbacks: @PostConstruct fired at refresh; @PreDestroy fires at close");
        System.out.println("step 1: @PostConstruct ran during SpringApp.start()? " + LifecycleProbe.postConstructRan
                + "  (construction + injection done, then the callback — refresh() step 5)");
        System.out.println("step 2: closing the context (what try-with-resources does in every other demo)...");
        ctx.close();
        System.out.println("step 3: @PreDestroy ran? " + LifecycleProbe.preDestroyRan);
        System.out.println("verdict: the container owns the bean's whole life: build -> inject -> init -> ... -> destroy.");

        banner("4. The anti-pattern exhibit: FIELD injection (@Autowired on a field)");
        try (AnnotationConfigApplicationContext fieldCtx =
                     new AnnotationConfigApplicationContext(FieldInjectionConfig.class)) {
            GreetingConsumer consumer = fieldCtx.getBean(GreetingConsumer.class);
            System.out.println("step 1: inside the container it works: consumer.greet() -> " + consumer.greet());
        }
        System.out.println("step 2: outside the container, 'new GreetingConsumer()' compiles — and the field is null:");
        try {
            new GreetingConsumer().greet();
        } catch (NullPointerException e) {
            System.out.println("        CAUGHT -> NullPointerException: the dependency is INVISIBLE in the API;");
            System.out.println("        WHY: nothing in the constructor says this class needs a Greeting.");
        }
        System.out.println("step 3: with CONSTRUCTOR injection the same mistake cannot compile — the dependency");
        System.out.println("        is the signature; tests pass fakes directly; the object is never half-built.");
        System.out.println("verdict: house rule — constructor injection everywhere; fields are for USING, not GETTING.");

        banner("5. Unsatisfied dependency: startup failure (compare module 01's runtime failure)");
        try (AnnotationConfigApplicationContext broken = new AnnotationConfigApplicationContext(BrokenConfig.class)) {
            System.out.println("never reached — refresh() aborts before this line");
        } catch (BeanCreationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()));
            Throwable root = rootCause(e);
            System.out.println("        root cause: " + root.getClass().getSimpleName() + ": " + firstLine(root.getMessage()));
            System.out.println("        WHY: MonthlyReport needs a Mailer bean; none is defined — so the CONTEXT");
            System.out.println("        REFUSES TO START. The miswiring is discovered in the first second of life,");
            System.out.println("        not on the first unlucky request.");
        }
        System.out.println("step 1: module 01 ServiceLocator: typo'd key compiled, deployed, passed smoke tests,");
        System.out.println("        then threw IllegalStateException at 2 a.m. on the first real lookup.");
        System.out.println("step 2: Spring container: same mistake = BeanCreationException during refresh() —");
        System.out.println("        the application never serves a request in a broken state.");
        System.out.println("In one sentence: injection moves wiring errors from RUNTIME (users present) to STARTUP (deploy in progress).");
    }

    // ---- banner 4 exhibits: the anti-pattern, in five lines --------------------------------

    /** The dependency, a plain bean. */
    public record Greeting(String text) {
    }

    /**
     * FIELD injection: @Autowired straight onto a private field. It works inside a
     * container — and that is exactly the seduction. The class's API (its constructors)
     * no longer mentions what it needs.
     */
    public static class GreetingConsumer {
        @Autowired
        private Greeting greeting;

        public String greet() {
            return greeting.text();                  // NPE outside a container: nothing enforced this was set
        }
    }

    @Configuration
    static class FieldInjectionConfig {
        @Bean
        Greeting greeting() {
            return new Greeting("hello from a field-injected bean");
        }

        @Bean
        GreetingConsumer greetingConsumer() {
            return new GreetingConsumer();           // no-args: the dependency is the container's secret
        }
    }

    // ---- banner 5 exhibit: a graph that cannot be satisfied --------------------------------

    /** Nobody provides a Mailer — refresh() must die, loudly, before any bean is served. */
    public record MonthlyReport(Mailer mailer) {
    }

    public interface Mailer {
    }

    @Configuration
    static class BrokenConfig {
        @Bean
        MonthlyReport monthlyReport(Mailer mailer) {   // <-- parameter the container cannot fill
            return new MonthlyReport(mailer);
        }
    }

    // ---- print helpers ----------------------------------------------------------------------

    private static String name(Object bean) {
        return bean.getClass().getSimpleName() + " @" + Integer.toHexString(System.identityHashCode(bean));
    }

    private static Throwable rootCause(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private static String firstLine(String message) {
        return message == null ? "" : message.lines().findFirst().orElse("");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
