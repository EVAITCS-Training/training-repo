package com.evaitcs.corejava.oop;

/**
 * Class vs object in two minutes — the picture to draw BEFORE opening any code:
 *
 *   CLASS  = the blueprint.  One Customer.java, compiled once.
 *   OBJECT = a house built from it. As many as the heap can hold, each with its
 *            own address and its own copies of the instance fields.
 *
 * `new` does all of this, in this order (module 01 drew the heap, module 02 drew
 * the reference pointing into it — this demo connects the two pictures):
 *   1. allocate space for the object's fields ON THE HEAP
 *   2. fill with defaults (null for references, 0/false for primitives)
 *   3. run field initializers and instance initializer blocks, IN TEXTUAL ORDER
 *   4. run the constructor body
 *   5. `new` evaluates to a REFERENCE (an address) to that heap object
 */
public class ClassVsObjectDemo {

    public static void main(String[] args) {

        banner("1. One blueprint, three houses");
        Customer ada = new Customer("Ada Lovelace");
        Customer grace = new Customer("Grace Hopper");
        Customer guest = new Customer();          // no-arg ctor -> delegates down the this(...) chain
        System.out.println("  " + ada.greet());
        System.out.println("  " + grace.greet());
        System.out.println("  " + guest.greet());
        System.out.println();
        System.out.println("  ada == grace                ? " + (ada == grace)
                + "    <- two objects = two heap addresses (== compares references, module 02)");
        System.out.println("  ada.getClass() == grace.get.. ? " + (ada.getClass() == grace.getClass())
                + "     <- but ONE blueprint: both references point at the same Class");
        System.out.println("  identityHashCode(ada)       = " + Integer.toHexString(System.identityHashCode(ada)));
        System.out.println("  identityHashCode(grace)     = " + Integer.toHexString(System.identityHashCode(grace))
                + "    <- different houses, different addresses");

        banner("2. Field initialization order — textual, after super(), before the ctor body");
        System.out.println("  Creating one Vault; every step of its birth announces itself:");
        new Vault();
        System.out.println();
        System.out.println("  Trap: an initializer that READS a field declared further down sees");
        System.out.println("        that field's default (null/0) — the order is TEXTUAL, not by dependency.");

        banner("3. Constructors: default vs explicit, this(...) chaining");
        System.out.println("  Declare NO constructor at all and the compiler inserts a free no-arg");
        System.out.println("  default. Declare even ONE yourself and the freebie disappears: without");
        System.out.println("  Customer()'s explicit body above, new Customer() would not compile.");
        System.out.println();
        System.out.println("  Customer's three constructors form a chain; only the last has real work:");
        System.out.println("    Customer()          ->  this(\"Walk-in Guest\")");
        System.out.println("    Customer(name)      ->  this(\"C-\" + ++nextNumber, name)");
        System.out.println("    Customer(id, name)  ->  validate, then assign   <- the ONE working body");
        System.out.println("  One working body = one place where validation lives. (That instinct has");
        System.out.println("  a name — encapsulation — and it gets the next demo to itself.)");
        System.out.println();
        System.out.println("  Why did the three customers get DIFFERENT ids? nextNumber is STATIC —");
        System.out.println("  one copy shared by the whole class instead of one per object.");
        System.out.println("  Planting the flag only: the full static-vs-instance dive is module 11.");
    }

    /** A tiny class whose only job is to narrate its own birth. */
    static class Vault {
        private String dial = trace("field initializer: dial    (textual position 1)");
        { trace("instance initializer block    (textual position 2)"); }
        private String shelf = trace("field initializer: shelf   (textual position 3)");

        Vault() {
            // super() — Object's constructor — has already run silently by this point,
            // and every initializer above has already executed. The body comes LAST.
            trace("constructor body              (runs LAST)");
        }
    }

    /** Prints the step and returns it, so it can be used AS a field initializer. */
    static String trace(String step) {
        System.out.println("    " + step);
        return step;
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
