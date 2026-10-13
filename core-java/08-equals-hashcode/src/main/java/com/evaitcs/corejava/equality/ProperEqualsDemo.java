package com.evaitcs.corejava.equality;

import java.math.BigDecimal;

/**
 * Demo 2 — writing equals CORRECTLY, line by line, then the two classic traps.
 *
 * {@link AccountKey} holds the real implementation; here we EXERCISE each of its five
 * lines so students see the guard clauses earn their keep, then hit:
 *   TRAP A — the OVERLOAD: `equals(MyType)` is NOT an override. It has a different
 *            signature, so collections (which call equals(Object)) never invoke it.
 *   TRAP B — BigDecimal: equals is scale-sensitive, compareTo is not. Decide which you mean.
 */
public class ProperEqualsDemo {

    /** TRAP A: an equals with the WRONG parameter type — a silent overload, never called by collections. */
    static class OverloadTrapKey {
        private final String accountNumber;

        OverloadTrapKey(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        // Looks like equals. It is NOT an override — the parameter is OverloadTrapKey,
        // not Object. `@Override` is absent on purpose; add it and the compiler SCREAMS.
        public boolean equals(OverloadTrapKey other) {
            return other != null && accountNumber.equals(other.accountNumber);
        }
    }

    public static void main(String[] args) {

        banner("1. Each line of AccountKey.equals, exercised");

        AccountKey k = new AccountKey("BR-01", "1001");

        // Line 1: identity shortcut -> reflexive.
        System.out.println("k.equals(k)              : " + k.equals(k) + "   <- line 1: this == other");
        // Line 2: null guard -> x.equals(null) is false, never an NPE.
        System.out.println("k.equals(null)           : " + k.equals(null) + "  <- line 2: null guard");
        // Line 3: type guard -> a different class is a category error, not a match.
        System.out.println("k.equals(\"BR-01/1001\")   : " + k.equals("BR-01/1001") + "  <- line 3: getClass() mismatch");
        // Lines 4-5: safe cast, then field-by-field via Objects.equals.
        AccountKey twin = new AccountKey("BR-01", "1001");
        AccountKey otherBranch = new AccountKey("BR-02", "1001");
        System.out.println("k.equals(twin)           : " + k.equals(twin) + "   <- lines 4-5: same content");
        System.out.println("k.equals(otherBranch)    : " + k.equals(otherBranch) + "  <- one field differs");

        // Null-safety of Objects.equals: neither side blows up even with a null field.
        AccountKey nullBranchA = new AccountKey(null, "1001");
        AccountKey nullBranchB = new AccountKey(null, "1001");
        System.out.println("null-branch a.equals(b)  : " + nullBranchA.equals(nullBranchB)
                + "   <- Objects.equals(null, null) is true, no NPE");

        banner("2. TRAP A — equals(MyType) is an OVERLOAD, not an override");

        OverloadTrapKey t1 = new OverloadTrapKey("1001");
        OverloadTrapKey t2 = new OverloadTrapKey("1001");

        // Called DIRECTLY with a statically-typed argument, the overload runs and lies "true":
        System.out.println("t1.equals(t2) [static type OverloadTrapKey] : " + t1.equals(t2)
                + "   <- the overload runs; looks fine!");
        // But the moment it is an Object (which is how EVERY collection calls it),
        // the inherited Object.equals (identity) runs instead:
        Object asObject = t2;
        System.out.println("t1.equals((Object) t2) [static type Object] : " + t1.equals(asObject)
                + "  <- Object.equals = identity; overload SKIPPED");

        java.util.Map<OverloadTrapKey, String> map = new java.util.HashMap<OverloadTrapKey, String>();
        map.put(t1, "payroll");
        // HashMap stores the key as Object and calls equals(Object). The overload is dead code.
        System.out.println("map.get(new equal key)   : " + map.get(new OverloadTrapKey("1001"))
                + "   <- lookup FAILS: collections call equals(Object), never the overload");
        System.out.println("=> Always annotate with @Override. If it will not compile, you overloaded.");

        banner("3. TRAP B — BigDecimal: equals vs compareTo (module 02 callback)");

        BigDecimal twoOh = new BigDecimal("2.0");
        BigDecimal twoOhOh = new BigDecimal("2.00");
        System.out.println("2.0.equals(2.00)          : " + twoOh.equals(twoOhOh)
                + "  <- scale differs (1 vs 2 decimal places)");
        System.out.println("2.0.compareTo(2.00) == 0  : " + (twoOh.compareTo(twoOhOh) == 0)
                + "   <- numerically equal");
        System.out.println("=> For money, compareTo==0 is usually what you MEAN. If you use it in equals,");
        System.out.println("   hashCode must ignore scale too — or you re-break the contract. Decide on purpose.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
