package com.evaitcs.advanced.exceptions;

import java.io.IOException;

/**
 * Demo 1 — THE Throwable HIERARCHY, and the checked/unchecked split.
 * (README "The Throwable hierarchy" and its compiler walkthrough.)
 *
 * The claim: every throwable is a Throwable, but the family splits in two, and the split
 * decides whether the COMPILER forces you to handle it.
 *
 *   Error       — the JVM is in trouble (OutOfMemoryError, StackOverflowError). Do not catch.
 *   Exception   — recoverable problems. Its non-RuntimeException branch is CHECKED.
 *   RuntimeException — programming mistakes (NullPointerException, ...). UNCHECKED.
 *
 * CHECKED   = Exception and subclasses, MINUS RuntimeException. The compiler forces handling.
 * UNCHECKED = RuntimeException (+ subclasses) and Error (+ subclasses). No compiler forcing.
 *
 * Evidence printed live: the subtype relationships (asked of the public Class objects, no
 * reaching into internals), one checked throw that MUST be declared+caught, and one
 * unchecked throw that a method may let escape with no `throws` clause at all.
 */
public class ThrowableHierarchyDemo {

    public static void main(String[] args) {
        banner("1. The family tree, asked of the Class objects themselves");

        // isAssignableFrom is PUBLIC API — we ask "is every X also a Y?", not poke internals.
        System.out.println("step 1: Throwable is the root. Everything you can throw IS-A Throwable.");
        System.out.println("step 2: Exception IS-A Throwable        = " + Throwable.class.isAssignableFrom(Exception.class));
        System.out.println("step 3: RuntimeException IS-A Exception = " + Exception.class.isAssignableFrom(RuntimeException.class));
        System.out.println("step 4: Error IS-A Throwable            = " + Throwable.class.isAssignableFrom(Error.class));
        System.out.println("step 5: Error IS-A Exception            = " + Exception.class.isAssignableFrom(Error.class) + "   (Error is a SEPARATE branch)");
        System.out.println();
        System.out.println("        Throwable");
        System.out.println("        +-- Error            (JVM in trouble: OutOfMemoryError)      -> UNCHECKED");
        System.out.println("        +-- Exception        (recoverable)");
        System.out.println("            +-- IOException  (checked: compiler forces handling)     -> CHECKED");
        System.out.println("            +-- RuntimeException (programming mistake)              -> UNCHECKED");
        System.out.println("                +-- NullPointerException, ArithmeticException, ...   -> UNCHECKED");
        System.out.println("verdict: CHECKED = Exception minus RuntimeException; UNCHECKED = RuntimeException + Error.");

        banner("2. A CHECKED throw: the compiler forces you to declare AND handle it");

        // readConfig() declares `throws IOException`. IOException is checked, so javac will
        // NOT compile a caller that neither catches it nor re-declares it. That forcing is
        // the entire point of "checked".
        try {
            readConfig();
        } catch (IOException e) {
            System.out.println("step 1: readConfig() declared `throws IOException` — a CHECKED exception.");
            System.out.println("step 2: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("        WHY: the compiler refused to build until this caller handled it.");
            System.out.println("verdict: checked = 'the compiler will not look away; handle it or declare it.'");
        }

        banner("3. An UNCHECKED throw: the method declares NOTHING and it still escapes");

        // parse() has no `throws` clause, yet Integer.parseInt can throw NumberFormatException
        // (unchecked). javac allows that: unchecked exceptions are nobody's compile-time
        // business. We still catch it here because we CHOOSE to recover, not because we must.
        try {
            int n = parse("not-a-number");
            System.out.println("never reached: " + n);
        } catch (NumberFormatException e) {
            System.out.println("step 1: parse() declared NO throws clause — NumberFormatException is UNCHECKED.");
            System.out.println("step 2: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("        WHY it happened: Integer.parseInt met a non-numeric string.");
            System.out.println("verdict: unchecked = the compiler stays silent; handling is the author's choice.");
        }

        banner("4. Walkthrough — what the compiler does when it sees a CHECKED throw");
        System.out.println("step 1: javac reads `throw new IOException(...)` and asks: is IOException checked?");
        System.out.println("step 2: checked = a subtype of Exception that is NOT a subtype of RuntimeException. IOException: yes.");
        System.out.println("step 3: javac then demands the enclosing method EITHER wraps it in try/catch");
        System.out.println("        OR lists it in a `throws IOException` clause. Neither? COMPILE ERROR.");
        System.out.println("step 4: if the method declares `throws`, the SAME rule applies to every caller,");
        System.out.println("        all the way up, until someone catches it or main() declares it.");
        System.out.println("step 5: unchecked exceptions skip steps 3-4 entirely — no forcing, no `throws` needed.");
        System.out.println("verdict: 'checked' is a COMPILE-TIME contract about handling, enforced up the call chain.");

        banner("5. In one sentence");
        System.out.println("Throwable splits into Error and Exception; Exception minus RuntimeException is CHECKED");
        System.out.println("(the compiler forces handling), and RuntimeException plus Error is UNCHECKED (it does not).");
    }

    /** A CHECKED exception in its natural habitat: the method must declare `throws IOException`. */
    static void readConfig() throws IOException {
        throw new IOException("config file missing");
    }

    /** An UNCHECKED exception: no `throws` clause, yet parseInt can still throw at runtime. */
    static int parse(String s) {
        return Integer.parseInt(s);   // may throw NumberFormatException (unchecked) — undeclared, legal
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
