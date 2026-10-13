package com.evaitcs.advanced.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;

/**
 * Demo 2 — INSPECTING THE SHAPE of a class. (README section "Reading the answer sheet".)
 *
 * Given a Class object (demo 1), reflection can list everything the class file recorded:
 * fields, methods, constructors, and the modifiers on each. This is what an IDE's outline,
 * a JSON mapper, and Spring's bean scanner all do.
 *
 * Two distinctions carry the whole demo:
 *   getDeclaredX()  -> ONLY this class, ANY visibility (private included)
 *   getX()          -> ONLY public, but INCLUDING members inherited from supertypes
 * and: reflection matches parameter types EXACTLY — long.class is not Long.class.
 */
public class InspectDemo {

    public static void main(String[] args) {
        banner("1. Fields — getDeclaredFields sees the PRIVATE ones too");

        for (Field field : Account.class.getDeclaredFields()) {
            // Modifier.toString turns the bitmask into words like "private final".
            System.out.println("  " + Modifier.toString(field.getModifiers())
                    + "  " + field.getType().getSimpleName() + " " + field.getName());
        }
        System.out.println("verdict: four private fields, invisible to normal code, listed here in one loop.");

        banner("2. Methods and constructors this class DECLARES");

        System.out.println("declared methods:");
        for (Method method : Account.class.getDeclaredMethods()) {
            System.out.println("  " + method.getName() + " returns " + method.getReturnType().getSimpleName()
                    + ", params " + method.getParameterCount());
        }
        System.out.println("declared constructors:");
        for (Constructor<?> ctor : Account.class.getDeclaredConstructors()) {
            System.out.println("  " + ctor.getName() + " params " + ctor.getParameterCount());
        }
        // The payoff of listing a constructor: you can BUILD one from nothing but its Class.
        // This is how a framework instantiates a bean named in a config file (module 03, WireMe).
        try {
            Constructor<Account> ctor =
                    Account.class.getDeclaredConstructor(String.class, String.class, BigDecimal.class);
            Account built = ctor.newInstance("A-7", "Gia", new BigDecimal("10.00"));
            System.out.println("Constructor.newInstance built a working Account -> " + built);
        } catch (ReflectiveOperationException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        banner("3. getDeclaredMethods() vs getMethods() — the INHERITED difference");

        int declared = Account.class.getDeclaredMethods().length;   // only Account's own
        int allPublic = Account.class.getMethods().length;          // Account's public + Object's public
        System.out.println("step 1: getDeclaredMethods().length = " + declared + "   (this class only, any visibility)");
        System.out.println("step 2: getMethods().length         = " + allPublic + "   (public only, PLUS inherited from Object)");
        System.out.println("step 3: the extra " + (allPublic - declared) + " are Object's public methods: toString, equals, hashCode, getClass, wait, notify...");
        System.out.println("verdict: getDeclared* = mine, any access. get* = public, but the whole family tree.");

        banner("4. Exact parameter types: long.class finds it, Long.class does NOT");

        try {
            Method byPrimitive = Account.class.getMethod("transferCents", long.class);
            System.out.println("step 1: getMethod(\"transferCents\", long.class) -> FOUND " + byPrimitive.getName());
        } catch (NoSuchMethodException e) {
            System.out.println("unexpected: " + e);
        }
        try {
            Account.class.getMethod("transferCents", Long.class);
            System.out.println("step 2: getMethod(\"transferCents\", Long.class) -> found (SHOULD NOT HAPPEN)");
        } catch (NoSuchMethodException e) {
            System.out.println("step 2: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("why: the parameter is the PRIMITIVE long; reflection matches the exact erased type,");
            System.out.println("     and long is not java.lang.Long. Autoboxing is a compile-time trick, not a runtime one.");
        }
        System.out.println("verdict: name alone is not enough — the parameter TYPE list is part of the signature.");

        banner("5. Generic signatures survive erasure (module 01's leftover, readable here)");

        try {
            Field statement = Account.class.getDeclaredField("statement");
            Class<?> raw = statement.getType();            // erasure left only List
            Type generic = statement.getGenericType();      // the signature kept List<Transaction>
            System.out.println("step 1: field 'statement' getType()        = " + raw.getName() + "   (raw — module 01 erased it)");
            System.out.println("step 2: field 'statement' getGenericType() = " + generic);
            if (generic instanceof ParameterizedType parameterized) {   // Java 21 pattern matching
                System.out.println("step 3: its type argument = " + parameterized.getActualTypeArguments()[0]);
            }
            System.out.println("verdict: objects lose their <Transaction>, but the DECLARATION is still on file.");
        } catch (NoSuchFieldException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        banner("6. In one sentence");
        System.out.println("A Class object lets you list a type's whole shape — getDeclared* for its own");
        System.out.println("members at any visibility, get* for the public inherited ones — matched by exact signature.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
