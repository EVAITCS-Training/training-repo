package com.evaitcs.evolution.spring;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * Demo 1 — THE CONTAINER FROM FIRST PRINCIPLES: before trusting Spring's Map, build one.
 * (README section "The container from first principles".)
 *
 * The two terms this module exists to demystify, in plain words:
 *   - IoC — INVERSION OF CONTROL: you stop calling `new` on your collaborators; something
 *     else decides when they are built. The "something else" is the container.
 *   - DI — DEPENDENCY INJECTION: the container hands each object its collaborators,
 *     through its constructor (house rule) — it CALLS YOUR CONSTRUCTOR for you.
 *
 * Banner 2 proves the idea needs no framework: MiniContainer below is ~25 lines — a Map of
 * factories, a Map of singletons, one getBean() that creates-on-demand and caches. The
 * REAL container in banner 3 is the same algorithm with extraordinary discipline added:
 * definitions instead of live factories, scopes, lifecycle callbacks, post-processors
 * (which is where proxies come from), events, environments — and 20 years of edge cases.
 *
 * Banner 4 is the walkthrough of context.refresh(): the single method where all of it happens.
 */
public class MiniContainerDemo {

    public static void main(String[] args) {
        banner("1. The problem module 01 could not solve: who calls the constructor?");
        System.out.println("step 1: module 01's servlet could not receive dependencies — the container made it,");
        System.out.println("        so it looked them up at RUNTIME in a static Map by STRING (ServiceLocator).");
        System.out.println("step 2: Spring inverts that: you declare what needs what; the CONTAINER builds the");
        System.out.println("        graph and calls your constructors — at STARTUP, type-checked, once.");
        System.out.println("verdict: 'IoC' = you stop calling new; 'DI' = the container calls your constructor.");

        banner("2. A container in 25 lines — MiniContainer, built by hand below");
        String db = DemoDb.freshDatabase();
        MiniContainer mini = new MiniContainer();
        // registerFactory = "here is the RECIPE for bean X" (Spring: the bean DEFINITION).
        // Each recipe may call getBean() for its own dependencies — that recursion IS the graph.
        mini.registerFactory("dataSource", () -> new DriverManagerDataSource(db));
        mini.registerFactory("transferRepository", () -> new JdbcTransferRepository(
                (DataSource) mini.getBean("dataSource")));                       // needs the datasource...
        mini.registerFactory("transferService", () -> new TransferService(
                (JdbcTransferRepository) mini.getBean("transferRepository"),     // ...needs the repository...
                (ApplicationEventPublisher) event ->                             // ApplicationEventPublisher is a
                        System.out.println("  [mini-publisher] event: " + event) // functional interface: one lambda
        ));                                                                      // stands in for the real bus.

        System.out.println("step 1: three recipes registered. Nothing constructed yet — definitions, not beans.");
        Object service = mini.getBean("transferService");
        System.out.println("step 2: getBean(\"transferService\") created, in dependency order: " + mini.creationLog());
        Object again = mini.getBean("transferService");
        System.out.println("step 3: getBean again -> same instance? " + (service == again)
                + " (the singleton cache — Spring's default scope, proven again in demo 3)");
        System.out.println("step 4: the hand-wired graph actually works — transfer 10.00 from account 1 to 3:");
        String reference = ((TransferService) service).transfer(1, 3, new BigDecimal("10.00"));
        System.out.println("        reference = " + reference);
        System.out.println("        account 1 = " + DemoDb.balanceOf(db, 1) + " (was 1500.00; no transaction yet —");
        System.out.println("        each statement auto-committed; demo 5 adds the transaction, demo 6 the proxy)");
        System.out.println("verdict: a Map<String, Object> + create-on-demand + cache = a container. That is the whole trick.");

        banner("3. The real container: AnnotationConfigApplicationContext builds the SAME graph from AppConfig");
        String db2 = DemoDb.freshDatabase();                 // fresh world, house rule — even between banners
        try (AnnotationConfigApplicationContext ctx = SpringApp.start(db2)) {
            System.out.println("step 1: bean definition names (the recipes Spring parsed out of AppConfig —");
            System.out.println("        note transactionInterceptor/transactionAttributeSource: @EnableTransactionManagement");
            System.out.println("        is itself just an import that REGISTERS MORE DEFINITIONS — annotations add beans):");
            for (String name : ctx.getBeanDefinitionNames()) {
                if (!name.startsWith("org.springframework")) {   // skip the framework's own internal beans
                    System.out.println("        " + name);
                }
            }
            Object real = ctx.getBean("transferService");
            System.out.println("step 2: getBean(\"transferService\") -> " + real.getClass().getSimpleName()
                    + " — the same singleton rule as MiniContainer, plus a CGLIB proxy wrapper (demo 6 explains why).");
            System.out.println("step 3: our three beans exist in BOTH containers; Spring's version adds scopes,");
            System.out.println("        lifecycle callbacks, exception translation, transactions, AOP, events —");
            System.out.println("        all hung off the same create-on-demand-and-cache core.");
            System.out.println("verdict: Spring is a Map<String, Object> with extraordinary discipline.");
        }

        banner("4. Walkthrough — what context.refresh() does (the one method that builds the world)");
        System.out.println("step 1: PARSE CONFIG — ConfigurationClassPostProcessor reads @Configuration classes:");
        System.out.println("        every @Bean method becomes a BeanDefinition (a recipe: class, scope, init method).");
        System.out.println("step 2: REGISTER POST-PROCESSORS — the machines that act ON beans later:");
        System.out.println("        @Value/@Autowired resolution, @PostConstruct, the auto-proxy creator (AOP + tx).");
        System.out.println("step 3: INSTANTIATE SINGLETONS — for each definition, call the factory (your @Bean method).");
        System.out.println("step 4: INJECT — each method parameter is resolved by TYPE: getBean(DataSource.class)...");
        System.out.println("        recursion builds dependencies first, exactly like MiniContainer's getBean calls.");
        System.out.println("step 5: INITIALIZE — @PostConstruct fires; then post-processors get the LAST word:");
        System.out.println("        the auto-proxy creator replaces advised beans with their proxies right here.");
        System.out.println("step 6: CACHE — each finished singleton lands in singletonObjects: THE Map. getBean()");
        System.out.println("        from now on is a lookup. Failures in steps 3-5 abort startup (demo 3, banner 5).");
        System.out.println("In one sentence: refresh() turns recipes into a cached, injected, proxied object graph.");
    }

    /**
     * The honest 25-line container: factories by name (bean definitions), singletons by
     * name (the cache), and getBean() that creates-on-demand — dependencies resolve through
     * recursion because a factory may call getBean() itself. Real Spring separates these
     * two maps into DefaultListableBeanFactory (definitions) and DefaultSingletonBeanRegistry
     * (singletonObjects — literally a Map<String, Object>) and then never stops adding
     * discipline. The core stays exactly this.
     */
    static final class MiniContainer {
        private final Map<String, Supplier<Object>> factories = new LinkedHashMap<>();
        private final Map<String, Object> singletons = new LinkedHashMap<>();
        private final List<String> creationLog = new ArrayList<>();

        void registerFactory(String name, Supplier<Object> factory) {
            factories.put(name, factory);
        }

        Object getBean(String name) {
            Object cached = singletons.get(name);
            if (cached != null) {
                return cached;                                 // singleton rule: build once, reuse forever
            }
            Supplier<Object> factory = factories.get(name);
            if (factory == null) {
                throw new NoSuchElementException("no bean named '" + name + "'");
            }
            Object bean = factory.get();                       // may call getBean(dependency) — the DI graph, recursively
            creationLog.add(name);                             // logged AFTER creation: dependencies always appear first
            singletons.put(name, bean);
            return bean;
        }

        List<String> creationLog() {
            return List.copyOf(creationLog);
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
