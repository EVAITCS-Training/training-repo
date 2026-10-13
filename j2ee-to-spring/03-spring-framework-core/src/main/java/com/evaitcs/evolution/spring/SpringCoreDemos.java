package com.evaitcs.evolution.spring;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 03-spring-framework-core compile exec:java
 *
 * The order is the argument: first the CONTAINER from first principles (a Map you build
 * by hand, then refresh()'s walkthrough), then the WIRING ERAS (XML live, annotations,
 * Java config), then BEHAVIOR (scopes, lifecycle, startup-vs-runtime failure), then the
 * DELETIONS of module 01's plumbing one banner at a time: JdbcTemplate kills the DAO
 * ritual, @Transactional kills the commit/rollback dance, AOP shows the proxy that did
 * it, and events show what grows on top. Module 01's ServiceLocator and JdbcTransferDao
 * are the "before" pictures; this run is the "after".
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 03-spring-framework-core compile exec:java "-Dexec.mainClass=com.evaitcs.evolution.spring.TransactionalTransferDemo"
 */
public class SpringCoreDemos {

    public static void main(String[] args) throws Exception {
        section("MiniContainerDemo — Spring is a Map<String, Object>: build one in 25 lines, then watch refresh()");
        MiniContainerDemo.main(args);

        section("XmlEraDemo — 2004's wiring file, loaded live: the same graph in XML and in Java");
        XmlEraDemo.main(args);

        section("BeanLifecycleDemo — singleton vs prototype, @PostConstruct/@PreDestroy, startup-vs-runtime failure");
        BeanLifecycleDemo.main(args);

        section("JdbcTemplateRewriteDemo — module 01's DAO plumbing, deleted banner by banner");
        JdbcTemplateRewriteDemo.main(args);

        section("TransactionalTransferDemo — @Transactional: commit, rollback, and the checked-exception trap");
        TransactionalTransferDemo.main(args);

        section("AopAndEventsDemo — the proxy under @Transactional, the self-invocation trap, and events");
        AopAndEventsDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
