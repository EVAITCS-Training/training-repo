package com.evaitcs.corejava.statics;

/**
 * Launcher for the whole module - runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 11-static-and-class-vs-instance compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 11-static-and-class-vs-instance compile exec:java "-Dexec.mainClass=com.evaitcs.corejava.statics.InitOrderDemo"
 */
public class StaticsDemos {

    public static void main(String[] args) {
        section("SharedStaticStateDemo - ONE slot in the class, shared by every object");
        SharedStaticStateDemo.main(args);

        section("StaticMethodsAndFactoriesDemo - no object, no `this`, and why that is useful");
        StaticMethodsAndFactoriesDemo.main(args);

        section("UtilityClassDemo - the all-static class done right: private ctor, constants, the final-List trap");
        UtilityClassDemo.main(args);

        section("InitOrderDemo - class load vs every `new`, printed step by step");
        InitOrderDemo.main(args);

        section("StaticStateCautionDemo - static mutable state is GLOBAL state");
        StaticStateCautionDemo.main(args);

        section("NestedVsInnerDemo - static nested vs inner: the naming trap");
        NestedVsInnerDemo.main(args);

        section("StaticHidingDemo - statics are hidden, not overridden + what cannot be static");
        StaticHidingDemo.main(args);

        section("ClassVsInstanceSummaryDemo - the whole module on one screen");
        ClassVsInstanceSummaryDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
