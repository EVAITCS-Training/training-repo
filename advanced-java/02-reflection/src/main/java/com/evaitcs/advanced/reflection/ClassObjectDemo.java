package com.evaitcs.advanced.reflection;

/**
 * Demo 1 — THE CLASS OBJECT, and the three doors to it.
 * (README sections "The big idea" and "Walkthrough — what happens when you call
 * Class.forName(name)".)
 *
 * The claim: for one loaded type there is exactly ONE Class object, and all three ways of
 * asking for it hand back the very same instance (==, not equals). A Class object is the
 * JVM's answer sheet about a type: its name, fields, methods, constructors, annotations.
 *
 * The second claim is subtler and is the reason frameworks care which door you use:
 * Class.forName(name) does not just LOOK UP the class, it LOADS and INITIALIZES it —
 * static initializers RUN. Probe's static block is the tripwire that proves it.
 */
public class ClassObjectDemo {

    public static void main(String[] args) {
        banner("1. Three doors, ONE Class object");

        Account account = new Account("A-1", "Dana", new java.math.BigDecimal("250.00"));

        // Door A: the compile-time literal. Does NOT initialize the class.
        Class<Account> byLiteral = Account.class;
        // Door B: ask any instance what it is. The class is obviously already loaded.
        Class<?> byInstance = account.getClass();
        // Door C: look it up by NAME at runtime — the door frameworks use when the type
        // is a string in a config file. This one LOADS + INITIALIZES.
        Class<?> byName;
        try {
            byName = Class.forName("com.evaitcs.advanced.reflection.Account");
        } catch (ClassNotFoundException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("why: the name did not match any class on the classpath.");
            return;
        }

        System.out.println("step 1: Account.class            = " + byLiteral.getName());
        System.out.println("step 2: account.getClass()       = " + byInstance.getName());
        System.out.println("step 3: Class.forName(\"...Account\") = " + byName.getName());
        System.out.println("step 4: all three == the same object? "
                + ((byLiteral == byInstance) && (byInstance == byName)));
        System.out.println("verdict: one loaded type has ONE Class object; every door returns it.");

        banner("2. Class.forName INITIALIZES — the static block runs (side-effect proof)");

        // ProbeLog lives in a SEPARATE class so reading the counter does NOT initialize
        // Probe. If the counter lived on Probe, this very read would trip the wire early.
        int before = ProbeLog.INITIALIZATIONS;
        System.out.println("step 1: Probe initializations so far = " + before + "  (Probe not touched yet)");
        try {
            Class<?> probe = Class.forName("com.evaitcs.advanced.reflection.Probe");
            int after = ProbeLog.INITIALIZATIONS;
            System.out.println("step 2: called Class.forName(\"...Probe\")  -> returned " + probe.getName());
            System.out.println("step 3: Probe initializations now       = " + after);
            System.out.println("step 4: probe == Probe.class (same object, now initialized)? " + (probe == Probe.class));
            System.out.println("verdict: forName ran Probe's static initializer — " + before + " became " + after + ".");
        } catch (ClassNotFoundException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        banner("3. Which door, when?");

        System.out.println("X.class        : you know the type at compile time. No initialization. Cheapest.");
        System.out.println("x.getClass()   : you hold an instance and want its RUNTIME class (may be a subclass).");
        System.out.println("Class.forName  : the type is a STRING chosen at runtime (drivers, plugins, Spring).");
        System.out.println("                 It initializes the class — a side effect you must expect.");
        System.out.println("                 Want the class WITHOUT initializing? forName(name, false, loader).");

        banner("4. In one sentence");
        System.out.println("A Class object is the JVM's answer sheet about a type: three doors reach the");
        System.out.println("same one object, and only Class.forName also runs the type's static initializers.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
