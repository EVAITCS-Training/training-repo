package com.evaitcs.corejava.strings;

/**
 * Claim #2: String LITERALS are interned — the JVM keeps ONE shared object per
 * literal text in a special area called the STRING POOL, and every occurrence of
 * the same literal points at that one object.
 *
 * This is the demo that finally explains why == "sometimes works" on Strings:
 * it works on literals (shared pool objects) and fails on anything built at
 * runtime (fresh heap objects). Same characters, different objects — and that is
 * why == is a trap and equals is the rule (next demo).
 *
 * Watch the identityHashCode numbers on the projector: same number = same object.
 */
public class StringPoolDemo {

    public static void main(String[] args) {

        banner("1. Literals are shared: \"ACC\" == \"ACC\" is TRUE");

        String a = "ACC";
        String b = "ACC";   // not a second object — the JVM reuses the pooled one

        System.out.println("a == b                  : " + (a == b) + "   <- SAME object");
        System.out.println("identityHashCode(a)     : " + System.identityHashCode(a));
        System.out.println("identityHashCode(b)     : " + System.identityHashCode(b) + "   <- same number");

        banner("2. `new` forces a fresh heap object: new String(\"ACC\") != \"ACC\"");

        String c = new String("ACC");   // explicit `new` means "I want a DISTINCT object"

        System.out.println("c == a                  : " + (c == a) + "  <- different objects");
        System.out.println("c.equals(a)             : " + c.equals(a) + "   <- same characters");
        System.out.println("identityHashCode(c)     : " + System.identityHashCode(c));
        System.out.println("identityHashCode(a)     : " + System.identityHashCode(a) + "   <- different numbers");
        System.out.println("=> Almost nobody writes new String(\"...\") on purpose — it is an interview");
        System.out.println("   question and a bug factory. The pool only serves LITERALS and intern() results.");

        banner("3. intern() — 'give me the pooled one'");

        String pooled = c.intern();     // returns the pool's object for the text "ACC"

        System.out.println("c.intern() == a         : " + (pooled == a) + "   <- intern() points back at the pool");

        banner("4. The compiler FOLDS constant concatenation at COMPILE TIME");

        String folded = "ACC" + "-1001";   // javac folds this into the literal "ACC-1001"

        System.out.println("folded == \"ACC-1001\"    : " + (folded == "ACC-1001") + "   <- same pooled object");
        System.out.println("identityHashCode(folded): " + System.identityHashCode(folded));
        System.out.println("identityHashCode(lit)   : " + System.identityHashCode("ACC-1001") + "   <- same number");
        System.out.println("=> \"ACC\" + \"-1001\" never runs at runtime: javac glued the two constants");
        System.out.println("   together BEFORE the program existed, so it is just one more literal.");

        // TRAP to name out loud: `final String PREFIX = "ACC"; PREFIX + "-1001"` ALSO
        // folds — a final String initialized with a literal IS a compile-time constant
        // (JLS 15.28). One little `final` changes whether == "works". Never depend on it.

        banner("5. Anything built at RUNTIME is a fresh object — even with the same text");

        String prefix = customerPrefix();      // method call — the compiler cannot fold this
        String runtime = prefix + "-1001";     // built while the program runs (StringBuilder inside)

        System.out.println("runtime                 : \"" + runtime + "\"");
        System.out.println("runtime == \"ACC-1001\"   : " + (runtime == "ACC-1001") + "  <- fresh heap object");
        System.out.println("runtime.equals(literal) : " + runtime.equals("ACC-1001") + "   <- same characters");
        System.out.println("identityHashCode(runtime): " + System.identityHashCode(runtime));
        System.out.println("identityHashCode(lit)    : " + System.identityHashCode("ACC-1001") + "   <- different numbers");
        System.out.println("=> Text read from a scanner, a database, a JSON body, or built with + at");
        System.out.println("   runtime NEVER arrives from the pool. That is the whole trap in one line.");

        banner("6. The rule this demo earns");

        System.out.println("==      asks: SAME OBJECT?       (identity — usually not what you mean for text)");
        System.out.println("equals  asks: SAME CHARACTERS?   (content  — what you actually meant)");
    }

    /**
     * A method call is NEVER a compile-time constant, so javac cannot fold what
     * comes back through it. This is the standard classroom trick for producing a
     * genuine runtime-built String with text identical to a literal.
     */
    private static String customerPrefix() {
        return "ACC";
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
