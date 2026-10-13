package com.evaitcs.evolution.spring;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The module's one-stop bootstrap: a fresh DemoDb URL in, a fully wired Spring context out.
 * (README section "The container from first principles"; the counterpart of module 01's
 * J2eeServer — except there is NO server here: the container is a library, not a process.)
 *
 * WHY this class exists: every demo and every test needs its OWN world (house determinism
 * rule) — one fresh in-memory database, one fresh context, closed again afterwards. The two
 * start methods hide the one bit of ceremony that plain Spring (without Boot) still has:
 * telling the context WHERE the database lives. We do it the Framework way: a property
 * source added to the Environment before refresh(), which AppConfig's @Bean method reads
 * as ${db.url}. (stage-2: Boot fills the same Environment from application.properties —
 * spring.datasource.url — and you never write this class at all.)
 *
 * Both contexts returned here are AutoCloseable: demos and tests use try-with-resources,
 * and close() fires every @PreDestroy and releases the singleton map.
 */
public final class SpringApp {

    /**
     * JUL loggers are held WEAKLY by the LogManager — set a level, drop the reference, and
     * the logger (level with it) can be garbage-collected. This list keeps ours alive for
     * the life of the JVM (the same trap J2eeServer documents; advanced-java/07's
     * ReferenceTypesDemo would approve).
     */
    private static final List<java.util.logging.Logger> HELD_LOGGERS = new ArrayList<>();

    static {
        // Spring logs through spring-jcl, which falls through to java.util.logging when no
        // other backend is on the classpath. Startup chatter at INFO/WARNING drowns the
        // lesson, so the framework's loggers are silenced; our teaching output is
        // System.out and stays untouched.
        java.util.logging.Logger spring = java.util.logging.Logger.getLogger("org.springframework");
        spring.setLevel(java.util.logging.Level.SEVERE);
        spring.setUseParentHandlers(false);
        HELD_LOGGERS.add(spring);
    }

    private SpringApp() {
    }

    /** The property key AppConfig and beans.xml both read — the pre-Boot stand-in for spring.datasource.url. */
    public static final String DB_URL_PROPERTY = "db.url";

    /**
     * Builds the annotation-configured context (AppConfig) against the given database.
     *
     * Walkthrough — what the four lines below do (this IS the modern era's whole setup):
     *   1. new AnnotationConfigApplicationContext() — an EMPTY container: it registers the
     *      annotation post-processors but refreshes nothing yet.
     *   2. addFirst(MapPropertySource) — publish db.url into the Environment BEFORE refresh,
     *      so @Value("${db.url}") can resolve when beans are created.
     *   3. register(AppConfig.class) — hand the container its blueprint: bean DEFINITIONS
     *      (recipes), not beans. Nothing is instantiated yet.
     *   4. refresh() — the one method that does everything (its own walkthrough lives in
     *      MiniContainerDemo banner 4): definitions parsed, singletons instantiated,
     *      constructor-injected, initialized, proxied, cached in the singleton Map.
     */
    public static AnnotationConfigApplicationContext start(String dbUrl) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("demoDb", Map.of(DB_URL_PROPERTY, dbUrl)));
        ctx.register(AppConfig.class);
        ctx.refresh();
        return ctx;
    }

    /**
     * Builds the 2004-style XML context (beans.xml) against the given database — same graph,
     * older language (XmlEraDemo). refresh=false on the constructor so the property source
     * can be added BEFORE the beans are created; then refresh() by hand. The ${db.url}
     * placeholder inside beans.xml is resolved by the PropertySourcesPlaceholderConfigurer
     * bean declared in that very file — XML needed an extra bean for what the annotation
     * context does by default.
     */
    public static ClassPathXmlApplicationContext startXml(String dbUrl) {
        ClassPathXmlApplicationContext ctx =
                new ClassPathXmlApplicationContext(new String[] { "beans.xml" }, false);
        ctx.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("demoDb", Map.of(DB_URL_PROPERTY, dbUrl)));
        ctx.refresh();
        return ctx;
    }
}
