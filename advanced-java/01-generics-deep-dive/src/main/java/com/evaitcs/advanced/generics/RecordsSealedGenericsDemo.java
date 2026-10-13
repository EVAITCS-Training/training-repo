package com.evaitcs.advanced.generics;

import java.math.BigDecimal;
import java.util.List;

/**
 * Demo 5 — GENERICS ON JAVA 21: records, sealed types, pattern matching.
 * (README section "Java 21 corner".)
 *
 * The deep rules never changed — records erase exactly like classes, wildcards still
 * follow PECS. What changed is how LITTLE ceremony the same guarantees need:
 *   - a record gives you the generic value type in one line (Envelope<T>),
 *   - a sealed interface closes the family so a switch can EXHAUST it,
 *   - pattern matching turns the instanceof-and-cast ladder into the switch itself.
 *
 * Contrast with core-java (release 8, classic idiom on purpose): there, this page of
 * code was a wall of classes. Here it is eleven lines. That contrast is the lesson.
 */
public class RecordsSealedGenericsDemo {

    /** A generic record: T erases like any class type parameter — no special rules. */
    record Envelope<T>(T payload, String trackingId) { }

    /** Sealed family: the compiler KNOWS every implementation, so switches can be exhaustive. */
    sealed interface Payment permits CardPayment, TransferPayment { }

    record CardPayment(String last4, BigDecimal amount) implements Payment { }
    record TransferPayment(String iban, BigDecimal amount) implements Payment { }

    public static void main(String[] args) {
        banner("1. A generic record in one line");

        Envelope<Transaction> parcel = new Envelope<>(
                new Transaction("T-1040", "Payroll", new BigDecimal("2450.00")), "TRACK-01");
        System.out.println("envelope = " + parcel);
        System.out.println("payload() is ALREADY a Transaction — no cast at the call site,");
        System.out.println("because the record's component is declared T. (Erasure still inserts");
        System.out.println("the cast inside the generated accessor — the compiler does it for you.)");
        System.out.println("erasure check: Envelope<Transaction> and Envelope<String> same class? "
                + (parcel.getClass() == new Envelope<>("x", "T-2").getClass()));

        banner("2. Sealed + generics + pattern-matching switch: exhaustive, no default needed");

        List<Payment> batch = List.of(
                new CardPayment("4242", new BigDecimal("86.40")),
                new TransferPayment("NL93-ABNA-0001", new BigDecimal("1200.00")));

        BigDecimal total = BigDecimal.ZERO;
        for (Payment p : batch) {
            // The switch EXHAUSTS the sealed family: add a third implementation tomorrow
            // and this code stops compiling until you handle it. That is a compiler-enforced
            // to-do list — the classic Visitor pattern's main benefit, with zero pattern classes.
            BigDecimal amount = switch (p) {
                case CardPayment c -> c.amount();
                case TransferPayment t -> t.amount();
            };
            total = total.add(amount);
        }
        System.out.println("batch total = " + total.toPlainString());

        banner("3. Wildcards + records in one pipeline");

        // List<? extends Envelope<?>> — "some list of SOME envelopes": fully general PRODUCER.
        // We only READ tracking ids out, so PECS says extends — and no type variable needed at all.
        System.out.println("tracking ids = " + trackingIds(List.of(parcel, new Envelope<>("memo", "TRACK-02"))));

        banner("4. In one sentence");
        System.out.println("Java 21 did not change the generic TYPE SYSTEM — it removed the ceremony");
        System.out.println("around it, and added exhaustiveness (sealed + switch) as a new compile-time guarantee.");
    }

    /** Wildcard-only method: reads tracking ids out of ANY list of ANY envelopes (PECS: producer extends). */
    static List<String> trackingIds(List<? extends Envelope<?>> envelopes) {
        return envelopes.stream().map(Envelope::trackingId).toList();   // streams: java8-features 04
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
