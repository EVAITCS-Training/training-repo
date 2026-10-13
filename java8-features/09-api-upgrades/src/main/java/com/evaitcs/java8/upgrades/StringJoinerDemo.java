package com.evaitcs.java8.upgrades;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * String.join and StringJoiner - the end of the "trailing delimiter" hack.
 *
 * WHY this existed: before Java 8, joining strings with a delimiter meant a StringBuilder
 * and either an `if (i > 0) append(delimiter)` guard inside the loop, or appending after
 * every element and then CHOPPING the last delimiter off with setLength. Everyone had one
 * of these two hacks; StringJoiner deletes both.
 */
public class StringJoinerDemo {

    public static void main(String[] args) {
        List<Transaction> statement = Transaction.sampleStatement();
        List<String> ids = new ArrayList<>();
        for (Transaction t : statement) {
            ids.add(t.getId());
        }

        System.out.println("=== The OLD StringBuilder + setLength hack (Java 7) ===");
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            sb.append(id).append(", ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);   // chop the trailing ", " - THE classic hack
        }
        System.out.println("   " + sb);

        System.out.println("=== String.join - the one-liner (static, new in Java 8) ===");
        // Signature to point at: join(CharSequence delimiter, Iterable<? extends CharSequence>)
        // There is also a varargs form: String.join(",", "T-101", "T-102").
        String joined = String.join(", ", ids);
        System.out.println("   " + joined);
        System.out.println("   same result, zero bookkeeping: " + sb.toString().equals(joined));

        System.out.println("=== StringJoiner - delimiter AND prefix AND suffix ===");
        // When join needs decoration, String.join is not enough. StringJoiner is the object
        // version: you add() pieces whenever you like, toString() assembles them at the end.
        StringJoiner joiner = new StringJoiner(", ", "TXN[ ", " ]");
        for (String id : ids) {
            joiner.add(id);
        }
        System.out.println("   " + joiner);   // -> TXN[ T-101, T-102, ... ]

        System.out.println("=== StringJoiner edge case: NOTHING was added ===");
        StringJoiner empty = new StringJoiner(", ", "TXN[ ", " ]");
        System.out.println("   default      : '" + empty + "'  (prefix+suffix still print!)");
        empty.setEmptyValue("TXN[]");          // opt-in fix for the empty case
        System.out.println("   setEmptyValue: '" + empty + "'");

        System.out.println("=== Overlap with streams (module 06) ===");
        // Collectors.joining(", ", "TXN[ ", " ]") produces EXACTLY this - in fact it uses a
        // StringJoiner internally. Rule of thumb:
        //   looping anyway          -> String.join / StringJoiner
        //   already in a stream     -> Collectors.joining
        String viaStream = statement.stream()
                .map(Transaction::getId)
                .collect(java.util.stream.Collectors.joining(", ", "TXN[ ", " ]"));
        System.out.println("   via Collectors.joining: " + viaStream);
        System.out.println("   identical to StringJoiner output: " + viaStream.equals(joiner.toString()));
    }
}
