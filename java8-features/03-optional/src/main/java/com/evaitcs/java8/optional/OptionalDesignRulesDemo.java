package com.evaitcs.java8.optional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * WHERE Optional belongs — and the places it must never go. These are Brian Goetz's
 * rules from the JSR-335 design discussions, and they are the "design guidance" act of
 * the module. (The serialization details are in the README; the punchline: Optional
 * does not implement Serializable, so it must never touch persistent state.)
 *
 *   GOOD: return type of a LOOKUP method where absence is a legal outcome.
 *   BAD #1: FIELDS              — not Serializable, extra wrapper object per instance.
 *   BAD #2: METHOD PARAMETERS   — forces every caller to wrap; overload soup.
 *   BAD #3: Optional<Collection> — an EMPTY collection already means "none found".
 *
 * This class doubles as the module's before/after slide: the same lookup written the
 * pre-Java-8 way and the Java 8 way, with the caller code side by side.
 */
public class OptionalDesignRulesDemo {

    public static void main(String[] args) {
        LegacyAccountRepository legacy = new LegacyAccountRepository();
        AccountRepository repo = new AccountRepository();

        // ---------- BEFORE / AFTER: same question, two worlds ----------
        String id = "ACC-9999"; // deliberately missing — the interesting case

        // PRE-JAVA-8: the CALLER owns the null check. Four lines, and safety only holds
        // if every caller in the codebase remembers to write them.
        Account raw = legacy.findById(id);
        BigDecimal before = BigDecimal.ZERO;
        if (raw != null) {
            before = raw.getBalance();
        }

        // JAVA 8: the CHAIN owns it. Read it aloud: "find it, map to its balance, else zero."
        BigDecimal after = repo.findById(id)
                .map(Account::getBalance)
                .orElse(BigDecimal.ZERO);

        System.out.println("BEFORE (null + if-wall) : balance of " + id + " = " + before.toPlainString());
        System.out.println("AFTER  (Optional chain) : balance of " + id + " = " + after.toPlainString());
        System.out.println("Same answer — but the AFTER version cannot FORGET the check: getting the value");
        System.out.println("out of an Optional REQUIRES stating what happens when it is absent. That is the win.");
        System.out.println();

        // ---------- BAD #1: Optional as a FIELD ----------
        // class WrongCustomer { private Optional<String> email; }
        //   -> Optional is NOT Serializable: the whole class breaks under Java serialization,
        //      JPA providers refuse to map it to a column, Jackson needs an extra module.
        //   -> every read becomes customer.getEmail().get() — the null check, back again.
        // Look at THIS module's Customer: the field is a plain String; only the lookup
        // BOUNDARY wraps it. That is the pattern to copy.
        Customer bob = Customer.sampleCustomers().get(1);
        System.out.println("Customer.email is a plain String field; wrapping happens at the boundary:");
        System.out.println("  Optional.ofNullable(bob.getEmail()) = " + Optional.ofNullable(bob.getEmail()));
        System.out.println();

        // ---------- BAD #2: Optional as a PARAMETER ----------
        // findById(Optional<String> id) makes every caller write Optional.of("ACC-1001") —
        // ceremony at a hundred call sites to save one if in the method. And what would
        // findById(Optional.empty()) even MEAN? The ambiguity of act one, round two.
        System.out.println("Parameters stay plain: findById(\"ACC-1001\") — the CALLER decides nothing;");
        System.out.println("only the RETURN type gets to announce that the answer may be absent.");
        System.out.println();

        // ---------- BAD #3: Optional<List<T>> ----------
        // An empty list is already a perfect "nothing found". Wrapping it forces callers
        // to unwrap a box just to discover it contains an empty box.
        List<String> aliceRows = repo.recentStatementsFor("ACC-1001");
        List<String> nobodyRows = repo.recentStatementsFor("ACC-9999");
        System.out.println("recentStatementsFor(\"ACC-1001\") = " + aliceRows);
        System.out.println("recentStatementsFor(\"ACC-9999\") = " + nobodyRows
                + "   <- EMPTY LIST, not Optional.empty(), not null");
        int printed = 0;
        for (String row : nobodyRows) { // zero iterations, zero unwrapping, zero if-checks
            System.out.println("  " + row);
            printed++;
        }
        System.out.println("caller loop over the empty list ran " + printed + " time(s) — that is the whole point.");
    }
}
