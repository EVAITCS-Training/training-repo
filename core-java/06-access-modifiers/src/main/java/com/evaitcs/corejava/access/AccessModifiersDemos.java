package com.evaitcs.corejava.access;

/**
 * Launcher for the whole module - runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 06-access-modifiers compile exec:java
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 06-access-modifiers compile exec:java "-Dexec.mainClass=com.evaitcs.corejava.access.ProtectedViaSubclassDemo"
 */
public class AccessModifiersDemos {

    public static void main(String[] args) {
        section("FromInsideTheClassDemo - vantage point: the class itself (all four levels visible)");
        FromInsideTheClassDemo.main(args);

        section("SamePackageDemo - vantage point: the same package (package-private opens up)");
        SamePackageDemo.main(args);

        section("OtherPackageDemo - vantage point: another package, no inheritance (public only)");
        OtherPackageDemo.main(args);

        section("ProtectedViaSubclassDemo - vantage point: subclass in another package (the exam favorite)");
        ProtectedViaSubclassDemo.main(args);

        section("ClassLevelModifiersDemo - modifiers on CLASSES: top-level vs nested");
        ClassLevelModifiersDemo.main(args);

        section("ReflectionEscapeHatchDemo - private is a compile-time door, and reflection has a key");
        ReflectionEscapeHatchDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
