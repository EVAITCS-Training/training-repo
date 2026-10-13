package com.evaitcs.corejava.jvm;

/**
 * Demo 5 — classes are loaded LAZILY: on first ACTIVE use, not before.
 *
 * The JVM does the least work it can get away with. A class is not initialized when your
 * program starts, nor when the file was compiled — it is initialized the first moment the
 * running code actually touches it (calls a method, reads a non-constant static field, does
 * `new`). Its static initializer runs exactly ONCE, at that moment.
 *
 * Deeper reflection (Class.forName, custom class loaders) belongs to a later course; one
 * pointer is enough here — see the comment on LazyVault below.
 */
public class ClassLoadingDemo {

    // These two flags live in THIS (outer) class, which is already initialized by the time
    // main() runs — so reading them canNOT accidentally trigger the thing we are demonstrating.
    // (If we put the flag inside LazyVault, then merely checking the flag would load the vault!)
    // Package-private so the JUnit test can watch the same evidence.
    static boolean vaultInitialized = false;
    static int initializerRuns = 0;

    public static void main(String[] args) {

        banner("First active use loads the class — watch for ONE extra println, between the lines");

        System.out.println("  Before touching LazyVault : vaultInitialized = " + vaultInitialized
                + ", initializerRuns = " + initializerRuns);

        System.out.println("  Calling LazyVault.open() the FIRST time...");
        System.out.println("    -> " + LazyVault.open());
        System.out.println("  After first touch         : vaultInitialized = " + vaultInitialized
                + ", initializerRuns = " + initializerRuns);

        System.out.println("  Calling LazyVault.open() a SECOND time...");
        System.out.println("    -> " + LazyVault.open());
        System.out.println("  After second touch        : initializerRuns = " + initializerRuns
                + "   <- the static block ran EXACTLY once, no second [static initializer] line appeared.");
        System.out.println();
        System.out.println("  The [static initializer] line printed BETWEEN our printlns: proof the class");
        System.out.println("  was loaded at that MOMENT (first use), not at compile time and not at startup.");
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    /**
     * A nested static class compiles to its OWN file — ClassLoadingDemo$LazyVault.class —
     * and is loaded/initialized INDEPENDENTLY of the outer class. That separation is what
     * lets us show "not yet" and then "now" for one class without touching the other.
     */
    static class LazyVault {
        static {
            // The static initializer: runs once, on first active use of LazyVault.
            initializerRuns++;
            vaultInitialized = true;
            System.out.println("    [static initializer] SecureBank vault class initialized RIGHT NOW.");
        }

        static String open() {
            return "vault SB-VLT-001 opened";
        }
    }
}
