package com.evaitcs.corejava.strings;

/**
 * Claim #4: `+` is fine in ONE expression and a disaster in a LOOP.
 *
 * THE DECOMPILED STORY to tell while pointing at the code: javac translates
 *
 *     String s = "a" + b + "c";
 *
 * into (conceptually — this is what `javap -c` shows):
 *
 *     String s = new StringBuilder().append("a").append(b).append("c").toString();
 *
 * One expression => one StringBuilder => perfectly fine. But a loop body is
 * compiled like any other expression, so javac re-creates that StringBuilder on
 * EVERY iteration — and every `+=` produces a brand-new String holding a full
 * COPY of everything so far. That is the trap this demo times.
 *
 * Module 04 (StringBuilder/StringBuffer) owns the fix in full; here it gets one
 * block, just enough to feel the difference.
 */
public class ConcatCostDemo {

    private static final int TARGET_LENGTH = 50_000; // a 50k-char statement detail line

    public static void main(String[] args) {

        banner("1. The trap: build the 50,000-char line with +=");

        String line = "";
        long startPlus = System.nanoTime();
        for (int i = 0; i < TARGET_LENGTH; i++) {
            line += digit(i);   // each iteration: new StringBuilder, new String, full copy
        }
        long plusMillis = (System.nanoTime() - startPlus) / 1_000_000;

        System.out.println("+= loop  : length " + line.length() + ", took " + plusMillis + " ms");
        System.out.println("           ~" + TARGET_LENGTH + " throwaway String objects (one per iteration),");
        System.out.println("           ~1.25 BILLION chars copied in total: iteration k copies k chars,");
        System.out.println("           so the work is 1+2+3+...+50000 = O(n^2). The line is quadratic.");

        banner("2. The fix: ONE mutable buffer (module 04 will take this apart properly)");

        StringBuilder sb = new StringBuilder();
        long startBuilder = System.nanoTime();
        for (int i = 0; i < TARGET_LENGTH; i++) {
            sb.append(digit(i)); // mutates the SAME object; copies only when it grows
        }
        String built = sb.toString(); // one final String, at the very end
        long builderMillis = (System.nanoTime() - startBuilder) / 1_000_000;

        System.out.println("builder  : length " + built.length() + ", took " + builderMillis + " ms");
        System.out.println("           1 buffer, amortized O(n) — append is cheap because nothing");
        System.out.println("           is immutable in there: it just writes into spare capacity.");

        banner("3. Same result — the timing is the only difference");

        System.out.println("line.equals(built) : " + line.equals(built) + "   <- identical text, wildly different cost");
        System.out.println("first 30 chars     : " + line.substring(0, 30) + "...");
        System.out.println("=> The tests for this module assert ONLY that both produce equal strings;");
        System.out.println("   timings print for the show but are never asserted (a slow laptop is not a bug).");
    }

    /** Digits 0-9 repeating — stands in for a long run of statement detail characters. */
    private static char digit(int i) {
        return (char) ('0' + (i % 10));
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
