package com.evaitcs.corejava.strings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 03-strings test
 * Each test proves ONE claim and prints its evidence, so the test run doubles
 * as a demo. Everything asserted here is deterministic — identity (==/intern),
 * content (equals), and API facts. Timings are NEVER asserted: a slow laptop
 * is not a bug.
 */
class StringsBasicsTest {

    @Test
    @DisplayName("no 'mutator' changes a String — each one RETURNS a new one")
    void mutatorsReturnNewStringsAndLeaveTheOriginalAlone() {
        String s = "secure";

        String upper = s.toUpperCase();
        String cut = s.substring(0, 3);
        String swapped = s.replace('s', 'S');

        assertEquals("secure", s, "s itself must be untouched");
        assertEquals("SECURE", upper);
        assertEquals("sec", cut);
        assertEquals("Secure", swapped);
        assertNotSame(s, upper, "toUpperCase must hand back a DIFFERENT object");

        System.out.println("s = \"" + s + "\" survived toUpperCase/substring/replace;");
        System.out.println("results: \"" + upper + "\", \"" + cut + "\", \"" + swapped + "\" — three NEW objects");
        System.out.println("identityHashCode: s=" + System.identityHashCode(s)
                + " upper=" + System.identityHashCode(upper) + " (different numbers = different objects)");
    }

    @Test
    @DisplayName("literals are interned: the same literal text is the SAME pooled object")
    void literalsShareOnePooledObject() {
        String a = "ACC";
        String b = "ACC";

        assertSame(a, b, "both literals must point at the one pooled object");
        assertEquals(System.identityHashCode(a), System.identityHashCode(b));

        System.out.println("\"ACC\" == \"ACC\" : " + (a == b)
                + "  identityHashCode " + System.identityHashCode(a) + " == " + System.identityHashCode(b));
    }

    @Test
    @DisplayName("new String(...) forces a fresh heap object; intern() goes back to the pool")
    void newStringIsFreshButInternReturnsThePooledOne() {
        String literal = "ACC";
        String forced = new String("ACC");

        assertNotSame(literal, forced, "explicit `new` must NOT hand back the pooled object");
        assertEquals(literal, forced, "...but the characters are identical");
        assertFalse(literal == forced);
        assertSame(literal, forced.intern(), "intern() must return the POOLED instance");

        System.out.println("new String(\"ACC\") == \"ACC\"      : " + (literal == forced) + " (different objects)");
        System.out.println("new String(\"ACC\").equals(\"ACC\") : " + forced.equals(literal) + " (same characters)");
        System.out.println("new String(\"ACC\").intern() == \"ACC\" : " + (forced.intern() == literal));
    }

    @Test
    @DisplayName("compile-time constants fold into one literal; runtime-built text is a fresh object")
    void constantFoldingVersusRuntimeBuilding() {
        // JLS 15.28: literal + literal is a compile-time constant — javac folds it,
        // so `folded` IS the pooled "ACC-1001" object.
        String folded = "ACC" + "-1001";
        assertSame("ACC-1001", folded, "constant folding must produce the pooled literal");

        // A method call is NEVER a compile-time constant: this string is built while
        // the program runs (StringBuilder inside) and is a distinct heap object.
        String runtime = builtAtRuntime("ACC");
        assertEquals("ACC-1001", runtime, "same characters...");
        assertNotSame("ACC-1001", runtime, "...but NOT the same object — this is the == trap");
        assertSame("ACC-1001", runtime.intern(), "intern() proves the pool has the same text");

        System.out.println("(\"ACC\" + \"-1001\") == \"ACC-1001\"        : " + (folded == "ACC-1001") + " (folded at compile time)");
        System.out.println("builtAtRuntime(\"ACC\") == \"ACC-1001\"    : " + (runtime == "ACC-1001") + " (built at runtime)");
        System.out.println("identityHashCode: runtime=" + System.identityHashCode(runtime)
                + " literal=" + System.identityHashCode("ACC-1001"));
    }

    private static String builtAtRuntime(String prefix) {
        return prefix + "-1001"; // prefix is a parameter — impossible for javac to fold
    }

    @Test
    @DisplayName("== asks same-object, equals asks same-characters; constant-first is null-safe")
    void equalsVersusDoubleEqualsAndTheNullTrap() {
        String accountId = "ACC-1001";
        String fromChars = new String(new char[] { 'A', 'C', 'C', '-', '1', '0', '0', '1' });

        assertFalse(accountId == fromChars, "== compares REFERENCES");
        assertTrue(accountId.equals(fromChars), "equals compares CHARACTERS");
        assertTrue("active".equalsIgnoreCase("ACTIVE"));

        String status = null; // the lookup found nothing
        assertThrows(NullPointerException.class, () -> status.equals("ACTIVE"),
                "variable.equals(...) explodes on null");
        assertFalse("ACTIVE".equals(status), "literal-first idiom just returns false — no NPE");

        System.out.println("accountId == fromChars      : " + (accountId == fromChars));
        System.out.println("accountId.equals(fromChars) : " + accountId.equals(fromChars));
        System.out.println("status.equals(\"ACTIVE\")     : threw NullPointerException (caught by assertThrows)");
        System.out.println("\"ACTIVE\".equals(status)     : " + "ACTIVE".equals(status) + " <- the null-safe idiom");
    }

    @Test
    @DisplayName("loop += and StringBuilder produce EQUAL strings — every += is a new object")
    void loopConcatAndStringBuilderAgree() {
        final int n = 10_000; // smaller than the demo's 50k — tests stay fast; the claim is the same

        String plusLoop = "";
        for (int i = 0; i < n; i++) {
            plusLoop += (char) ('0' + (i % 10));
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append((char) ('0' + (i % 10)));
        }
        String built = sb.toString();

        assertEquals(built, plusLoop, "identical text — the ONLY difference is cost");
        assertEquals(n, plusLoop.length());

        // The deterministic half of the "object churn" claim: a runtime concat result
        // is ALWAYS a fresh object, even when a pooled string holds the same text.
        String seed = "ACC";
        assertNotSame("ACC-1", seed + "-1", "each `+` allocates — that is the churn, proven by identity");

        System.out.println("+= loop and StringBuilder both produced " + n + " chars, equal: " + built.equals(plusLoop));
        System.out.println("first 20: " + plusLoop.substring(0, 20) + " (demo scales this to 50,000 and prints ms)");
    }

    @Test
    @DisplayName("substring end is EXCLUSIVE and split takes REGEX — the two everyday traps")
    void slicingAndSplittingFacts() {
        String accountId = "ACC-1001";

        assertEquals("ACC", accountId.substring(0, 3), "begin inclusive, END EXCLUSIVE");
        assertEquals("1001", accountId.substring(4));
        assertEquals(3 - 0, accountId.substring(0, 3).length(), "substring(a,b).length() == b - a");

        String[] columns = "T-101,Payroll,2500.00".split(",");
        assertEquals(3, columns.length);
        assertEquals("T-101", columns[0]);
        assertEquals("Payroll", columns[1]);
        assertEquals("2500.00", columns[2]);

        // "." as a REGEX means "any character" — every char becomes a delimiter and
        // split drops the trailing empties, so NOTHING survives. The escape is mandatory.
        assertEquals(0, "2500.00".split(".").length, "unescaped dot destroys everything");
        assertTrue(Arrays.equals(new String[] { "2500", "00" }, "2500.00".split("\\.")));

        System.out.println("\"ACC-1001\".substring(0,3) = \"" + accountId.substring(0, 3) + "\" (end exclusive)");
        System.out.println("CSV split(\",\")            = " + Arrays.toString(columns));
        System.out.println("\"2500.00\".split(\".\")      = " + Arrays.toString("2500.00".split(".")) + "  <- the trap");
        System.out.println("\"2500.00\".split(\"\\\\.\")     = " + Arrays.toString("2500.00".split("\\.")));
    }

    @Test
    @DisplayName("compareTo is lexicographic; format builds the receipt line; isEmpty reads as the question")
    void comparisonAndFormattingFacts() {
        assertTrue("ACC-1001".compareTo("ACC-1002") < 0, "'1' (49) sorts before '2' (50)");
        assertEquals(0, "ACC-1001".compareTo("ACC-1001"));
        assertTrue("ACC-1002".compareTo("ACC-1001") > 0, "antisymmetric");

        String receipt = String.format("Receipt #%d  %s  amount %.2f", 101, "Payroll", 2500.00);
        assertEquals("Receipt #101  Payroll  amount 2500.00", receipt);

        assertEquals("101", String.valueOf(101));
        assertEquals("null", String.valueOf((Object) null), "valueOf is the null-safe toString");

        assertTrue("".isEmpty());
        assertFalse("ACC-1001".isEmpty());

        System.out.println("compareTo(\"ACC-1001\",\"ACC-1002\") = " + "ACC-1001".compareTo("ACC-1002") + " (negative = sorts first)");
        System.out.println("String.format(...) = \"" + receipt + "\"");
        System.out.println("String.join(\" | \", ...) = \"" + String.join(" | ", "ACC-1001", "Ada Lovelace", "ACTIVE") + "\"");
    }

    @Test
    @DisplayName("char[] can be zeroed; the String version of the same password cannot")
    void charArrayCanBeWipedStringCannot() {
        String asString = "password123";       // demo credential only
        char[] asChars = "password123".toCharArray();

        Arrays.fill(asChars, '\0');           // wipe NOW — deterministic

        // The char[] is gone...
        for (char c : asChars) {
            assertEquals('\0', c, "every char must be zeroed");
        }
        // ...and the String is still there, untouched, because NOTHING can touch it.
        assertEquals("password123", asString, "the String copy is unwipeable until GC collects it");

        System.out.println("char[] after Arrays.fill : " + Arrays.toString(asChars).replace("\0", "0"));
        System.out.println("String after 'wipe' try  : \"" + asString + "\" <- still readable, still in the heap");
        System.out.println("=> this asymmetry is why JPasswordField.getPassword() returns char[]");
    }
}
