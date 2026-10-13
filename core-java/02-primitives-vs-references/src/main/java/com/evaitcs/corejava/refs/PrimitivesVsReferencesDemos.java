package com.evaitcs.corejava.refs;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 02-primitives-vs-references compile exec:java
 *
 * To run a single demo while pointing at it on the projector:
 *   .\mvnw.cmd -pl 02-primitives-vs-references compile exec:java "-Dexec.mainClass=com.evaitcs.corejava.refs.PassByValueDemo"
 */
public class PrimitivesVsReferencesDemos {

    public static void main(String[] args) {
        section("PrimitiveTourDemo — the 8 primitives: size, range, defaults, literals, char");
        PrimitiveTourDemo.main(args);

        section("OverflowDemo — int wraps around silently (the classic bug)");
        OverflowDemo.main(args);

        section("MoneyDoubleVsBigDecimalDemo — why double loses cents and BigDecimal does not");
        MoneyDoubleVsBigDecimalDemo.main(args);

        section("ReferenceSemanticsDemo — a variable holds a POINTER; == vs equals; null/NPE");
        ReferenceSemanticsDemo.main(args);

        section("PassByValueDemo — Java is pass-by-value, always (three definitive cases)");
        PassByValueDemo.main(args);

        section("BoxingAndCacheDemo — wrappers, autoboxing sugar, and the Integer cache trap");
        BoxingAndCacheDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
