package com.evaitcs.corejava.equality;

/**
 * Demo 3 — the FIVE-part equals contract, each property printed as a true/false.
 *
 * equals is not "whatever you want it to be". It is an EQUIVALENCE RELATION with five
 * rules, and the entire collections library silently assumes you kept them. Break one
 * and HashSet/HashMap start behaving like they are haunted. {@link AccountKey} obeys
 * all five; this demo walks them one at a time so the class can chant the list.
 *
 *   1. Reflexive  : x.equals(x)              -> true
 *   2. Symmetric  : x.equals(y) == y.equals(x)
 *   3. Transitive : x.equals(y) && y.equals(z) -> x.equals(z)
 *   4. Consistent : repeated calls agree (as long as nothing used in equals changes)
 *   5. Null       : x.equals(null)           -> false (never true, never throws)
 */
public class ContractWalkDemo {

    public static void main(String[] args) {

        AccountKey x = new AccountKey("BR-01", "1001");
        AccountKey y = new AccountKey("BR-01", "1001");   // equal content, distinct object
        AccountKey z = new AccountKey("BR-01", "1001");   // equal content, distinct object
        AccountKey w = new AccountKey("BR-09", "9999");   // different

        banner("The five properties, on the FIXED AccountKey");

        // 1. Reflexive — an object equals itself. Free, thanks to the `this == other` line.
        boolean reflexive = x.equals(x);
        System.out.println("1. reflexive   x.equals(x)                 = " + reflexive);

        // 2. Symmetric — order of comparison must not matter.
        boolean symmetric = x.equals(y) && y.equals(x);
        System.out.println("2. symmetric   x.equals(y)=" + x.equals(y) + ", y.equals(x)=" + y.equals(x)
                + "  => " + symmetric);

        // 3. Transitive — if x~y and y~z then x~z. (SymmetryTrapDemo shows instanceof
        //    hierarchies breaking exactly this and symmetry together.)
        boolean transitive = x.equals(y) && y.equals(z) && x.equals(z);
        System.out.println("3. transitive  x~y=" + x.equals(y) + ", y~z=" + y.equals(z)
                + ", x~z=" + x.equals(z) + "  => " + transitive);

        // 4. Consistent — call it many times; the answer must not flicker.
        boolean consistent = true;
        for (int i = 0; i < 5; i++) {
            if (!x.equals(y)) {
                consistent = false;
            }
        }
        System.out.println("4. consistent  x.equals(y) five times      = all " + consistent);

        // 5. Null — nothing equals null; the guard makes it false instead of an NPE.
        boolean nullIsFalse = !x.equals(null);
        System.out.println("5. null        x.equals(null)              = " + x.equals(null)
                + "  (must be false) => " + nullIsFalse);

        banner("And a NON-match, to prove equals is not just returning true");
        System.out.println("x.equals(w) [different account]           = " + x.equals(w)
                + "  (must be false)");

        System.out.println();
        System.out.println("All five held: "
                + (reflexive && symmetric && transitive && consistent && nullIsFalse && !x.equals(w)));
        System.out.println("=> These are asserted individually in EqualityContractTest.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
