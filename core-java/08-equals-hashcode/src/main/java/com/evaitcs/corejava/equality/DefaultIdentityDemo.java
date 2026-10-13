package com.evaitcs.corejava.equality;

/**
 * Demo 1 — the DEFAULT: Object.equals IS ==. Before we fix anything, see the disease.
 *
 * A class that overrides nothing inherits Object.equals, which is literally
 * `return this == obj;` — pure IDENTITY. Two objects with identical fields are NOT
 * equal, because they are two objects. For VALUE concepts (ids, keys, money) that is
 * the wrong question: we want "same content", not "same heap slot".
 *
 * The nested BrokenKey here overrides NOTHING (no equals, no hashCode) so we can watch
 * the inherited identity behavior, and print the identityHashCodes that differ.
 */
public class DefaultIdentityDemo {

    /** Overrides nothing — this is what EVERY class looks like before you touch it. */
    static class PlainKey {
        private final String branchCode;
        private final String accountNumber;

        PlainKey(String branchCode, String accountNumber) {
            this.branchCode = branchCode;
            this.accountNumber = accountNumber;
        }

        @Override
        public String toString() {
            return branchCode + "/" + accountNumber;
        }
    }

    public static void main(String[] args) {

        banner("1. Same fields, two objects — the default says NOT equal");

        PlainKey a = new PlainKey("BR-01", "1001");
        PlainKey b = new PlainKey("BR-01", "1001");   // identical content, different object

        System.out.println("a                       = " + a);
        System.out.println("b                       = " + b);
        System.out.println("a == b                  : " + (a == b) + "   <- different heap slots");
        System.out.println("a.equals(b)             : " + a.equals(b) + "   <- DEFAULT equals IS ==");
        System.out.println("identityHashCode(a)     : " + System.identityHashCode(a));
        System.out.println("identityHashCode(b)     : " + System.identityHashCode(b));
        System.out.println("=> Two keys for the SAME account, and Java says they are different.");

        banner("2. Why that is catastrophic for a Map");

        // The map keys off the OBJECT we put in. A brand-new equal-content key is a
        // different object, so containsKey/get say "never heard of it".
        java.util.Map<PlainKey, Transaction> balances = new java.util.HashMap<PlainKey, Transaction>();
        balances.put(a, Transaction.payroll());
        System.out.println("put(a, payroll); map size = " + balances.size());
        System.out.println("balances.get(a) [SAME object]  : " + balances.get(a));
        System.out.println("balances.get(b) [EQUAL object] : " + balances.get(b) + "   <- the money is GONE");
        System.out.println("=> For a VALUE like an account key, identity is simply the wrong question.");

        banner("3. The fix this module builds toward");
        System.out.println("Override equals to compare CONTENT, and hashCode to match —");
        System.out.println("then a.equals(b) is true and balances.get(b) finds the payroll.");
        System.out.println("(ProperEqualsDemo does equals line by line; HashCodeFixDemo closes the trap.)");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
