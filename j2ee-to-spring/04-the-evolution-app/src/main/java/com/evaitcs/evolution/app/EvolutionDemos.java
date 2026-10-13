package com.evaitcs.evolution.app;

/**
 * Launcher for the whole module — runs the capstone demo.
 *
 *   .\mvnw.cmd -pl 04-the-evolution-app compile exec:java
 *
 * One demo, because the capstone IS one demo: the same transfer through three eras on
 * one server, ending at the wiring ledger that hands every line of this module to a
 * named Boot auto-configuration. (Modules 01-03 carried the multi-demo tours; this
 * module's tour is the server itself.)
 *
 * To run it directly while pointing at it:
 *   .\mvnw.cmd -pl 04-the-evolution-app compile exec:java "-Dexec.mainClass=com.evaitcs.evolution.app.EvolutionServerDemo"
 */
public class EvolutionDemos {

    public static void main(String[] args) {
        section("EvolutionServerDemo — the SAME bank transfer, THREE eras, ONE embedded server");
        EvolutionServerDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
