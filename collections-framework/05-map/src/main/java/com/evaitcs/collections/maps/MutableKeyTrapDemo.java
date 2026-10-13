package com.evaitcs.collections.maps;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo 3 — THE MUTABLE-KEY TRAP: the classic "my map lost the key" bug, staged as a crime scene.
 *
 * Module 04's CorruptedSetDemo, replayed on a HashMap — because it is the SAME machine:
 * put() hashes the key ONCE and files the entry in that bucket. Mutate the key's identity
 * field afterwards and the filing never changes, but every lookup now hashes to a
 * DIFFERENT bucket:
 *
 *   get(mutatedKey)        -> null    (searches the NEW bucket — empty)
 *   containsKey(mutatedKey)-> false   (same wrong bucket)
 *   size()                 -> unchanged (the entry never left!)
 *   entrySet() iteration   -> still hands the entry back (it walks buckets, it does not hash)
 *
 * The entry is IN the map and UNREACHABLE. You cannot get it, you cannot remove it by
 * key — you can only stumble over it while iterating. This is why hash-map keys must be
 * immutable: String, Integer, or your own class with FINAL identity fields.
 */
public class MutableKeyTrapDemo {

    /**
     * A key whose identity field is SETTABLE — the one design decision this demo is about.
     * equals/hashCode are correct (by id, consistent — straight out of core-java 08);
     * the bug is mutating the field while the entry sits in a bucket.
     */
    static class MutableKey {
        private String id;

        MutableKey(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }

        void setId(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof MutableKey)) {
                return false;
            }
            MutableKey that = (MutableKey) other;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return "MutableKey(" + id + ")";
        }
    }

    public static void main(String[] args) {
        banner("1. put — the contract honored, lookup works");

        Map<MutableKey, String> balances = new HashMap<MutableKey, String>();
        MutableKey key = new MutableKey("K-1");
        balances.put(key, "1200.00");
        System.out.println("put(K-1, \"1200.00\")      -> size = " + balances.size());
        System.out.println("get(key)                  -> " + balances.get(key));
        System.out.println("get(new MutableKey(\"K-1\")) -> " + balances.get(new MutableKey("K-1"))
                + "   <- an EQUAL stranger finds it: hashCode -> bucket -> equals. Textbook.");

        banner("2. Mutate the key — the map loses it SILENTLY");

        key.setId("K-2");
        System.out.println("After key.setId(\"K-2\"):");
        System.out.println("get(key)                  -> " + balances.get(key)
                + "     <- NULL. The very key object that was used to put!");
        System.out.println("containsKey(key)          -> " + balances.containsKey(key)
                + "  <- it hashes as K-2 now; the entry sits in K-1's bucket.");
        System.out.println("get(new MutableKey(\"K-1\")) -> " + balances.get(new MutableKey("K-1"))
                + "     <- the old name finds the right bucket, but the mutated key's equals says 'I am K-2'.");
        System.out.println("get(new MutableKey(\"K-2\")) -> " + balances.get(new MutableKey("K-2"))
                + "     <- the new name hashes to an empty bucket.");
        System.out.println("size()                    -> " + balances.size()
                + "        <- the entry NEVER LEFT. In the map, unreachable.");

        banner("3. Proof it is still in there — entrySet walks buckets, it does not hash");

        for (Map.Entry<MutableKey, String> entry : balances.entrySet()) {
            System.out.println("iteration hands back: " + entry.getKey() + " -> " + entry.getValue()
                    + "   <- physically present, logically lost");
        }
        System.out.println("remove(new MutableKey(\"K-2\")) -> " + balances.remove(new MutableKey("K-2"))
                + "  <- you cannot even delete it by its new name");
        System.out.println("size() is STILL " + balances.size() + ". The only classic-idiom escape: iterate and use iterator.remove() (module 03).");

        banner("4. The law");

        System.out.println("A key's hashCode must not change while the entry lives in the map.");
        System.out.println("Make identity fields FINAL — then the trap is not 'avoided', it is UNREPRESENTABLE.");
        System.out.println("That is why String and Integer are the perfect keys... and why module 10's");
        System.out.println("ConcurrentHashMap cannot save you here either: no map can find what it cannot hash.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
