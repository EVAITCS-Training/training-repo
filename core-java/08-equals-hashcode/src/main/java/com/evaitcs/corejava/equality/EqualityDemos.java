package com.evaitcs.corejava.equality;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 08-equals-hashcode compile exec:java
 *
 * The order is the argument: start from the broken default, build a correct equals,
 * prove it against the five-part contract, show the symmetry trap, then reveal WHY
 * hashCode must come along (the map loses your money), fix it, and finish with the
 * two "still bites you in production" traps: mutable keys and compareTo consistency.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 08-equals-hashcode compile exec:java "-Dexec.mainClass=com.evaitcs.corejava.equality.BrokenEqualsOnlyDemo"
 */
public class EqualityDemos {

    public static void main(String[] args) {
        section("DefaultIdentityDemo — Object.equals IS ==; identity is the wrong question for values");
        DefaultIdentityDemo.main(args);

        section("ProperEqualsDemo — equals line by line, plus the overload and BigDecimal traps");
        ProperEqualsDemo.main(args);

        section("ContractWalkDemo — the five equals properties, printed true/false");
        ContractWalkDemo.main(args);

        section("SymmetryTrapDemo — instanceof across a hierarchy breaks symmetry");
        SymmetryTrapDemo.main(args);

        section("BrokenEqualsOnlyDemo — equals WITHOUT hashCode: the map loses your money");
        BrokenEqualsOnlyDemo.main(args);

        section("HashCodeFixDemo — the fix, and the three hashCode rules (incl. collisions)");
        HashCodeFixDemo.main(args);

        section("MutableKeyTrapDemo — mutate a key and it moves buckets inside the map");
        MutableKeyTrapDemo.main(args);

        section("CompareToConsistencyTeaserDemo — TreeSet uses compareTo, not equals (module 10)");
        CompareToConsistencyTeaserDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
