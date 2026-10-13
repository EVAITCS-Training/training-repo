package com.evaitcs.corejava.contracts;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 07-interfaces-vs-abstract-classes compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 07-interfaces-vs-abstract-classes compile exec:java "-Dexec.mainClass=com.evaitcs.corejava.contracts.TemplateMethodDemo"
 */
public class ContractsDemos {

    public static void main(String[] args) {
        section("InterfaceAnatomyDemo — what an interface may contain, pre-Java-8");
        InterfaceAnatomyDemo.main(args);

        section("AbstractAccountDemo — state, constructors, partial implementation");
        AbstractAccountDemo.main(args);

        section("TemplateMethodDemo — the abstract class killer app: final skeleton, abstract blanks");
        TemplateMethodDemo.main(args);

        section("MultipleInheritanceDemo — extends ONE class, implements MANY interfaces, and why");
        MultipleInheritanceDemo.main(args);

        section("DecisionWalkthroughDemo — the capability table and how to actually decide");
        DecisionWalkthroughDemo.main(args);

        section("AnonymousCallbackDemo — interfaces as callback contracts, the pre-lambda way");
        AnonymousCallbackDemo.main(args);

        section("Java8TeaserDemo — which rows of the table Java 8 rewrites (PRINTS ONLY)");
        Java8TeaserDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
