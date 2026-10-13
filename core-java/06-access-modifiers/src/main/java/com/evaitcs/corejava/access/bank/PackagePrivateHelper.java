package com.evaitcs.corejava.access.bank;

/**
 * NO `public` keyword on the class => the CLASS ITSELF is package-private.
 * Modifiers apply to types too, and for TOP-LEVEL classes the dial has only
 * two settings: public or package-private. (private/protected top-level classes
 * are compile errors - ClassLevelModifiersDemo shows the exact messages.)
 *
 * This is the JDK's favorite hiding trick: java.util.Arrays is public, but the
 * implementation helpers around it are not - you cannot import what was never
 * exported. Package-private = "module-internal", years before Java 9 had modules.
 *
 * Proof it works: SamePackageClass (same package) calls describeRules() and
 * compiles. UnrelatedClass (other package) has the identical import commented
 * out beside its real javac error.
 */
class PackagePrivateHelper {

    private PackagePrivateHelper() {
    }

    static String describeRules() {
        return "PackagePrivateHelper: a top-level class with NO modifier - visible only inside "
                + "com.evaitcs.corejava.access.bank";
    }
}
