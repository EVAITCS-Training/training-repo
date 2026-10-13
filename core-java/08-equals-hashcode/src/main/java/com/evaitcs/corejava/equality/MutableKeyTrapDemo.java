package com.evaitcs.corejava.equality;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Demo 7 — the MUTABLE KEY trap: change a key after it is in the map, and it vanishes.
 *
 * equals and hashCode here are CORRECT. The bug is not in the methods — it is in mutating
 * a field that the map already hashed on. HashMap computes the bucket ONCE, at put() time.
 * Mutate the key and its hashCode changes, so it now belongs in a DIFFERENT bucket — but
 * the map still has it filed under the OLD one. get() hashes the (changed) key, looks in
 * the NEW bucket, finds nothing. "The key moved buckets while it was inside the map."
 *
 * THE RULE: keys must be IMMUTABLE. This is why AccountKey is final with final fields, and
 * why String is the canonical key (module 03: String even CACHES its hashCode after the
 * first call — safe precisely because its contents can never change).
 */
public class MutableKeyTrapDemo {

    /** A key with a setter — equals/hashCode are correct, but the field can change. That is the flaw. */
    static class MutableAccountKey {
        private String branchCode;                 // NOT final — the whole problem
        private final String accountNumber;

        MutableAccountKey(String branchCode, String accountNumber) {
            this.branchCode = branchCode;
            this.accountNumber = accountNumber;
        }

        void setBranchCode(String branchCode) {    // a setter on a key is a loaded gun
            this.branchCode = branchCode;
        }

        String getBranchCode() {
            return branchCode;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            MutableAccountKey that = (MutableAccountKey) other;
            return Objects.equals(branchCode, that.branchCode)
                    && Objects.equals(accountNumber, that.accountNumber);
        }

        @Override
        public int hashCode() {
            return Objects.hash(branchCode, accountNumber);   // correct... until branchCode mutates
        }

        @Override
        public String toString() {
            return branchCode + "/" + accountNumber;
        }
    }

    public static void main(String[] args) {

        banner("put a key, then MUTATE it, then try to get it back");

        MutableAccountKey key = new MutableAccountKey("BR-01", "1001");
        Map<MutableAccountKey, Transaction> balances = new HashMap<MutableAccountKey, Transaction>();
        balances.put(key, Transaction.payroll());

        System.out.println("before mutation:");
        System.out.println("  key                 = " + key);
        System.out.println("  key.hashCode()      = " + key.hashCode());
        System.out.println("  balances.get(key)   = " + balances.get(key) + "   <- found");

        // The same OBJECT the map holds — we just changed a field it hashed on.
        key.setBranchCode("BR-02");

        System.out.println("after key.setBranchCode(\"BR-02\"):");
        System.out.println("  key                 = " + key);
        System.out.println("  key.hashCode()      = " + key.hashCode() + "   <- the hash CHANGED");
        System.out.println("  balances.get(key)   = " + balances.get(key)
                + "   <- LOST: map looks in the NEW bucket, entry is filed in the OLD one");
        System.out.println("  map size            = " + balances.size()
                + "   <- the entry is STILL in there, just unreachable");

        banner("The rule");
        System.out.println("The key moved buckets while it was inside the map.");
        System.out.println("=> Never mutate a field used in hashCode. Make keys IMMUTABLE (final fields, no setters).");
        System.out.println("   AccountKey is final/final for exactly this reason; String caches its hash (module 03).");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
