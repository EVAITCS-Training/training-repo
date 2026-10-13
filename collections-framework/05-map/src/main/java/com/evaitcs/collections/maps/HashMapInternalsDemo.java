package com.evaitcs.collections.maps;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo 2 — THE MACHINE ROOM: how HashMap actually finds your value.
 *
 * The whole story in four steps (the README's flowchart, as code):
 *   1. call key.hashCode()                       -> an int (core-java 08's contract)
 *   2. SPREAD it: h ^ (h >>> 16)                 -> mixes the high bits into the low ones
 *   3. bucket = (capacity - 1) & spread          -> capacity is a POWER OF TWO, so the
 *                                                   bitmask IS "mod capacity", but faster
 *   4. walk the tiny chain in that bucket,
 *      comparing candidates with key.equals()    -> collisions are NORMAL, equals settles them
 *
 * That is the entire contract: hashCode picks the drawer, equals picks the paper inside.
 * Everything else follows by logic — load factor 0.75 (when the drawers get crowded,
 * double the cabinet and rehash everything), initialCapacity (do the math ONCE, up
 * front), and Java 8's treeification (a chain longer than 8 becomes a small red-black
 * tree — module 04's TreeSet machinery, mentioned only; we never peek inside java.util
 * with reflection, modern JDKs forbid it and you never need it).
 *
 * The star witness: "Aa".hashCode() == "BB".hashCode() — two DIFFERENT strings, the SAME
 * bucket, and the map still works, because equals() has the last word.
 */
public class HashMapInternalsDemo {

    /** The default capacity — HashMap always allocates a power of two. */
    static final int CAPACITY = 16;

    public static void main(String[] args) {
        banner("1. The table, drawn: an array of buckets, each a tiny chain");

        System.out.println("HashMap keeps an array (the 'table'); capacity is ALWAYS a power of 2 (here 16).");
        System.out.println("Each slot is a bucket; entries that land together form a chain:");
        System.out.println();
        System.out.println("   table (capacity 16)");
        System.out.println("   [ 0] -> (\"Aa\", Dana) -> (\"BB\", Bo) -> null          <- two keys, ONE bucket (banner 2)");
        System.out.println("   [ 3] -> (ACC-1027, ...) -> (ACC-1005, ...) -> null  <- our own ids collide HERE (banner 3 computes it)");
        System.out.println("   [ 8] -> (ACC-1011, ...) -> null");
        System.out.println("   [..]  ... every other slot: null");
        System.out.println("   [15] -> null");
        System.out.println();
        System.out.println("get(key): hash the key -> pick the bucket -> walk the chain comparing with equals.");
        System.out.println("That is why ONE bad hashCode is survivable (chains) and NO equals override is fatal (core-java 08).");

        banner("2. The classic collision — \"Aa\".hashCode() == \"BB\".hashCode()");

        System.out.println("\"Aa\".hashCode() = " + "Aa".hashCode());
        System.out.println("\"BB\".hashCode() = " + "BB".hashCode() + "   <- EQUAL: 'A'*31+'a' == 'B'*31+'B' == 2112. Not a bug — a fact.");
        System.out.println("spread(h) = h ^ (h >>> 16) — for small hashes this is the identity:");
        System.out.println("spread(\"Aa\") = " + spread("Aa".hashCode()) + ", spread(\"BB\") = " + spread("BB".hashCode()));
        System.out.println("bucket = (capacity - 1) & spread = 15 & " + spread("Aa".hashCode()) + " = " + bucketOf("Aa")
                + " for BOTH keys — same drawer, every run, deterministic.");

        Map<String, String> collided = new HashMap<String, String>();
        collided.put("Aa", "Dana");
        collided.put("BB", "Bo");
        System.out.println("put both -> size = " + collided.size() + " (the second did NOT replace the first!)");
        System.out.println("get(\"Aa\") = " + collided.get("Aa") + ", get(\"BB\") = " + collided.get("BB"));
        System.out.println("Same bucket, different entries: hashCode found the DRAWER, equals picked the PAPER.");

        banner("3. The bucket math for our own ids — capacity 16");

        for (Account a : Account.sampleAccounts()) {
            String id = a.getId();
            System.out.println("key " + id + "   hashCode " + id.hashCode()
                    + " -> spread " + spread(id.hashCode())
                    + " -> bucket (15 & spread) = " + bucketOf(id));
        }
        System.out.println("String.hashCode is SPECIFIED (s[0]*31^(n-1) + ... + s[n-1]), so these numbers are the same on every JVM.");

        banner("4. Load factor, resize, and the initialCapacity hint");

        System.out.println("load factor 0.75: at capacity 16 the map resizes when the 13th entry arrives");
        System.out.println("(16 * 0.75 = 12 — more than 12 and it's time). Resize = allocate a table of 32 and");
        System.out.println("REHASH every entry into it. Amortized still O(1) — but why pay it if you know the size?");
        System.out.println();
        System.out.println("Expecting n entries? Size the map up front:  expected / 0.75 + 1, i.e.");
        int expected = 100;
        int hint = expected * 4 / 3 + 1;
        System.out.println("new HashMap<String, Account>(" + hint + ") for " + expected + " entries — zero resizes, ever.");
        System.out.println("(HashMap rounds the hint UP to the next power of two — you can't make it non-power-of-2.)");
        System.out.println();
        System.out.println("Java 8 footnote: if one bucket's chain grows past 8 entries (pathological hashes),");
        System.out.println("HashMap TREEIFIES it — the chain becomes a small red-black tree and the worst case");
        System.out.println("improves from O(n) to O(log n). Module 04's tree, inside module 05's map. Mentioned only.");
    }

    /** HashMap's spread function: fold the high 16 bits into the low ones so they matter to the mask. */
    static int spread(int h) {
        return h ^ (h >>> 16);
    }

    /** The bucket index for a key at capacity 16 — the same math HashMap does, computed in the open. */
    static int bucketOf(String key) {
        return (CAPACITY - 1) & spread(key.hashCode());
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
