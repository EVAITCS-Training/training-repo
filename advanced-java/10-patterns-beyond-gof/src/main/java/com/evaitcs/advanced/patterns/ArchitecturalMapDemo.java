package com.evaitcs.advanced.patterns;

/**
 * Demo A0 — THE WHOLE MODULE ON ONE SCREEN: the four families beyond GoF.
 * (README section "The map: four families the GoF never covered".)
 *
 * The GoF's 23 patterns (module 09) are about OBJECTS: how one object creates,
 * composes with, or talks to another. Real systems are built from larger shapes —
 * how BOXES arrange (architectural), how THREADS share (concurrency), how APIs
 * READ (idioms), and how codebases DIE (anti-patterns). Every row below names
 * where you already saw the pattern, or where you are about to see it for real:
 * stage-2-monolith runs family A on Spring; stage-5-microservices runs sagas,
 * gateways and event-driven flows at production size.
 */
public class ArchitecturalMapDemo {

    public static void main(String[] args) {
        banner("1. Why 'beyond GoF' is not a footnote");

        System.out.println("The GoF book is from 1994: one JVM, one process, objects all the way down.");
        System.out.println("The systems you will build are layered services talking over networks,");
        System.out.println("running thousands of concurrent requests. The vocabulary had to grow —");
        System.out.println("four families, each with patterns you have ALREADY met in this curriculum.");

        banner("2. FAMILY A — ARCHITECTURAL: how the boxes are arranged");

        row("Layered", "controller -> service -> repository; one-way traffic",
                "stage-2's web/service/repository/domain packages; LayeredBankDemo here");
        row("Dependency Injection / IoC", "declare needs, a container builds and injects them",
                "stage-2's @Autowired at startup; MiniContainerDemo rebuilds it in ~60 lines");
        row("Repository (vs DAO)", "storage shaped like a collection of DOMAIN objects",
                "stage-2's AccountRepository (Spring Data generates the implementation)");
        row("DTO", "a frozen record that crosses a boundary; entities stay home",
                "stage-2's web/dto package (TransferRequest/TransferResponse records)");
        row("Facade / API Gateway", "one front door hiding many services behind it",
                "stage-5's api-gateway: routes, auth and rate limits for eight services");
        row("Saga", "a sequence of local steps, each with a compensating undo",
                "stage-5's payment-service TransferSagaService: debit, credit, or roll back");
        row("Event-driven", "publish what HAPPENED; whoever cares subscribes",
                "stage-5's Kafka topics; module 09's Observer grown up to network scale");
        row("Hexagonal (ports & adapters)", "domain in the center; IO at the edges via interfaces",
                "the shape stage-5's services approximate: business core, adapters outside");

        banner("3. FAMILY B — CONCURRENCY: how threads share (full map in its own demo)");

        row("Producer-consumer", "a queue between makers and takers of work",
                "BlockingQueue (collections-framework 06+10); module 06's pools");
        row("Immutability / confinement", "unshared or unchanging state needs no locks",
                "module 04's happy fact; ConcurrencyPatternMapDemo's live scene");
        row("Worker pool · Future · Reactor · Fork-join", "the organized answers to 'who runs it'",
                "modules 06 and 08; java8-features 07 (parallel streams)");

        banner("4. FAMILY C — API IDIOMS: how code reads");

        row("Fluent interface", "setters return this, so calls chain into sentences",
                "module 09's Builder; StringBuilder; IdiomsDemo scene 1");
        row("Static factory methods", "valueOf / of / from / getInstance instead of new",
                "everywhere in the JDK; Money.of here; PaymentFactory.from in module 09");
        row("Composition over inheritance", "has-a + delegation beats is-a for swapping behavior",
                "IdiomsDemo scene 3; module 09's Decorator is the same shape");
        row("Law of Demeter", "only talk to immediate friends — one hop, not four",
                "IdiomsDemo scene 4: city() survives a restructure, the train wreck does not");
        row("Null Object vs Optional", "typed shapes for 'nothing', chosen by position",
                "java8-features 03 (Optional); IdiomsDemo scene 5");
        row("Value Object", "defined by value; invalid states unrepresentable",
                "MoneyValueObjectDemo: Money record with a compact-constructor bouncer");

        banner("5. FAMILY D — ANTI-PATTERNS: how banking codebases die");

        row("Anemic domain model", "data here, rules over there — invariants slip through gaps",
                "AntiPatternsDemo: a scripted overdraft CORRUPTS the anemic account");
        row("God class", "one class parses HTTP, writes SQL, mails and PDFs — 4,000 lines",
                "AntiPatternsDemo banner 4: the six-smell checklist");
        row("Singleton abuse / service locator", "global lookup hides dependencies and poisons tests",
                "module 09's Singleton trap; MiniContainerDemo is the cure");
        row("Shared mutable state", "the root race behind lost updates and lock gymnastics",
                "modules 04-05 lived it; ConcurrencyPatternMapDemo narrates the 73412");
        row("Golden hammer · premature optimization", "one tool for everything; speed before measurement",
                "AntiPatternsDemo banner 6: the two one-liners");

        banner("6. How to read this map");

        System.out.println("Family A decides where code LIVES. Family B decides who may TOUCH state.");
        System.out.println("Family C decides how an API READS. Family D is what happens when A-C are skipped.");
        System.out.println("verdict: the GoF named the object-level shapes; these four families name the");
        System.out.println("         system-level ones — and stage-2/stage-5 are literally written in them.");

        banner("7. In one sentence");
        System.out.println("Beyond GoF, patterns stop being about single objects and start being about");
        System.out.println("arrangements — of layers, threads, APIs and mistakes.");
    }

    /** One map row: pattern | one-line meaning | where you already saw it. */
    private static void row(String pattern, String what, String where) {
        System.out.printf("  %-34s | %s%n", pattern, what);
        System.out.printf("  %-34s | seen in: %s%n", "", where);
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
