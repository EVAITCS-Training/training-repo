package com.evaitcs.collections.concurrent;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo 3 — the null policy: HashMap shrugs, ConcurrentHashMap refuses. WHY: ambiguity.
 *
 * Single-threaded, `map.get(k) == null` has an escape hatch: you ask `containsKey(k)` and
 * learn whether the null meant "absent" or "present, holding null". Under concurrency the
 * escape hatch slams shut — the map can change BETWEEN the two calls, and no ordering of
 * two separate calls can ever produce one answer. containsKey-then-get is a check-then-act
 * compound: module 09's broken sequence wearing a different hat. Java's choice for the
 * concurrent maps is therefore not a limitation but a PROOF-THEOREM: ban nulls outright,
 * and `get() == null` means exactly one thing forever — ABSENT.
 *
 * DETERMINISM: banner 3 does not race threads; it PLAYS the ambiguity as a scripted
 * interleaving on one thread (module 09's technique), with real calls whose return values
 * are printed — the two answers A collects are individually correct and jointly impossible.
 */
public class NullPolicyDemo {

    public static void main(String[] args) {
        banner("1. HashMap — null key AND null value both legal (module 05's permissive engine)");

        Map<String, Account> plain = new HashMap<String, Account>();
        Account dana = new Account("ACC-1027", "Dana", new BigDecimal("2450.00"));
        plain.put(null, dana);                     // a null KEY — special-cased into bucket 0 (module 05)
        plain.put("ACC-1005", null);               // a null VALUE
        System.out.println("get(null)     -> " + plain.get(null) + "   <- the null key is a legal key");
        System.out.println("get(ACC-1005) -> " + plain.get("ACC-1005") + "   <- a null value is a legal value");
        System.out.println("get(ACC-9999) -> " + plain.get("ACC-9999") + "   <- and so is absence. Same answer, THREE meanings.");
        System.out.println("Single-threaded, you can ALWAYS disambiguate with a second question:");
        System.out.println("  containsKey(ACC-1005) -> " + plain.containsKey("ACC-1005") + "  => that null was a VALUE");
        System.out.println("  containsKey(ACC-9999) -> " + plain.containsKey("ACC-9999") + " => that null meant ABSENT");
        System.out.println("Hold that 'second question' thought. Banner 3 kills it.");

        banner("2. ConcurrentHashMap — both refused, at the door, with an NPE");

        ConcurrentHashMap<String, Account> concurrent = new ConcurrentHashMap<String, Account>();
        try {
            concurrent.put(null, dana);
        } catch (NullPointerException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " on a NULL KEY");
        }
        try {
            concurrent.put("ACC-1027", null);
        } catch (NullPointerException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " on a NULL VALUE");
        }
        System.out.println("Both refused, every run, before anything is stored. Not a missing feature —");
        System.out.println("a design decision, and the next banner is the reason.");

        banner("3. The ambiguity, played as an interleaving (module 09's scripting technique)");

        // A map that ALLOWS null values, and a question with no stable answer:
        // "does ACC-1027 exist, and if so, what collateral does it hold?"
        Map<String, String> collateral = new HashMap<String, String>();
        collateral.put("ACC-1027", null);                          // account exists, NO collateral on file
        System.out.println("setup: collateral.put(ACC-1027, null) — the account exists, its collateral is null");

        boolean exists = collateral.containsKey("ACC-1027");       // thread A's CHECK...
        System.out.println("A: containsKey(ACC-1027) -> " + exists + "   ('the account is there')");

        collateral.remove("ACC-1027");                             // *** thread B closes the account IN THE GAP ***
        System.out.println("B: remove(ACC-1027)            (account closed a nanosecond after A's check)");

        String value = collateral.get("ACC-1027");                 // ...thread A's ACT
        System.out.println("A: get(ACC-1027)         -> " + value + "   ('...so its collateral must be null')");

        System.out.println();
        System.out.println("A's two answers — exists:true and value:null — are INDIVIDUALLY correct and");
        System.out.println("JOINTLY IMPOSSIBLE: they were read from two different moments. A concludes");
        System.out.println("'the account exists and holds null'; the truth is 'the account is GONE'.");
        System.out.println("containsKey-then-get is a check-then-act compound — module 09's lost update in a");
        System.out.println("read-only costume. No retry loop fixes it: the pair itself is the race.");
        System.out.println("Ban nulls and the ambiguity DIES: get() == null can then mean exactly one thing.");

        banner("4. The same law, everywhere in java.util.concurrent");

        System.out.println("ConcurrentSkipListMap : no nulls — same ambiguity, plus a null key cannot be compared");
        System.out.println("BlockingQueue family  : no null elements — poll() ALREADY returns null to mean 'empty',");
        System.out.println("                        so a null element would be indistinguishable from the end of work");
        System.out.println("ConcurrentLinkedQueue : no nulls — same one-meaning rule");
        System.out.println("CopyOnWriteArrayList  : nulls ALLOWED — it is a positional List like any other; index i");
        System.out.println("                        either holds something or the list is shorter. No ambiguity.");
        System.out.println("And if your business genuinely means 'maybe no value', the honest package for it is");
        System.out.println("Optional — java8-features/03-optional. A null in a map is a question with three answers;");
        System.out.println("an Optional is a question with one.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
