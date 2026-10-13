package com.evaitcs.corejava.equality;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Demo 6 — the fix, and the hashCode rules that make it correct.
 *
 * {@link AccountKey} overrides hashCode from the SAME fields as equals (via Objects.hash),
 * so the exact scenario that lost money in BrokenEqualsOnlyDemo now works: equal keys
 * hash to the same bucket, equals runs inside it, and get() finds the payroll.
 *
 * The three hashCode rules, all shown below:
 *   R1 (MANDATORY) equal objects => equal hashCodes.       [proven by the round-trip]
 *   R2 (allowed)   unequal objects MAY share a hashCode — a COLLISION. That is fine:
 *                  the bucket is a short chain and equals walks it to tell them apart.
 *   R3             hashCode must be CONSISTENT across calls during one execution
 *                  (so do not build it from mutable state — see MutableKeyTrapDemo).
 *
 * Also: Objects.hash(a, b) is shorthand for the classic 31*result+field loop, printed
 * side by side so students recognize what the IDE generates.
 */
public class HashCodeFixDemo {

    public static void main(String[] args) {

        banner("1. Same scenario as the broken demo — now it works");

        Map<AccountKey, Transaction> balances = new HashMap<AccountKey, Transaction>();
        balances.put(new AccountKey("BR-01", "1001"), Transaction.payroll());

        AccountKey equalButDistinct = new AccountKey("BR-01", "1001");
        Transaction found = balances.get(equalButDistinct);
        System.out.println("get(new equal-but-distinct key) : " + found + "   <- the money is BACK");
        System.out.println("containsKey(equalButDistinct)   : " + balances.containsKey(equalButDistinct));

        Set<AccountKey> seen = new HashSet<AccountKey>();
        seen.add(new AccountKey("BR-01", "1001"));
        seen.add(new AccountKey("BR-01", "1001"));
        System.out.println("Set after adding the SAME account twice -> size " + seen.size()
                + "   <- dedupes correctly now");

        banner("2. R1 — equal objects have equal hashCodes (the mandatory rule)");

        AccountKey a = new AccountKey("BR-01", "1001");
        AccountKey b = new AccountKey("BR-01", "1001");
        System.out.println("a.equals(b)        : " + a.equals(b));
        System.out.println("a.hashCode()       : " + a.hashCode());
        System.out.println("b.hashCode()       : " + b.hashCode());
        System.out.println("=> equal? " + a.equals(b) + ", hashes equal? " + (a.hashCode() == b.hashCode()));

        banner("3. Objects.hash IS the classic 31*result+field loop");

        // The manual, pre-Java-7 idiom — this is EXACTLY what Objects.hash(a, b) computes.
        int manual = 1;
        manual = 31 * manual + (a.getBranchCode() == null ? 0 : a.getBranchCode().hashCode());
        manual = 31 * manual + (a.getAccountNumber() == null ? 0 : a.getAccountNumber().hashCode());
        System.out.println("manual 31*result+field : " + manual);
        System.out.println("Objects.hash(same flds): " + Objects.hash(a.getBranchCode(), a.getAccountNumber()));
        System.out.println("a.hashCode()           : " + a.hashCode());
        System.out.println("=> all three identical. 31 = an odd prime; 31*i is (i<<5)-i, a cheap shift-subtract.");
        System.out.println("   Real world: let the IDE generate it, then READ it — you own the fields it picked.");

        banner("4. R2 — collisions are fine: unequal keys CAN share a hash");

        // "Aa" and "BB" are the textbook collision: identical String hashCodes, unequal strings.
        String aa = "Aa";
        String bb = "BB";
        System.out.println("\"Aa\".hashCode() = " + aa.hashCode() + ", \"BB\".hashCode() = " + bb.hashCode()
                + ", equal strings? " + aa.equals(bb));
        Map<String, String> colliding = new HashMap<String, String>();
        colliding.put(aa, "first");
        colliding.put(bb, "second");
        System.out.println("both stored: size " + colliding.size()
                + ", get(\"Aa\")=" + colliding.get(aa) + ", get(\"BB\")=" + colliding.get(bb));
        System.out.println("=> Same bucket, and equals walked the chain to keep them apart. Collision handled.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
