package com.evaitcs.java8.streams;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * STREAMS ARE ONE-SHOT — the #1 runtime surprise in Java 8.
 *
 * A stream is a single-use ticket: exactly one terminal op, ever. The second terminal
 * op throws IllegalStateException — at RUNTIME, not compile time, which is what makes
 * it nasty. This demo catches it so the class sees the exact message.
 *
 * Mental model: a Collection is a jukebox (replay any song, any time). A Stream is a
 * water pipe — the water that went through is GONE; to run it again you turn the tap
 * on at the SOURCE again.
 */
public class OneShotDemo {

    public static void main(String[] args) {

        List<Transaction> statement = Transaction.sampleStatement();

        banner("1. First terminal op — works fine");

        Stream<Transaction> stream = statement.stream();
        List<String> deposits = stream
                .filter(Transaction::isDeposit)
                .map(Transaction::getDescription)
                .collect(Collectors.toList());
        System.out.println("first collect() = " + deposits);
        System.out.println("The stream is now CONSUMED. That was its one and only life.");

        banner("2. Second terminal op on the SAME stream — boom");

        try {
            long again = stream.count();
            System.out.println("this line is never reached: " + again);
        } catch (IllegalStateException e) {
            System.out.println("caught " + e.getClass().getSimpleName() + ": \"" + e.getMessage() + "\"");
            System.out.println("Memorize the message — you WILL meet it in production stack traces.");
        }

        banner("3. The fix — go back to the SOURCE and build a fresh stream");

        long depositCount = statement.stream()          // fresh stream from the same list
                .filter(Transaction::isDeposit)
                .count();
        System.out.println("statement.stream().filter(...).count() = " + depositCount
                + "   <- the list never wore out; only the stream did");

        banner("4. Where this bites in real code");

        System.out.println("TRAP: a helper that returns a Stream someone else already consumed.");
        System.out.println("      The bug surfaces far from its cause — classic action-at-a-distance.");
        System.out.println("Convention: methods should return a FRESH stream every call, e.g.");
        System.out.println("      Stream<Transaction> deposits() { return statement.stream().filter(Transaction::isDeposit); }");
        System.out.println("so every caller gets its own single-use ticket.");
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
