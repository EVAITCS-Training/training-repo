package com.evaitcs.collections.maps;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Demo 1 — THE CORE VOCABULARY: put/get/remove, and the return values that tell the truth.
 *
 * A Map is NOT a Collection (module 01 proved it with instanceof) — key-value pairs don't
 * fit "a bag of elements". But it HANDS OUT Collections: keySet(), values(), entrySet()
 * are live views of the very same entries, and they are the only way to iterate a Map.
 *
 * The four return values worth memorizing, all printed below:
 *   put(k,v)          -> the OLD value (null if the key was new) — put is REPLACE
 *   get(k)            -> the value, or null — and null is AMBIGUOUS (banner 5, the trap)
 *   remove(k)         -> the removed value, or null
 *   putIfAbsent(k,v)  -> the CURRENT value: non-null means "I did not store yours"
 *
 * getOrDefault and putIfAbsent are plain interface methods — fine in the classic idiom.
 * Their cousins computeIfAbsent/merge/compute package the whole get-check-put dance into
 * one call taking a function: that is java8-features 01 territory (lambdas), forward-ref only.
 */
public class MapBasicsDemo {

    public static void main(String[] args) {
        banner("1. A registry: account id -> Account — put, get, size");

        Map<String, Account> registry = new HashMap<String, Account>();
        for (Account a : Account.sampleAccounts()) {
            registry.put(a.getId(), a);
        }
        System.out.println("registry.size() = " + registry.size());
        System.out.println("registry.get(\"ACC-1005\") = " + registry.get("ACC-1005") + "   <- one hop: hash, bucket, done");
        System.out.println("registry.get(\"ACC-9999\") = " + registry.get("ACC-9999") + "        <- absent key: null (banner 5 explains why that is dangerous)");

        banner("2. put returns the OLD value — put is REPLACE, and the return is the proof");

        Account ana = registry.get("ACC-1005");
        Account corrected = new Account("ACC-1005", "Ana", new BigDecimal("90.00"));
        Account old = registry.put("ACC-1005", corrected);
        System.out.println("put(ACC-1005, balance 90.00) returned: " + old + "   <- the value it REPLACED");
        System.out.println("registry.get(\"ACC-1005\") is now     : " + registry.get("ACC-1005"));
        System.out.println("put on a NEW key returns null: " + registry.put("ACC-1099", new Account("ACC-1099", "Cid", new BigDecimal("10.00"))));
        System.out.println("size = " + registry.size() + " — unlike a Set (module 04), a Map OVERWRITES on a duplicate key.");

        banner("3. getOrDefault / putIfAbsent — the polite operations");

        Account placeholder = new Account("ACC-0000", "Unknown", BigDecimal.ZERO.setScale(2));
        System.out.println("getOrDefault(\"ACC-7777\", placeholder) = " + registry.getOrDefault("ACC-7777", placeholder) + "   <- absent: the default");
        System.out.println("getOrDefault(\"ACC-1011\", placeholder) = " + registry.getOrDefault("ACC-1011", placeholder) + "   <- present: the real value");

        Account intruder = new Account("ACC-1011", "Impostor Bo", new BigDecimal("0.01"));
        Account result = registry.putIfAbsent("ACC-1011", intruder);
        System.out.println("putIfAbsent(\"ACC-1011\", impostor) returned: " + result + "   <- non-null means: 'key was there, yours NOT stored'");
        System.out.println("registry.get(\"ACC-1011\") unchanged       : " + registry.get("ACC-1011"));
        System.out.println("putIfAbsent(\"ACC-2022\", new account) returned: "
                + registry.putIfAbsent("ACC-2022", new Account("ACC-2022", "Dee", new BigDecimal("7.77"))) + "   <- null means: stored");
        // The get-check-put dance above is exactly what computeIfAbsent(k, factory) and
        // merge(k, v, f) compress into one call — with a lambda. java8-features 01, forward-ref only.

        banner("4. containsKey is O(1); containsValue is an O(n) SCAN");

        System.out.println("containsKey(\"ACC-1027\")   = " + registry.containsKey("ACC-1027") + "  <- hashed straight to the bucket");
        System.out.println("containsValue(Dana-stranger) = "
                + registry.containsValue(new Account("ACC-1027", "Not Dana At All", new BigDecimal("0.00")))
                + "  <- found via equals() (by id)...");
        System.out.println("...but only after walking EVERY entry: values have no hash index. containsValue is O(n).");

        banner("5. The ambiguity — get() == null has TWO meanings");

        registry.put("ACC-3033", null);   // null VALUES are legal in a HashMap
        System.out.println("get(\"ACC-3033\") = " + registry.get("ACC-3033") + "  <- key PRESENT, value null");
        System.out.println("get(\"ACC-8888\") = " + registry.get("ACC-8888") + "  <- key ABSENT");
        System.out.println("Same answer, opposite meanings. containsKey tells them apart:");
        System.out.println("containsKey(\"ACC-3033\") = " + registry.containsKey("ACC-3033") + ", containsKey(\"ACC-8888\") = " + registry.containsKey("ACC-8888"));
        System.out.println("ConcurrentHashMap (module 10) simply BANS null keys and values — to kill this ambiguity in concurrent code.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
