package com.evaitcs.corejava.builders;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests as PROOF for the mutation claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 04-stringbuilder-stringbuffer test
 * Each test proves ONE claim and prints the evidence it asserts.
 */
class BuilderMutationTest {

    @Test
    @DisplayName("append mutates the SAME object — no new String is born, and it returns `this`")
    void appendMutatesSameObject() {
        StringBuilder audit = new StringBuilder("AUDIT");
        int identityBefore = System.identityHashCode(audit);

        StringBuilder returned = audit.append(" ACC-1042 FROZEN");

        int identityAfter = System.identityHashCode(audit);
        System.out.println("identityHashCode before append : " + identityBefore);
        System.out.println("identityHashCode after append  : " + identityAfter);
        System.out.println("content                        : " + audit);

        assertSame(audit, returned, "append must return `this` — that is all fluent chaining is");
        assertEquals(identityBefore, identityAfter, "append must mutate in place, never recreate");
        assertEquals("AUDIT ACC-1042 FROZEN", audit.toString());
    }

    @Test
    @DisplayName("insert/replace/delete/deleteCharAt are [start, end) surgery on the same buffer")
    void rangeSurgery() {
        StringBuilder line = new StringBuilder("TRANSFER -1,850.00 USD");
        line.insert(0, "2026-09-03 ");
        System.out.println("after insert(0, date)        : " + line);
        assertEquals("2026-09-03 TRANSFER -1,850.00 USD", line.toString());

        line.replace(11, 19, "PAYMENT");
        System.out.println("after replace(11,19,PAYMENT) : " + line);
        assertEquals("2026-09-03 PAYMENT -1,850.00 USD", line.toString());

        line.delete(19, line.length());
        System.out.println("after delete(19, length)     : [" + line + "]");
        assertEquals("2026-09-03 PAYMENT ", line.toString());

        line.deleteCharAt(line.length() - 1);
        System.out.println("after deleteCharAt(last)     : [" + line + "]");
        assertEquals("2026-09-03 PAYMENT", line.toString());
    }

    @Test
    @DisplayName("reverse — the interview classic: reverse(\"SecureBank\") = \"knaBeruceS\"")
    void reverseClassic() {
        String reversed = new StringBuilder("SecureBank").reverse().toString();
        System.out.println("new StringBuilder(\"SecureBank\").reverse() = " + reversed);
        assertEquals("knaBeruceS", reversed);
    }

    @Test
    @DisplayName("setLength truncates AND pads — but pads with \\u0000, NOT with spaces")
    void setLengthTruncatesAndPads() {
        StringBuilder sb = new StringBuilder("SecureBank");

        sb.setLength(6);                                   // shorter: extra chars are forgotten
        System.out.println("setLength(6)  : [" + sb + "]");
        assertEquals("Secure", sb.toString());

        sb.setLength(10);                                  // longer: padded with NUL characters
        System.out.println("setLength(10) : [" + BuilderApiTourDemo.visible(sb.toString()) + "]");
        assertEquals(10, sb.length(), "length is 10 again...");
        assertEquals("Secure", sb.substring(0, 6), "...but only the first 6 are the old text");
        for (int i = 6; i < 10; i++) {
            assertEquals('\u0000', sb.charAt(i), "pad char at index " + i + " must be NUL, not ' '");
        }
    }

    @Test
    @DisplayName("toString() hands back an immutable SNAPSHOT — later appends never touch it")
    void toStringIsASnapshot() {
        StringBuilder balance = new StringBuilder("BALANCE 4200.00");
        String snapshot = balance.toString();              // copies chars into a brand-new String
        balance.append("  (then frozen)");                 // the builder keeps mutating

        System.out.println("snapshot : " + snapshot);
        System.out.println("builder  : " + balance);
        assertEquals("BALANCE 4200.00", snapshot, "the snapshot must not see later mutations");
        assertEquals("BALANCE 4200.00  (then frozen)", balance.toString());
    }

    @Test
    @DisplayName("a single '+' expression and a hand-rolled builder produce identical strings")
    void plusExpressionEqualsBuilder() {
        String account = "ACC-1042";
        BigDecimal amount = new BigDecimal("1850.00");

        String withPlus = "TRANSFER " + account + " " + amount;
        String withBuilder = new StringBuilder("TRANSFER ")
                .append(account).append(' ').append(amount)
                .toString();

        System.out.println("with +      : " + withPlus);
        System.out.println("with builder: " + withBuilder);
        assertEquals(withPlus, withBuilder);
        // The '+' version compiles to a builder anyway (javap -c proves it), so for a
        // SINGLE expression the choice is style, not speed. The trap is '+' in a LOOP.
    }
}
