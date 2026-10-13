package com.evaitcs.advanced.gof;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 09-design-patterns-gof compile exec:java
 *
 * The order is the argument: first the MAP (patterns are names, not recipes), then
 * Strategy — the pattern every student already wrote as a Comparator — then the
 * creational trio (Factory, Builder, Singleton ends the module on its DI bridge),
 * with the structural and behavioral shapes in between. Template Method follows
 * Strategy because they are the inheritance-vs-parameter pair; Decorator follows
 * Template because module 02's Proxy is still fresh for the contrast; Observer
 * lands just before Singleton so the "publish, don't hard-wire" theme carries into
 * "inject, don't globalize" — which is module 10's opening move.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 09-design-patterns-gof compile exec:java "-Dexec.mainClass=com.evaitcs.advanced.gof.FeeStrategyDemo"
 */
public class GofDemos {

    public static void main(String[] args) throws Exception {
        section("PatternMapDemo — the 23 GoF patterns, 3 intents, each with a JDK example you already used");
        PatternMapDemo.main(args);

        section("FeeStrategyDemo — Strategy: swap the fee algorithm at runtime (a Comparator IS one)");
        FeeStrategyDemo.main(args);

        section("PaymentFactoryDemo — Factory Method: from(String) picks the sealed subtype, fails loudly");
        PaymentFactoryDemo.main(args);

        section("LoanBuilderDemo — Builder: fluent collection, validation once, no half-made objects");
        LoanBuilderDemo.main(args);

        section("ReportTemplateDemo — Template Method: final skeleton, subclass slots, exact step order");
        ReportTemplateDemo.main(args);

        section("AuditDecoratorDemo — Decorator: same interface, added audit line, result unchanged (vs Proxy)");
        AuditDecoratorDemo.main(args);

        section("LegacyLedgerAdapterDemo — Adapter: 2009 pipe-rows translated to typed modern entries");
        LegacyLedgerAdapterDemo.main(args);

        section("BalanceObserverDemo — Observer: listeners notified in order, silence after deregister");
        BalanceObserverDemo.main(args);

        section("EnumSingletonDemo — Singleton: the enum form beats reflection AND serialization; DI beats both");
        EnumSingletonDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
