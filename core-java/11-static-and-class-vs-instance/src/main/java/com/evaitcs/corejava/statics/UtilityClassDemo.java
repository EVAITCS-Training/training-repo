package com.evaitcs.corejava.statics;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Demo 3 - the utility class done right: all-static BankUtils with a PRIVATE
 * constructor, constants, and the static-final-mutable-object trap.
 *
 * The reflection half is module 06 in action: the constructor EXISTS (getDeclared
 * finds it) but is INACCESSIBLE by name - and setAccessible(true) proves private
 * is a compile-time door with a runtime lock that reflection can pick.
 */
public class UtilityClassDemo {

    public static void main(String[] args) {

        banner("1. new BankUtils() does not compile - the constructor is PRIVATE");
        //     BankUtils u = new BankUtils();
        //         javac: error: BankUtils() has private access in BankUtils
        System.out.println("Why private at all? A utility class is a NAMESPACE, not a type:");
        System.out.println("every member is static, so an instance would hold no state and");
        System.out.println("add no behavior. The private ctor says that out loud - and as a");
        System.out.println("side effect blocks subclassing (a subclass ctor must call super()).");

        banner("2. Reflection proof: the ctor exists but is inaccessible (module 06)");
        Constructor<?>[] ctors = BankUtils.class.getDeclaredConstructors();
        System.out.println("getDeclaredConstructors().length = " + ctors.length);
        System.out.println("  found: " + ctors[0] + "   <- modifiers say "
                + (Modifier.isPrivate(ctors[0].getModifiers()) ? "PRIVATE" : "?"));
        try {
            ctors[0].newInstance(); // no setAccessible - the honest attempt
            System.out.println("instantiated?! That should not happen.");
        } catch (IllegalAccessException e) {
            System.out.println("newInstance() threw IllegalAccessException");
            System.out.println("  " + e.getMessage());
            System.out.println("=> the RUNTIME enforces private, just like the compiler did.");
        } catch (ReflectiveOperationException e) {
            System.out.println("unexpected reflection failure: " + e);
        }
        ctors[0].setAccessible(true); // the escape hatch picks the lock...
        try {
            ctors[0].newInstance();   // ...and walks straight into the guard
            System.out.println("instantiated with setAccessible?! The guard did not fire?");
        } catch (InvocationTargetException e) {
            System.out.println("setAccessible(true) got in - and the ctor's guard fired:");
            System.out.println("  " + e.getCause());
            System.out.println("=> belt (private) AND braces (throwing guard): even reflection");
            System.out.println("   is told, loudly, that this class has no instances.");
        } catch (ReflectiveOperationException e) {
            System.out.println("unexpected reflection failure: " + e);
        }

        banner("3. Constants: static final + UPPER_SNAKE_CASE");
        System.out.println("BankUtils.CURRENCY_CODE   = " + BankUtils.CURRENCY_CODE);
        System.out.println("BankUtils.OVERDRAFT_LIMIT = " + BankUtils.OVERDRAFT_LIMIT);
        //     BankUtils.CURRENCY_CODE = "EUR";
        //         javac: error: cannot assign a value to final variable CURRENCY_CODE

        banner("4. The TRAP: static final List - final REFERENCE, mutable CONTENTS");
        System.out.println("before: SUPPORTED_CURRENCIES = " + BankUtils.SUPPORTED_CURRENCIES);
        BankUtils.SUPPORTED_CURRENCIES.add("GBP"); // perfectly legal! final froze the SLOT, not the object
        System.out.println("after add(\"GBP\"): SUPPORTED_CURRENCIES = " + BankUtils.SUPPORTED_CURRENCIES);
        BankUtils.SUPPORTED_CURRENCIES.remove("GBP"); // tidy up: this is a GLOBAL - demo 5 explains why that matters
        //     BankUtils.SUPPORTED_CURRENCIES = new ArrayList<String>();
        //         javac: error: cannot assign a value to final variable SUPPORTED_CURRENCIES
        System.out.println("`final` freezes the VARIABLE (the slot holding the reference), never the");
        System.out.println("object behind it - the same split as effectively-final captures in");
        System.out.println("java8-features/01-lambdas. Want REAL immutability? Wrap it at the");
        System.out.println("declaration: Collections.unmodifiableList(...)");
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
