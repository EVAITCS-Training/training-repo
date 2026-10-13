package com.evaitcs.advanced.annotations;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 03-annotations compile exec:java
 *
 * The order is the argument: first the BUILT-INS you already trust (and who reads them), then the
 * RETENTION axis that decides whether reflection can see a note at all, then the CENTERPIECE — a
 * mini validation framework that reads custom stickers — then the two exact rules (@Repeatable,
 * @Inherited), and finally a tiny DI container that builds an object graph from annotations.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 03-annotations compile exec:java "-Dexec.mainClass=com.evaitcs.advanced.annotations.MiniValidatorDemo"
 */
public class AnnotationDemos {

    public static void main(String[] args) throws Exception {
        section("BuiltInsDemo — @Override, @Deprecated, @FunctionalInterface, @SafeVarargs, @SuppressWarnings");
        BuiltInsDemo.main(args);

        section("RetentionMatrixDemo — SOURCE vs CLASS vs RUNTIME: only RUNTIME reaches reflection");
        RetentionMatrixDemo.main(args);

        section("MiniValidatorDemo — a ~40-line framework that READS custom stickers and reports violations");
        MiniValidatorDemo.main(args);

        section("RepeatableInheritedDemo — @Repeatable needs a container; @Inherited is classes-only");
        RepeatableInheritedDemo.main(args);

        section("WireMeDemo — a 20-line DI container: @WireMe + @Inject build a live object graph");
        WireMeDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
