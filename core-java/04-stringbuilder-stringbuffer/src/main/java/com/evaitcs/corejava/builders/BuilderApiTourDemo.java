package com.evaitcs.corejava.builders;

import java.math.BigDecimal;

/**
 * The whole StringBuilder API you will actually touch, in one pass.
 * Point at a section, PREDICT the output out loud, then run it.
 *
 * Every word here applies equally to StringBuffer: both classes inherit their
 * methods from AbstractStringBuilder, so the two share every signature.
 * The ONLY difference is locking — BufferVsBuilderThreadDemo covers that.
 */
public class BuilderApiTourDemo {

    public static void main(String[] args) {

        banner("1. append — one overload per type, and it returns THIS");
        StringBuilder audit = new StringBuilder();
        audit.append("AUDIT ");                       // append(String)
        audit.append('[');                            // append(char)
        audit.append(4200);                           // append(int)
        audit.append(" -> ");
        audit.append(new BigDecimal("1850.00"));      // append(Object): calls toString() on it.
        // There is an append(double) too, but module 02's rule stands: money is BigDecimal.
        audit.append(" frozen=").append(true);         // append(boolean), chained
        audit.append(']');
        System.out.println(audit);

        // Why does sb.append(a).append(b).append(c) work without repeating the variable?
        // Every append ends with `return this;` — it hands you back the SAME builder.
        StringBuilder returned = audit.append(" tail");
        System.out.println("append returned the SAME object? " + (returned == audit)
                + "   <- fluent chaining is just `return this`");

        banner("2. insert / replace / delete / deleteCharAt — in-place surgery");
        StringBuilder line = new StringBuilder("TRANSFER -1,850.00 USD");
        line.insert(0, "2026-09-03 ");                // shift everything right, write at offset 0
        System.out.println("insert(0, date)          : " + line);
        line.replace(11, 19, "PAYMENT");              // [start, end) like substring — length may change!
        System.out.println("replace(11,19,PAYMENT)   : " + line);
        line.delete(19, line.length());               // drop the amount and currency
        System.out.println("delete(19, length)       : [" + line + "]  (trailing space left behind)");
        line.deleteCharAt(line.length() - 1);         // remove ONE char — here, that space
        System.out.println("deleteCharAt(length - 1) : [" + line + "]");

        banner("3. reverse — the interview classic");
        StringBuilder name = new StringBuilder("SecureBank");
        name.reverse();                               // swaps chars in place, O(n/2) swaps, no new object
        System.out.println("reverse(\"SecureBank\") = " + name);
        System.out.println("(on a real interview: this is the one-liner they're fishing for)");

        banner("4. charAt / setCharAt — single-char read and write");
        StringBuilder masked = new StringBuilder("SecureBank");
        System.out.println("charAt(0)                : " + masked.charAt(0));
        masked.setCharAt(6, '*');                     // mask the 'B' in place — no new object
        System.out.println("after setCharAt(6, '*')  : " + masked);

        banner("5. setLength — truncate AND pad (the padding is the trap)");
        StringBuilder sb = new StringBuilder("SecureBank");
        sb.setLength(6);                              // shorter: extra chars are simply forgotten
        System.out.println("setLength(6)             : [" + sb + "]  <- truncated, no error thrown");
        sb.setLength(10);                             // longer: pads with '\u0000' — NOT with spaces!
        System.out.println("setLength(10)            : [" + visible(sb.toString()) + "]  length=" + sb.length());
        System.out.println("TRAP: the 4 pad chars are NUL (\\u0000). Invisible in logs, they");
        System.out.println("      corrupt file formats and make equals() fail mysteriously.");

        banner("6. length() vs capacity() — used seats vs seats in the bus");
        StringBuilder seats = new StringBuilder("AUDIT");
        System.out.println("length()                 : " + seats.length() + "   <- chars actually used");
        System.out.println("capacity()               : " + seats.capacity() + "  <- slots in the char[] (5 + 16 default headroom)");
        seats.ensureCapacity(500);                    // grow ONCE, deliberately, instead of many auto-grows
        System.out.println("after ensureCapacity(500): " + seats.capacity());
        System.out.println("(the growth rule 2n+2 gets its own demo next: CapacityGrowthDemo)");

        banner("7. toString() hands back an immutable SNAPSHOT");
        StringBuilder balance = new StringBuilder("BALANCE 4200.00");
        String snapshot = balance.toString();         // copies the used chars into a brand-new String
        balance.append("  (then frozen)");            // the builder keeps mutating...
        System.out.println("snapshot : " + snapshot); // ...the snapshot is untouched — it's a real String
        System.out.println("builder  : " + balance);
    }

    /**
     * Renders invisible chars so the projector shows them: NUL becomes the text \0.
     * (Yes, this helper uses a StringBuilder to explain StringBuilders — that's how
     * natural the idiom is: build char-by-char into one mutable buffer.)
     */
    static String visible(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(c == '\u0000' ? "\\0" : String.valueOf(c));
        }
        return out.toString();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
