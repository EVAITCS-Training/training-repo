package com.evaitcs.advanced.generics;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 01-generics-deep-dive compile exec:java
 *
 * The order is the argument: first PROVE erasure (everything else follows from it),
 * then the sound flexibility wildcards buy back (PECS), then erasure's visible
 * fingerprint (bridge methods), then the five forbidden moves and their workarounds,
 * and finally the same rules wearing Java 21 clothes (records, sealed, switch).
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 01-generics-deep-dive compile exec:java "-Dexec.mainClass=com.evaitcs.advanced.generics.WildcardsPecsDemo"
 */
public class GenericsDemos {

    public static void main(String[] args) throws Exception {
        section("ErasureProofDemo — List<String> and List<Integer> are ONE class; the cast is real");
        ErasureProofDemo.main(args);

        section("WildcardsPecsDemo — invariance, then PECS: producer extends, consumer super");
        WildcardsPecsDemo.main(args);

        section("BridgeMethodsDemo — the compiler-generated adapter that proves erasure happened");
        BridgeMethodsDemo.main(args);

        section("GenericTrapsDemo — the five forbidden moves, each with its workaround");
        GenericTrapsDemo.main(args);

        section("RecordsSealedGenericsDemo — same type system, Java 21 ceremony");
        RecordsSealedGenericsDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
