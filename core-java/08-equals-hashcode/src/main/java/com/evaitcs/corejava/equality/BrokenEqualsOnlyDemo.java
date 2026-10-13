package com.evaitcs.corejava.equality;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Demo 5 — the payoff: equals WITHOUT hashCode loses your data. Loudly.
 *
 * {@link BrokenAccountKey} has a perfect equals and the inherited (identity) hashCode.
 * We store a payroll under one key, then try to read it back with an equal-content key.
 * HashMap hashes the lookup key, lands in a DIFFERENT bucket than the stored key, walks
 * that (empty) bucket, finds nothing, and returns null — equals is never even called.
 *
 * This is deterministic, not flaky: default Object.hashCode is identity-based, so two
 * distinct objects virtually never collide into the same bucket. The test asserts the
 * null. "The map lost your money" is the line to say out loud.
 */
public class BrokenEqualsOnlyDemo {

    public static void main(String[] args) {

        banner("1. equals says they match... but watch the Map");

        BrokenAccountKey stored = new BrokenAccountKey("BR-01", "1001");
        BrokenAccountKey lookup = new BrokenAccountKey("BR-01", "1001");

        System.out.println("stored.equals(lookup)        : " + stored.equals(lookup)
                + "   <- equals is CORRECT, they are the same account");
        System.out.println("stored.hashCode()            : " + stored.hashCode());
        System.out.println("lookup.hashCode()            : " + lookup.hashCode());
        System.out.println("hashCodes equal?             : " + (stored.hashCode() == lookup.hashCode())
                + "  <- identity hash => almost always DIFFERENT");

        banner("2. put with one key, get with an equal key -> NULL");

        Map<BrokenAccountKey, Transaction> balances = new HashMap<BrokenAccountKey, Transaction>();
        balances.put(stored, Transaction.payroll());

        Transaction found = balances.get(lookup);
        System.out.println("balances.put(stored, payroll)");
        System.out.println("balances.get(lookup)         : " + found);
        System.out.println("balances.containsKey(lookup) : " + balances.containsKey(lookup));
        if (found == null) {
            System.out.println(">>> THE MAP LOST YOUR MONEY. 2450.00 is in there, and get() returns null. <<<");
        }

        banner("3. Same bug in a Set — 'duplicates' sail straight through");

        Set<BrokenAccountKey> seen = new HashSet<BrokenAccountKey>();
        seen.add(new BrokenAccountKey("BR-01", "1001"));
        seen.add(new BrokenAccountKey("BR-01", "1001"));   // equal to the first!
        seen.add(new BrokenAccountKey("BR-01", "1001"));   // ...and again
        System.out.println("added the SAME account three times");
        System.out.println("set.size()                   : " + seen.size()
                + "   <- should be 1; a Set that cannot dedupe is not a Set");

        banner("4. The one-line diagnosis");
        System.out.println("hash picks the BUCKET; equals searches WITHIN it. Wrong hash => wrong bucket");
        System.out.println("=> equals never runs => 'not found'. The fix is HashCodeFixDemo, next.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
