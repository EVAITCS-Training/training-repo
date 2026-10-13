package com.evaitcs.corejava.builders;

/**
 * WHY StringBuilder exists — module 03's concat trap in five lines, then the mental
 * picture that carries the rest of the module: SAME object, GROWING array.
 *
 * Domain: assembling a SecureBank statement line by line — the most common
 * string-building shape in real code.
 */
public class WhyBuildersDemo {

    /** A tiny statement to assemble. Real code would loop over a List of Transactions. */
    static final String[] STATEMENT_LINES = {
            "2026-09-01  PAYROLL DIRECT DEP   +5,400.00",
            "2026-09-03  RENT TRANSFER        -1,850.00",
            "2026-09-05  GROCERY MART           -86.40",
            "2026-09-07  INTEREST CREDIT          +3.11",
            "2026-09-09  CARD PAYMENT            -420.00",
    };

    public static void main(String[] args) {

        banner("1. The trap, recapped (module 03 in five lines)");
        // 1. String is IMMUTABLE — its char[] is fixed the moment it is constructed.
        // 2. So `result += line` cannot append to result; it builds a WHOLE NEW String.
        // 3. The intermediate String from the previous iteration is instantly garbage.
        // 4. N iterations => N throwaway Strings => every char copied again and again (O(n^2)).
        // 5. StringBuilder flips this: ONE mutable char buffer; append() writes into it.

        String result = "";
        int charsCopied = 0;
        for (String line : STATEMENT_LINES) {
            result += line + "\n";              // <-- a brand-new String object is born HERE, every pass
            charsCopied += result.length();     // ...and every birth re-copies everything built so far
        }
        System.out.println("+= produced " + result.length() + " chars, but copied " + charsCopied
                + " chars total and threw away " + STATEMENT_LINES.length + " intermediate Strings");

        banner("2. Same job, StringBuilder — ONE object the whole way");
        StringBuilder statement = new StringBuilder();
        int identityBefore = System.identityHashCode(statement); // the object's "address", roughly
        for (String line : STATEMENT_LINES) {
            statement.append(line).append('\n'); // writes into the builder's own array — nothing is recreated
        }
        int identityAfter = System.identityHashCode(statement);

        System.out.println("identityHashCode BEFORE the loop : " + identityBefore);
        System.out.println("identityHashCode AFTER the loop  : " + identityAfter);
        System.out.println("SAME object? " + (identityBefore == identityAfter)
                + "   <- only the array inside grew");
        System.out.println("both versions equal? " + result.equals(statement.toString()));

        banner("3. The mental picture (draw this on the whiteboard)");
        System.out.println("  statement --> [ '2' '0' '2' '6' '-' '0' '9' ... '0'  _  _  _  _ ]");
        System.out.println("                |<--------- length (chars used) -------->|");
        System.out.println("                |<----------- capacity (array size) ------------>|");
        System.out.println();
        System.out.println("  append() writes into the SPARE slots — no new object.");
        System.out.println("  Only when the array is FULL is it replaced by a bigger one");
        System.out.println("  (2n+2 slots, everything copied across — CapacityGrowthDemo).");
        System.out.println("  toString() finally FREEZES the used chars into an immutable String.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
