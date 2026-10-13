package com.evaitcs.java8.upgrades;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests as PROOF for StringJoinerDemo: the one-liners produce EXACTLY what the old
 * StringBuilder + setLength hack produced - including the edge cases.
 */
class StringJoinerTest {

    private String oldHack(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            sb.append(id).append(", ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2); // chop the trailing delimiter
        }
        return sb.toString();
    }

    @Test
    @DisplayName("String.join equals the old StringBuilder+setLength hack on every size")
    void stringJoinMatchesOldHack() {
        List<String> empty = new ArrayList<>();
        List<String> one = new ArrayList<>(); one.add("T-101");
        List<String> many = new ArrayList<>(); many.add("T-101"); many.add("T-102"); many.add("T-103");

        for (List<String> ids : java.util.Arrays.asList(empty, one, many)) {
            assertEquals(oldHack(ids), String.join(", ", ids), "must agree for " + ids);
        }
        System.out.println("String.join agreed with the setLength hack on sizes 0, 1 and 3");
        System.out.println("  many -> '" + String.join(", ", many) + "'");
    }

    @Test
    @DisplayName("StringJoiner builds delimiter+prefix+suffix exactly: 'TXN[ T-101, T-102 ]'")
    void stringJoinerPrefixSuffix() {
        StringJoiner joiner = new StringJoiner(", ", "TXN[ ", " ]");
        joiner.add("T-101").add("T-102");
        System.out.println("joiner -> '" + joiner + "'");
        assertEquals("TXN[ T-101, T-102 ]", joiner.toString());
    }

    @Test
    @DisplayName("empty StringJoiner prints prefix+suffix; setEmptyValue overrides that")
    void emptyJoinerEdgeCase() {
        StringJoiner empty = new StringJoiner(", ", "TXN[ ", " ]");
        System.out.println("empty default -> '" + empty + "'");
        assertEquals("TXN[  ]", empty.toString(), "prefix and suffix still print when nothing was added");

        StringJoiner withEmptyValue = new StringJoiner(", ", "TXN[ ", " ]").setEmptyValue("TXN[]");
        System.out.println("empty with setEmptyValue -> '" + withEmptyValue + "'");
        assertEquals("TXN[]", withEmptyValue.toString());
    }

    @Test
    @DisplayName("Collectors.joining (module 06) produces the SAME string as StringJoiner")
    void collectorsJoiningOverlap() {
        List<Transaction> statement = Transaction.sampleStatement();

        StringJoiner joiner = new StringJoiner(", ", "TXN[ ", " ]");
        for (Transaction t : statement) {
            joiner.add(t.getId());
        }
        String viaStream = statement.stream()
                .map(Transaction::getId)
                .collect(Collectors.joining(", ", "TXN[ ", " ]"));

        System.out.println("StringJoiner      -> '" + joiner + "'");
        System.out.println("Collectors.joining-> '" + viaStream + "'");
        assertEquals(joiner.toString(), viaStream, "joining uses a StringJoiner internally - must match");
    }
}
