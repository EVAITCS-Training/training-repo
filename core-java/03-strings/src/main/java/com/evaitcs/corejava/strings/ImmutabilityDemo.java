package com.evaitcs.corejava.strings;

/**
 * Claim #1 of the module: a String is IMMUTABLE.
 *
 * There is not one single method in java.lang.String that changes the characters
 * inside an existing object. Every method that LOOKS like it modifies a String —
 * toUpperCase, substring, replace, concat, trim — actually builds a BRAND-NEW
 * String and returns it. Ignore the return value and nothing happened at all,
 * which is exactly what we prove below.
 *
 * Students arrive believing "s.toUpperCase() uppercases s". This demo kills that
 * idea in thirty seconds, and the rest of the module (pool, ==, concat cost)
 * stands on the corpse.
 */
public class ImmutabilityDemo {

    public static void main(String[] args) {

        banner("1. Call every 'mutator', ignore every result");

        String s = "secure";
        System.out.println("before anything       : s = \"" + s + "\"");

        // Each of these SOUNDS like it edits s. None of them do.
        // Each one builds a new String, hands it back, and we drop it on the floor.
        s.toUpperCase();      // returns "SECURE"   — dropped
        s.concat("!");        // returns "secure!"  — dropped
        s.replace('s', 'S');  // returns "Secure"   — dropped
        s.substring(0, 3);    // returns "sec"      — dropped
        s.trim();             // returns "secure"   — dropped

        System.out.println("after all five calls  : s = \"" + s + "\"   <- UNCHANGED");
        System.out.println("=> No method changes a String. If you ignore the return value,");
        System.out.println("   the call did literally nothing but waste a new object.");

        banner("2. 'Modifying' a String is really RE-BINDING the variable");

        // identityHashCode is the closest thing Java gives us to "the address of the
        // object" — two references with the SAME number point at the SAME object.
        System.out.println("s = \"" + s + "\"   identityHashCode(s) = " + System.identityHashCode(s));

        s = s.toUpperCase();  // read old object -> build NEW object -> point s at the new one

        System.out.println("s = s.toUpperCase();  ->  s = \"" + s + "\"");
        System.out.println("                        identityHashCode(s) = " + System.identityHashCode(s)
                + "   <- DIFFERENT number = DIFFERENT object");
        System.out.println("=> The old \"secure\" still exists (unchanged). s simply stopped pointing at it.");

        // The right way to read a chain: each step consumes one String, produces the next.
        // Four objects are involved below: the literal, and one per call.
        String auditKey = "  acc-1001  ".trim().toUpperCase().concat("-AUDIT");
        System.out.println("chain it properly     : \"  acc-1001  \".trim().toUpperCase().concat(\"-AUDIT\") = \"" + auditKey + "\"");

        banner("3. WHY did Java make Strings immutable? Four reasons to say out loud");

        System.out.println("1. SAFE TO SHARE  — the string pool (next demo) hands ONE object to everyone");
        System.out.println("                    who writes the same literal. That is only safe because");
        System.out.println("                    nobody can change the shared object under anyone's feet.");
        System.out.println("2. CACHED HASH    — hashCode is computed once and stored in the String.");
        System.out.println("                    That is why Strings make perfect HashMap keys.");
        System.out.println("                    (Plant the flag: the equals/hashCode contract is module 08.)");
        System.out.println("3. THREAD-SAFE    — two threads can read the same String with no locking;");
        System.out.println("                    there is no state anyone could race to change.");
        System.out.println("4. SECURITY       — validate a path/URL/account id, then pass it onward:");
        System.out.println("                    it CANNOT change after validation. If Strings were mutable,");
        System.out.println("                    a checked \"/safe/report.txt\" could be rewritten to");
        System.out.println("                    \"/etc/passwd\" AFTER the security check but BEFORE the open.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
