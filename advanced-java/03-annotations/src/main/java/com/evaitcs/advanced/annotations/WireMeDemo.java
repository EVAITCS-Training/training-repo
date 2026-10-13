package com.evaitcs.advanced.annotations;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Demo 5 — A TINY DEPENDENCY-INJECTION CONTAINER. (README section "WireMe: a 20-line Spring".)
 *
 * The final proof that "annotation + reader = framework." Two stickers:
 *   @WireMe marks a class the container should build (Spring's @Component).
 *   @Inject marks a field the container should fill by type (Spring's @Autowired).
 * The reader below — about twenty lines — instantiates every @WireMe class, then walks each
 * bean's fields and sets every @Inject field to the one bean whose type matches. That is the
 * CORE of dependency injection. Spring adds classpath scanning, scopes, proxies (module 02) and
 * lifecycle callbacks on top, but the heart is exactly this loop.
 */
public class WireMeDemo {

    /** A component with no dependencies. Gets the default bean name "auditLogger". */
    @WireMe
    public static class AuditLogger {
        String lastEvent = "(none)";

        void log(String event) {
            lastEvent = event;
        }
    }

    /** A component that DEPENDS on AuditLogger. Gets the explicit bean name "transfers". */
    @WireMe("transfers")
    public static class TransferHandler {
        @Inject
        AuditLogger logger;   // package-private so the demo and the test can inspect the wired result

        String transfer(String from, String to) {
            logger.log(from + "->" + to);
            return "moved " + from + "->" + to;
        }
    }

    public static void main(String[] args) throws Exception {
        banner("1. Scan and instantiate every @WireMe component");

        Map<String, Object> beans = wire(List.of(AuditLogger.class, TransferHandler.class));
        System.out.println("step 1: handed the container [AuditLogger, TransferHandler].");
        System.out.println("step 2: it registered beans named " + beans.keySet());
        System.out.println("verdict: @WireMe(\"transfers\") chose that name; AuditLogger got the default (decapitalized simple name).");

        banner("2. Inject every @Inject field by type");

        TransferHandler handler = (TransferHandler) beans.get("transfers");
        System.out.println("step 1: TransferHandler.logger carries @Inject.");
        System.out.println("step 2: after wiring, handler.logger == null? " + (handler.logger == null));
        System.out.println("step 3: is it the SAME AuditLogger the container built? " + (handler.logger == beans.get("auditLogger")));
        System.out.println("verdict: the reader matched the field's TYPE to a bean and set it — @Autowired in miniature.");

        banner("3. Prove the wired graph actually works");

        String result = handler.transfer("A", "B");
        System.out.println("step 1: handler.transfer(\"A\",\"B\") = " + result);
        System.out.println("step 2: the injected logger recorded = " + handler.logger.lastEvent);
        System.out.println("verdict: the dependency is real — calling the handler reached the injected logger.");

        banner("4. This is Spring's DI container, minus the extras");

        System.out.println("Replace @WireMe with @Component, @Inject with @Autowired, this Map with the");
        System.out.println("ApplicationContext, and 'match by type' with Spring's autowiring — and you have DI.");
        System.out.println("Spring adds classpath scanning, scopes, proxies (module 02) and lifecycle callbacks,");
        System.out.println("but the core is these two passes: instantiate the marked, then inject the marked fields.");

        banner("5. In one sentence");
        System.out.println("Dependency injection is a reader that turns @WireMe/@Inject stickers into a live object");
        System.out.println("graph — twenty lines here, and the exact idea Spring scales to a whole application.");
    }

    /**
     * The container. Pass 1 builds a named instance for every @WireMe class. Pass 2 fills every
     * @Inject field with the first bean assignable to the field's type. Reflection (module 02) does
     * the reading and the setting; the annotations only say WHERE to look.
     */
    static Map<String, Object> wire(List<Class<?>> componentClasses) throws ReflectiveOperationException {
        Map<String, Object> beans = new LinkedHashMap<>();

        // Pass 1 — instantiate every @WireMe class under its bean name.
        for (Class<?> type : componentClasses) {
            WireMe mark = type.getAnnotation(WireMe.class);
            if (mark == null) {
                continue;
            }
            String name = mark.value().isEmpty() ? decapitalize(type.getSimpleName()) : mark.value();
            beans.put(name, type.getDeclaredConstructor().newInstance());
        }

        // Pass 2 — fill every @Inject field with the one bean whose type matches.
        for (Object bean : beans.values()) {
            for (Field field : bean.getClass().getDeclaredFields()) {
                if (!field.isAnnotationPresent(Inject.class)) {
                    continue;
                }
                field.setAccessible(true);
                Object match = beans.values().stream()
                        .filter(candidate -> field.getType().isInstance(candidate))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "no bean of type " + field.getType().getSimpleName() + " to inject"));
                field.set(bean, match);
            }
        }
        return beans;
    }

    /** "AuditLogger" -> "auditLogger": Spring's default bean-naming rule, in one line. */
    static String decapitalize(String name) {
        return name.isEmpty() ? name : Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
