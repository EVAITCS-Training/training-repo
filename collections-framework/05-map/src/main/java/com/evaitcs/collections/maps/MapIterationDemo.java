package com.evaitcs.collections.maps;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Demo 5 — ITERATION IDIOMS: entrySet (one lookup) vs keySet + get (double lookup).
 *
 * A Map is NOT Iterable — you cannot write for (Map.Entry<...> e : map). It hands out
 * three Collection VIEWS instead (module 01's dashed arrow, cashed in):
 *   keySet()   — the keys, as a Set
 *   values()   — the values, as a Collection (not a Set: values may repeat!)
 *   entrySet() — the pairs, as a Set of Map.Entry
 *
 * All three are LIVE views over the same entries: iterator.remove() on any of them
 * deletes the real entry (module 03's only-safe-remove rule applies verbatim), and
 * view.remove(x) writes through too.
 *
 * The performance sentence that decides the idiom: keySet + get does the bucket walk
 * TWICE per pair — once for the iterator, once for the get. entrySet hands you the
 * entry the iterator is already standing on. Same output, half the hashing.
 */
public class MapIterationDemo {

    public static void main(String[] args) {
        Map<String, BigDecimal> balances = new HashMap<String, BigDecimal>();
        for (Account a : Account.sampleAccounts()) {
            balances.put(a.getId(), a.getBalance());
        }
        // HashMap iteration order depends on hashes, not insertion — the two loops below
        // print in whatever order the buckets give; we compare CONTENT, never order.

        banner("1. entrySet — the canonical loop: ONE bucket walk per pair");

        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
        System.out.println("The iterator is ALREADY standing on the entry: getKey/getValue cost nothing extra.");

        banner("2. keySet + get — same output, DOUBLE the lookup per pair");

        for (String id : balances.keySet()) {
            System.out.println(id + " -> " + balances.get(id));
        }
        System.out.println("Each get(id) re-hashes the key and re-walks the bucket the iterator just visited.");
        System.out.println("Works, prints the same thing — and pays twice. entrySet is the idiom; this is the anti-idiom.");

        banner("3. values() — when the keys are noise");

        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (BigDecimal balance : balances.values()) {
            total = total.add(balance);
        }
        System.out.println("sum of all balances = " + total.toPlainString() + "   <- the hand-rolled reduce loop...");
        System.out.println("...that java8-features 04 rewrites as balances.values().stream().reduce(...). Forward-ref only.");
        System.out.println("values() is a Collection, NOT a Set — two accounts may share a balance, and both appear.");

        banner("4. Removing during iteration — module 03's rule, wearing a Map");

        Iterator<Map.Entry<String, BigDecimal>> it = balances.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, BigDecimal> entry = it.next();
            if (entry.getValue().compareTo(new BigDecimal("1000.00")) < 0) {
                System.out.println("removing " + entry.getKey() + " (" + entry.getValue().toPlainString() + ") — under 1000.00");
                it.remove();      // the ONLY safe remove during iteration — anything else is a CME (module 03/09)
            }
        }
        System.out.println("survivors: " + balances.keySet() + "   <- the view deleted REAL entries: size = " + balances.size());
        System.out.println("balances.keySet().remove(id) writes through the same way — views are live windows, not copies.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
