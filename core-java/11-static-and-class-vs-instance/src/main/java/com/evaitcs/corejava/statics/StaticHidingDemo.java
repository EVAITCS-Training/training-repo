package com.evaitcs.corejava.statics;

/**
 * Demo 7 - two closing traps:
 *
 * A. Static methods are HIDDEN, not overridden (module 05 callback). An instance
 *    method is dispatched at RUNTIME on the object (polymorphism); a static method
 *    is bound at COMPILE time from the reference's declared type - there is no
 *    object and no `this`, so there is nothing to dispatch on.
 *
 * B. What CANNOT be static - top-level classes, constructors, local variables -
 *    with the real javac messages, because compile errors can't run: the commented
 *    line beside its message IS the exhibit (module 06's format).
 */
public class StaticHidingDemo {

    public static void main(String[] args) {

        banner("1. Hiding vs overriding - same reference, two verdicts");
        AccountBase asBase = new SavingsSub(); // polymorphic reference, module 05
        System.out.println("asBase.describe()   -> " + asBase.describe());
        System.out.println("asBase.whoAmI()     -> " + asBase.whoAmI() + "   <- SURPRISE");
        System.out.println("SavingsSub.whoAmI() -> " + SavingsSub.whoAmI());
        System.out.println("The OBJECT is a SavingsSub in all three lines. The instance call");
        System.out.println("asks the object at runtime (override wins); the static call asks");
        System.out.println("the REFERENCE TYPE at compile time (hiding - both methods coexist).");
        System.out.println("That is also why @Override on whoAmI() is itself a compile error:");
        System.out.println("    javac: error: method does not override or implement a method from a supertype");

        banner("2. What CANNOT be static - real javac messages");
        // (a) A top-level class:
        //     public static class IllegalTopLevel { }     // in its own .java file
        //         javac: error: modifier static not allowed here
        //     -> `static` means "belongs to an enclosing CLASS, not to instances".
        //        A top-level class has no enclosing class - the word has nothing to mean.
        //        (Nested classes are the opposite: static nested is the RECOMMENDED default, demo 6.)
        //
        // (b) A constructor:
        //     static IllegalTopLevel() { }
        //         javac: error: modifier static not allowed here
        //     -> a constructor's whole job is initializing a NEW object (`this` inside it
        //        is that object). A static constructor would have no object to build.
        //        The legitimate "static way to make objects" is the factory - demo 2.
        //
        // (c) A local variable inside a method:
        //     void transfer() { static int retries = 0; }
        //         javac: error: modifier static not allowed here
        //     -> locals live in ONE method call's stack frame and die with it; `static`
        //        (one slot shared across ALL calls, for the class's lifetime) contradicts
        //        what a local IS. (Local classes follow the same rule set - skipped here.)
        System.out.println("  public static class Top { }   javac: error: modifier static not allowed here");
        System.out.println("  static Top() { }              javac: error: modifier static not allowed here");
        System.out.println("  void m() { static int x = 0; } javac: error: modifier static not allowed here");
        System.out.println("(top-level class / constructor / local variable - the commented lines above");
        System.out.println("carry the WHY for each.)");
        System.out.println("Pattern: `static` always means 'per CLASS, one copy, no object needed'.");
        System.out.println("Wherever that idea is meaningless - or already taken by a better");
        System.out.println("tool (factories) - the language simply forbids the keyword.");
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    /** The superclass half of the hiding exhibit. */
    static class AccountBase {
        static String whoAmI() {
            return "AccountBase.whoAmI() - the SUPERCLASS version";
        }

        String describe() {
            return "AccountBase.describe() - superclass instance method";
        }
    }

    /** The subclass that THINKS it overrides a static method. It hides it. */
    static class SavingsSub extends AccountBase {
        // Both whoAmI() methods now exist side by side; which one RUNS is decided by
        // the compile-time type of the reference, never by the object.
        static String whoAmI() {
            return "SavingsSub.whoAmI() - the SUBCLASS version";
        }

        @Override // legal HERE: instance methods really are overridden
        String describe() {
            return "SavingsSub.describe() - instance method, overridden for real";
        }
    }
}
