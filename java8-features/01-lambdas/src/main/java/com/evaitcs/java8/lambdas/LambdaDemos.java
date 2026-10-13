package com.evaitcs.java8.lambdas;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 01-lambdas compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 01-lambdas compile exec:java "-Dexec.mainClass=com.evaitcs.java8.lambdas.ThisMeaningDemo"
 */
public class LambdaDemos {

    public static void main(String[] args) {
        section("LambdaSyntaxDemo — the shapes of a lambda");
        LambdaSyntaxDemo.main(args);

        section("AnonymousVsLambdaDemo — Java 7 way vs Java 8 way");
        AnonymousVsLambdaDemo.main(args);

        section("ThisMeaningDemo — what `this` means inside each");
        ThisMeaningDemo.main(args);

        section("EffectivelyFinalDemo — what a lambda may capture");
        EffectivelyFinalDemo.main(args);

        section("OwnFunctionalInterfaceDemo — writing your own SAM interface");
        OwnFunctionalInterfaceDemo.main(args);

        section("MethodReferenceDemo — the four shapes of ::");
        MethodReferenceDemo.main(args);

        section("ComposingDemo — building big behavior from small behavior");
        ComposingDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
