package com.evaitcs.java8.interfaces;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 02-interface-evolution compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 02-interface-evolution compile exec:java "-Dexec.mainClass=com.evaitcs.java8.interfaces.DiamondConflictDemo"
 */
public class InterfaceEvolutionDemos {

    public static void main(String[] args) throws Exception {
        section("WhyDefaultMethodsDemo — the interface-evolution problem");
        WhyDefaultMethodsDemo.main(args);

        section("StaticInterfaceMethodDemo — helpers on the contract, not on instances");
        StaticInterfaceMethodDemo.main(args);

        section("ResolutionRulesDemo — who wins: class, sub-interface, or default?");
        ResolutionRulesDemo.main(args);

        section("DiamondConflictDemo — the diamond, and InterfaceName.super.method()");
        DiamondConflictDemo.main(args);

        section("CompositionBridgeDemo — default methods power module 01's Predicate.and/negate");
        CompositionBridgeDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
