package com.evaitcs.advanced.exceptions;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 11-exceptions compile exec:java
 *
 * The order is the argument: first the FAMILY and the checked/unchecked contract (the map),
 * then what `throw` physically DOES — stack unwinding (the aha), then how to carry a failure
 * upward honestly (cause chain, multi-catch, precise rethrow), then resource safety
 * (try-with-resources and suppression, the module-08 mechanism seen from here), then the
 * DESIGN centerpiece (a sealed, typed banking-error family), then the COST (fillInStackTrace
 * and why control-flow-by-throw is slow), and finally the Java 21 corner (helpful NPEs and
 * compiler-checked exhaustiveness).
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 11-exceptions compile exec:java "-Dexec.mainClass=com.evaitcs.advanced.exceptions.StackUnwindingDemo"
 */
public class ExceptionDemos {

    public static void main(String[] args) throws Exception {
        section("ThrowableHierarchyDemo — Error vs Exception vs RuntimeException; checked vs unchecked");
        ThrowableHierarchyDemo.main(args);

        section("StackUnwindingDemo — what throw DOES: frames pop until a catch matches, or the thread dies");
        StackUnwindingDemo.main(args);

        section("CauseAndRethrowDemo — wrap WITH the cause, never swallow it; multi-catch; precise rethrow");
        CauseAndRethrowDemo.main(args);

        section("SuppressionDemo — try-with-resources: reverse close order and suppressed exceptions");
        SuppressionDemo.main(args);

        section("BankingErrorsDemo — custom exceptions the RIGHT way: a sealed, typed error family");
        BankingErrorsDemo.main(args);

        section("ExceptionCostDemo — fillInStackTrace is the cost; exceptions are not control flow");
        ExceptionCostDemo.main(args);

        section("Java21ExceptionsDemo — helpful NullPointerExceptions and compiler-checked exhaustiveness");
        Java21ExceptionsDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
