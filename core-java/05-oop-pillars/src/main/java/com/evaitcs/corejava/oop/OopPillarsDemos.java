package com.evaitcs.corejava.oop;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 05-oop-pillars compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 05-oop-pillars compile exec:java "-Dexec.mainClass=com.evaitcs.corejava.oop.PolymorphismDemo"
 */
public class OopPillarsDemos {

    public static void main(String[] args) {
        section("ClassVsObjectDemo — blueprints, houses, and what `new` actually does");
        ClassVsObjectDemo.main(args);

        section("EncapsulationDemo — private state, public rules, and an invariant that says NO");
        EncapsulationDemo.main(args);

        section("InheritanceDemo — extends, @Override, and the typo'd-signature trap");
        InheritanceDemo.main(args);

        section("ConstructionOrderDemo — static init -> super ctor -> child ctor, step by step");
        ConstructionOrderDemo.main(args);

        section("PolymorphismDemo — one list, many behaviors: dynamic dispatch");
        PolymorphismDemo.main(args);

        section("OverloadVsOverrideDemo — compile-time name-sharing vs runtime dispatch");
        OverloadVsOverrideDemo.main(args);

        section("AbstractionDemo — shapes without gears: abstract class + interface");
        AbstractionDemo.main(args);

        section("BankDayStoryDemo — all four pillars in one day at the bank");
        BankDayStoryDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
