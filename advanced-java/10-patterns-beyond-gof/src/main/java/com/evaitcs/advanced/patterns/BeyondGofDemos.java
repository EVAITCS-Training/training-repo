package com.evaitcs.advanced.patterns;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 10-patterns-beyond-gof compile exec:java
 *
 * The order is the argument: first the MAP of all four families, then family A
 * (architectural) bottom-up — LayeredBankDemo shows wiring by hand, so
 * MiniContainerDemo's automatic wiring has something to replace; then the Value
 * Object BEFORE the concurrency scene, because that scene transfers immutable
 * Money between confined accounts; then the API idioms; and finally the
 * anti-patterns, so the module ends on judgment — which is what "beyond GoF"
 * really means: not more recipes, but knowing which shape to refuse.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 10-patterns-beyond-gof compile exec:java "-Dexec.mainClass=com.evaitcs.advanced.patterns.MiniContainerDemo"
 */
public class BeyondGofDemos {

    public static void main(String[] args) throws Exception {
        section("ArchitecturalMapDemo — the four families beyond GoF, on one screen");
        ArchitecturalMapDemo.main(args);

        section("LayeredBankDemo — controller -> service -> repository by hand; entities die at the boundary");
        LayeredBankDemo.main(args);

        section("MiniContainerDemo — Dependency Injection: Spring's @Autowired rebuilt in ~60 lines");
        MiniContainerDemo.main(args);

        section("MoneyValueObjectDemo — the Value Object: compact-constructor bouncer, value equality, immutability");
        MoneyValueObjectDemo.main(args);

        section("ConcurrencyPatternMapDemo — the seven concurrency shapes, and a lock-free confined transfer");
        ConcurrencyPatternMapDemo.main(args);

        section("IdiomsDemo — fluent chains, static factories, composition, Demeter, Null Object vs Optional");
        IdiomsDemo.main(args);

        section("AntiPatternsDemo — anemic vs rich: the same overdraft, one corrupts and one refuses");
        AntiPatternsDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
