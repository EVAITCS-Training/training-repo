package com.evaitcs.collections.sets;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Demo 1 — THE CONTRACT: "no duplicates", and add() TELLS you when it refused one.
 *
 * A Set's add returns a boolean — the only Collection that answers "did that change
 * anything?". false means: an element that is ALREADY inside (per this implementation's
 * notion of duplicate) rejected the newcomer, and — this surprises everyone — the
 * ORIGINAL stays. A Set never overwrites like a Map does; it simply refuses.
 *
 * And "duplicate" means EQUALS, not ==: the stranger with the same id is rejected even
 * though it is a different object with different everything-else. That is core-java 08's
 * contract doing real work — the harvest this whole module exists for.
 */
public class NoDuplicatesDemo {

    /** Deliberately WITHOUT equals/hashCode — the control group for banner 4. */
    static class RawTransaction {
        private final String id;

        RawTransaction(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "RawTransaction(" + id + ")";
        }
    }

    public static void main(String[] args) {
        banner("1. add() returns a boolean — the Set's whole promise in one value");

        Set<Transaction> statement = new LinkedHashSet<Transaction>(Transaction.sampleStatement());
        System.out.println("after loading the statement: size = " + statement.size());

        Transaction impostor = new Transaction("T-1040", "NOT Payroll", new BigDecimal("0.01"), 1_700_000_999_000L);
        System.out.println("add(stranger with same id)  returned " + statement.add(impostor) + " — REFUSED.");
        System.out.println("size is still " + statement.size() + ", and the ORIGINAL survived:");
        for (Transaction t : statement) {
            if (t.getId().equals("T-1040")) {
                System.out.println("  " + t + "   <- a Set refuses; it never overwrites (a Map would — module 05)");
            }
        }

        banner("2. 'Duplicate' means equals(), not ==");

        Transaction original = Transaction.sampleStatement().get(0);
        Transaction stranger = new Transaction(original.getId(), "Copycat", new BigDecimal("0.00"), 0L);
        System.out.println("original == stranger      ? " + (original == stranger) + "    <- different objects...");
        System.out.println("original.equals(stranger) ? " + original.equals(stranger) + "     <- ...but equal by id, so a duplicate.");
        System.out.println("HashSet/LinkedHashSet ask hashCode() first, then equals() inside the bucket — core-java 08, cashed in.");

        banner("3. The dedupe idiom — List in, Set out");

        List<String> flagged = new ArrayList<String>();
        flagged.add("ACC-1005");
        flagged.add("ACC-1027");
        flagged.add("ACC-1005");
        flagged.add("ACC-1005");
        flagged.add("ACC-1011");
        System.out.println("the List : " + flagged + "   <- duplicates kept, order kept");
        Set<String> unique = new LinkedHashSet<String>(flagged);
        System.out.println("the Set  : " + unique + "   <- duplicates gone, insertion order kept");
        System.out.println("new LinkedHashSet<String>(list) is THE one-line dedupe that preserves order.");
        System.out.println("(A HashSet would dedupe too but scramble the order; stream distinct() is java8-features 04.)");

        banner("4. Control group — no equals override, no deduplication, ever");

        Set<RawTransaction> raw = new LinkedHashSet<RawTransaction>();
        System.out.println("add #1 returned " + raw.add(new RawTransaction("T-1040")));
        System.out.println("add #2 returned " + raw.add(new RawTransaction("T-1040")) + " — same data, ACCEPTED.");
        System.out.println("size = " + raw.size() + "   <- Object.equals is ==, so nothing is ever a 'duplicate'.");
        System.out.println("Same lesson shape as RawAccount in module 01: no contract, no Set behavior.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
