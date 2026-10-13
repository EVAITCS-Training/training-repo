package com.evaitcs.collections.sets;

import java.util.HashSet;
import java.util.Set;

/**
 * Demo 6 — THE CORRUPTED SET: mutate an element's identity fields AFTER inserting it.
 *
 * The element is still in the Set — size proves it, iteration hands it back — but NO
 * lookup can find it anymore, not even the object itself:
 *
 *   - it PHYSICALLY sits in the bucket chosen from its OLD hashCode;
 *   - every query for the NEW id hashes to a DIFFERENT bucket and finds nothing;
 *   - every query for the OLD id lands in the right bucket, walks the chain, and the
 *     mutated element answers equals() with "no, I'm K-2 now".
 *
 * In there, unreachable. The Set is not empty and not correct — it is CORRUPTED.
 *
 * This is the direct consequence of the hashCode contract (core-java 08): equal objects
 * must hash equal — and a hash structure additionally assumes the hash NEVER CHANGES
 * while the object lives inside it. That is why String and Integer are the perfect
 * citizens of every hash structure: immutable. Module 05 replays this exact tragedy on
 * a HashMap — the mutable-key trap is the same bug wearing a different collection.
 */
public class CorruptedSetDemo {

    /**
     * A key whose identity field is SETTABLE — the one design decision this demo is about.
     * Its equals/hashCode are CORRECT (by id, consistent — straight out of core-java 08);
     * the bug is not the contract, it is mutating the field while the object sits in a bucket.
     */
    static class Key {
        private String id;

        Key(String id) {
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
            if (!(other instanceof Key)) {
                return false;
            }
            Key that = (Key) other;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return "Key(" + id + ")";
        }
    }

    public static void main(String[] args) {
        banner("1. Insert — the contract honored, lookup works");

        Set<Key> keys = new HashSet<Key>();
        Key key = new Key("K-1");
        keys.add(key);
        System.out.println("add(key K-1)                -> size = " + keys.size());
        System.out.println("contains(new Key(\"K-1\"))   -> " + keys.contains(new Key("K-1")) + "  <- found via hashCode then equals, textbook.");

        banner("2. Mutate the identity field — the Set corrupts SILENTLY");

        key.setId("K-2");
        System.out.println("After key.setId(\"K-2\"):");
        System.out.println("contains(new Key(\"K-1\"))   -> " + keys.contains(new Key("K-1"))
                + " <- right bucket, wrong answer: the element's equals now says 'I am K-2'.");
        System.out.println("contains(new Key(\"K-2\"))   -> " + keys.contains(new Key("K-2"))
                + " <- wrong bucket entirely: the element sits where K-1 hashed to.");
        System.out.println("contains(key) itself       -> " + keys.contains(key)
                + " <- EVEN ITSELF is unfound: it hashes as K-2, it lives in K-1's bucket.");
        System.out.println("size                       -> " + keys.size()
                + "  <- the element never left. In the Set, unreachable.");

        banner("3. Proof it is still in there — iteration sees what lookup cannot");

        for (Key k : keys) {
            System.out.println("the iterator hands back: " + k + " — physically present, logically lost.");
        }
        System.out.println("remove(new Key(\"K-2\")) -> " + keys.remove(new Key("K-2")) + " <- you cannot even delete it by its new name.");

        banner("4. The law");

        System.out.println("An object's hashCode must not change while it lives in a hash structure.");
        System.out.println("Make identity fields FINAL — that is the whole prevention, and it is why String wins.");
        System.out.println("Module 05 (MutableKeyTrapDemo) is this same trap on a HashMap: get() returns null for a key");
        System.out.println("that is demonstrably still inside the map.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
