package com.evaitcs.corejava.statics;

import java.lang.reflect.Field;

/**
 * Demo 6 - static NESTED classes vs INNER classes: the naming trap everyone
 * repeats ("inner class" is NOT a synonym for "nested class" - inner means
 * NON-static nested, and the two are different animals).
 *
 *   static nested  = a normal class that merely LIVES inside another. No outer
 *                    instance needed; cannot touch outer instance members.
 *   inner          = glued to ONE outer object via an invisible reference
 *                    (Outer.this), built with outer.new Inner(), and it keeps
 *                    the outer object ALIVE as long as it is reachable.
 *
 * Rule: default to STATIC nested. Write an inner class only when the object
 * genuinely needs a per-outer-instance identity (iterators, handlers wired to
 * one outer object).
 */
public class NestedVsInnerDemo {

    public static void main(String[] args) {

        banner("1. STATIC nested - constructible with NO outer object in existence");
        Bank.Vault vault = new Bank.Vault("V-01"); // note: no Bank instance anywhere yet
        System.out.println("new Bank.Vault(\"V-01\") -> " + vault.describe());
        // Inside Vault, reaching for the outer INSTANCE field fails exactly like in
        // a static method (a static nested class IS a static member):
        //     String s = branchName;
        //         javac: error: non-static variable branchName cannot be referenced from a static context

        banner("2. INNER - glued to one outer object, built as outer.new Inner()");
        Bank bank = new Bank("Northside");
        Bank.Teller teller = bank.new Teller("Ann"); // the syntax itself gives the game away
        System.out.println("bank.new Teller(\"Ann\") -> " + teller.describe());
        Bank other = new Bank("Southside");
        Bank.Teller teller2 = other.new Teller("Bo");
        System.out.println("other.new Teller(\"Bo\") -> " + teller2.describe());
        System.out.println("Same inner class, two DIFFERENT outer objects - each Teller reads");
        System.out.println("ITS OWN bank's branchName through the hidden reference.");

        banner("3. The invisible reference, made visible");
        for (Field f : Bank.Teller.class.getDeclaredFields()) {
            System.out.println("  Teller field: " + f);
        }
        System.out.println("`this$0` is javac-generated: THAT is how teller reaches branchName.");
        System.out.println("Consequence: while a Teller is reachable, its Bank can NOT be garbage");
        System.out.println("collected - a long-lived inner object pins its outer object in memory");
        System.out.println("(the classic listener/Handler memory leak; mentioned, not demoed).");

        banner("4. The rule");
        System.out.println("Default to STATIC NESTED (Vault). It is simpler, leak-free, and says");
        System.out.println("'I need no outer object'. Choose INNER (Teller) only when each instance");
        System.out.println("must talk to ONE specific outer object.");
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    /** The "outer" class. Static nested in the demo so main() can build one directly. */
    static class Bank {

        /** A static member of Bank - visible even to the static nested Vault. */
        static final String BANK_CODE = "SB";

        private final String branchName; // an INSTANCE member - Vault can never see this

        Bank(String branchName) {
            this.branchName = branchName;
        }

        /** STATIC nested: independent; sees Bank's STATICS, never a branchName. */
        static class Vault {
            private final String vaultId;

            Vault(String vaultId) {
                this.vaultId = vaultId;
            }

            String describe() {
                return "Vault " + BANK_CODE + "-" + vaultId
                        + " (static nested - reads the outer STATIC, no outer instance involved)";
            }
        }

        /** INNER: every Teller belongs to exactly ONE Bank and reads its fields freely. */
        class Teller {
            private final String name;

            Teller(String name) {
                this.name = name;
            }

            String describe() {
                // branchName is really Bank.this.branchName - the qualification below
                // proves the hidden reference is a plain field access in disguise.
                return "Teller " + name + " at " + Bank.this.branchName
                        + " (inner - one hidden reference to its own outer object)";
            }
        }
    }
}
