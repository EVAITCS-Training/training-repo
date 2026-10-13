package com.evaitcs.corejava.access;

import com.evaitcs.corejava.access.bank.BankAccount;

/**
 * Modifiers on CLASSES, not members. The dial loses two of its four settings
 * at top level - a fact most students discover via a baffling error at 2 a.m.
 * Nested classes keep all four. This demo walks the three cases.
 */
public class ClassLevelModifiersDemo {

    public static void main(String[] args) {

        banner("1. Top-level classes: public or package-private ONLY");
        // private class TopLevel { }
        //   javac: error: modifier private not allowed here
        // protected class TopLevelEither { }
        //   javac: error: modifier protected not allowed here
        System.out.println("`private class TopLevel {}`    -> error: modifier private not allowed here");
        System.out.println("`protected class TopLevel {}`  -> error: modifier protected not allowed here");
        System.out.println("WHY: a top-level class has no enclosing class or inheritance chain to");
        System.out.println("     give 'private'/'protected' a MEANING - the package is the smallest");
        System.out.println("     unit that can contain it. So the dial has two settings: exported");
        System.out.println("     (public) or module-internal (no keyword).");

        banner("2. Package-private top-level class: PackagePrivateHelper");
        // import com.evaitcs.corejava.access.bank.PackagePrivateHelper;
        //   javac: error: PackagePrivateHelper is not public in com.evaitcs.corejava.access.bank; cannot be accessed from outside package
        System.out.println("It exists and works - SamePackageClass calls it - but from THIS package");
        System.out.println("it cannot even be imported (real javac error above in the comment).");
        try {
            // At RUNTIME the name is just a string, so reflection can still load it.
            // (Loading != compiling: the compiler never saw the forbidden NAME here.)
            Class<?> hidden = Class.forName("com.evaitcs.corejava.access.bank.PackagePrivateHelper");
            System.out.println("Class.forName found it anyway: " + hidden.getName()
                    + ", public? " + java.lang.reflect.Modifier.isPublic(hidden.getModifiers()));
        } catch (ClassNotFoundException e) {
            System.out.println("not found: " + e.getMessage());
        }
        System.out.println("=> this is how the JDK hides implementation classes inside a package.");

        banner("3. NESTED classes keep all four settings: the private Vault");
        // BankAccount.Vault vault = ...;
        //   javac: error: Vault has private access in BankAccount
        System.out.println("BankAccount.Vault is private static nested - invisible outside BankAccount.");
        System.out.println("We can still watch it WORK through the public door:");
        BankAccount acct = BankAccount.sampleAccount();
        System.out.println("  acct.checkPin(\"2468\") = " + acct.checkPin("2468") + "   (Vault said yes)");
        System.out.println("  acct.checkPin(\"1111\") = " + acct.checkPin("1111") + "  (Vault said no)");
        System.out.println("=> private nested class = an implementation detail with a NAME, kept");
        System.out.println("   out of the API. Callers depend on checkPin(), never on Vault.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
