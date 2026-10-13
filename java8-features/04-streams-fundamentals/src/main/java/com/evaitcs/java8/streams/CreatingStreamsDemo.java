package com.evaitcs.java8.streams;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * WHERE DO STREAMS COME FROM? A guided tour of every source you'll use in real code.
 *
 * The rule from demo 2: a pipeline starts with exactly ONE source. Everything here
 * produces that source. Nothing here computes anything yet — sources are lazy too.
 */
public class CreatingStreamsDemo {

    public static void main(String[] args) {

        banner("1. From a collection — the one you'll use 90% of the time");

        List<Customer> customers = Customer.sampleCustomers();
        List<String> names = customers.stream()
                .map(Customer::getName)
                .collect(Collectors.toList());
        System.out.println("customers.stream() -> " + names);
        // NOTE: .stream() does NOT copy the list. The stream reads the collection
        // live when pulled — mutate the list before the terminal op and you feel it.

        banner("2. From fixed values — Stream.of / Stream.empty");

        Stream<String> tiers = Stream.of("STANDARD", "GOLD", "PLATINUM");
        System.out.println("Stream.of(...)   -> " + tiers.collect(Collectors.toList()));

        Stream<Account> noAccounts = Stream.empty();
        System.out.println("Stream.empty()   -> count = " + noAccounts.count()
                + "   (the honest 'no results' — better than returning null)");

        banner("3. From an array — Arrays.stream");

        String[] codes = { "SWIFT-EVA", "IBAN-001", "BIC-XYZ" };
        System.out.println("Arrays.stream(array)      -> " + Arrays.stream(codes).count() + " elements");
        System.out.println("Arrays.stream(array, 1,3) -> "
                + Arrays.stream(codes, 1, 3).collect(Collectors.toList()) + "   (from/to slice)");

        banner("4. Infinite sources — generate and iterate");

        // generate: each element comes from calling the Supplier again. No memory of the past.
        List<String> auditIds = Stream.generate(() -> "AUDIT-" + System.nanoTime() % 1000)
                .limit(3)
                .collect(Collectors.toList());
        System.out.println("Stream.generate(supplier) -> " + auditIds.size() + " generated ids (limit(3)!)");

        // iterate: each element is a FUNCTION OF THE PREVIOUS ONE — seed, then f(seed), f(f(seed))...
        List<Integer> powers = Stream.iterate(1, n -> n * 2)
                .limit(8)
                .collect(Collectors.toList());
        System.out.println("Stream.iterate(1, n -> n*2) -> " + powers);

        // TRAP: an infinite stream without limit()/findFirst()/anyMatch() hangs forever.
        // The terminal op decides how much gets pulled — see demo 5 (short-circuiting).
        System.out.println("TRAP: infinite source + count() = a program that never ends. Always tame it first.");

        banner("5. Numeric streams — IntStream.range / rangeClosed");

        System.out.println("IntStream.range(1, 5)       -> "
                + IntStream.range(1, 5).boxed().collect(Collectors.toList()) + "   (EXCLUSIVE end, like a for-loop)");
        System.out.println("IntStream.rangeClosed(1, 5) -> "
                + IntStream.rangeClosed(1, 5).boxed().collect(Collectors.toList()) + "   (INCLUSIVE end)");
        System.out.println("rangeClosed(1, 100).sum()   -> " + IntStream.rangeClosed(1, 100).sum()
                + "   (primitive streams can sum/average directly — no boxing)");

        banner("6. Characters of a string — \"...\".chars()");

        // chars() returns an IntStream (not Stream<Character>) — Java 8 has no CharStream.
        // That's why the cast to (char) is needed when printing. A classic surprise.
        long vowels = "SecureBank".chars()
                .filter(c -> "aeiouAEIOU".indexOf(c) >= 0)
                .count();
        System.out.println("\"SecureBank\".chars() -> " + "SecureBank".chars().count() + " chars, " + vowels + " vowels");
        System.out.print("printed via forEach with a (char) cast: ");
        "EVA".chars().forEach(c -> System.out.print((char) c + " "));
        System.out.println();
    }

    private static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
