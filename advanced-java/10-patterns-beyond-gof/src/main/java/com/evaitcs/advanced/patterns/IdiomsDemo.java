package com.evaitcs.advanced.patterns;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Demo C2 — API IDIOMS: the small recurring shapes that make Java APIs read well.
 * (README section "API idioms: fluent, factories, composition, Demeter, null handling".)
 *
 * These are not GoF patterns — they are below that radar: habits of good API design
 * that the JDK itself follows so consistently you can PREDICT an unfamiliar API's
 * shape. Five scenes, one banner each:
 *   1. fluent interfaces (module 09's Builder seen from the API side),
 *   2. static factory methods everywhere (valueOf/of/from — a table),
 *   3. composition over inheritance (has-a + delegation; Decorator's daily-life form),
 *   4. the Law of Demeter (train wreck vs one hop — the test simulates a restructure),
 *   5. Null Object vs Optional (java8-features 03, from the API-design side).
 */
public class IdiomsDemo {

    // ---- scene 1: fluent interface ----

    public record WireRequest(String fromId, String toId, BigDecimal amount, String reference) { }

    /** Fluent: every setter returns this, so calls chain into something readable. */
    public static final class WireRequestBuilder {
        private String fromId;
        private String toId;
        private BigDecimal amount;
        private String reference = "AUTO";

        public WireRequestBuilder fromId(String fromId) {
            this.fromId = fromId;
            return this;
        }

        public WireRequestBuilder toId(String toId) {
            this.toId = toId;
            return this;
        }

        public WireRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public WireRequestBuilder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public WireRequest build() {
            return new WireRequest(fromId, toId, amount, reference);
        }
    }

    // ---- scene 3: composition over inheritance ----

    public interface StatementFormatter {
        String format(String id, BigDecimal amount);
    }

    /** Formatters as records: stateless strategies, one line each (module 09 tie). */
    public record CsvFormatter() implements StatementFormatter {
        @Override
        public String format(String id, BigDecimal amount) {
            return id + ";" + amount.toPlainString();
        }
    }

    public record JsonFormatter() implements StatementFormatter {
        @Override
        public String format(String id, BigDecimal amount) {
            return "{\"id\":\"" + id + "\",\"amount\":\"" + amount.toPlainString() + "\"}";
        }
    }

    /** HAS-A, not IS-A: the printer OWNS a formatter and delegates to it. */
    public static final class StatementPrinter {
        private final StatementFormatter formatter;

        public StatementPrinter(StatementFormatter formatter) {
            this.formatter = formatter;
        }

        public String print(String id, BigDecimal amount) {
            return "PRINTED[" + formatter.format(id, amount) + "]";
        }
    }

    // ---- scene 4: Law of Demeter ----

    static final class Address {
        private final String city;

        Address(String city) {
            this.city = city;
        }

        public String getCity() {
            return city;
        }
    }

    static final class BankAccount {
        private final Address address;

        BankAccount(Address address) {
            this.address = address;
        }

        public Address getAddress() {
            return address;
        }
    }

    /** TODAY's customer: offers BOTH the train wreck's hops and the one-hop city(). */
    static final class Customer {
        private final BankAccount account;

        Customer(BankAccount account) {
            this.account = account;
        }

        public BankAccount getAccount() {
            return account;
        }

        /** The Demeter-clean accessor: ONE hop, internals hidden. */
        public String city() {
            return account.getAddress().getCity();
        }
    }

    /**
     * AFTER the restructure: the bank moved addresses into customer profiles.
     * There is NO getAccount and NO getAddress anymore — the train-wreck call sites
     * stop compiling, while every city() call site is untouched. The test proves
     * both halves of that claim (including a NoSuchMethodException on getAccount).
     */
    record CustomerProfile(String city, String memberSince) { }

    static final class ReorganizedCustomer {
        private final CustomerProfile profile;

        ReorganizedCustomer(CustomerProfile profile) {
            this.profile = profile;
        }

        public String city() {                       // same public promise, different internals
            return profile.city();
        }
    }

    // ---- scene 5: Null Object ----

    /** The collaborator interface; call sites just CALL it — no null checks anywhere. */
    public interface AuditSink {
        void record(String entry);

        String lastEntry();

        int size();
    }

    public static final class ListAuditSink implements AuditSink {
        private final List<String> entries = new ArrayList<>();

        @Override
        public void record(String entry) {
            entries.add(entry);
        }

        @Override
        public String lastEntry() {
            return entries.isEmpty() ? "(none)" : entries.get(entries.size() - 1);
        }

        @Override
        public int size() {
            return entries.size();
        }
    }

    /** The Null Object: same interface, every method a harmless, EXACT default. */
    public static final class NullAuditSink implements AuditSink {
        @Override
        public void record(String entry) {
            // deliberately silent: auditing is switched off in this deployment
        }

        @Override
        public String lastEntry() {
            return "(none)";
        }

        @Override
        public int size() {
            return 0;
        }
    }

    public static void main(String[] args) {
        banner("1. Fluent interface: the chain reads like the sentence");

        WireRequest request = new WireRequestBuilder()
                .fromId("ACC-1").toId("ACC-2")
                .amount(new BigDecimal("250.00"))
                .reference("PAYROLL-JAN")
                .build();
        System.out.println("built = " + request);
        System.out.println("Each setter returns this — module 09's Builder seen from the API side.");
        System.out.println("Trap: fluent on MUTATORS hides state changes (obj.setA(1).setB(2) — did");
        System.out.println("      setA return a NEW object or mutate? the name must tell the truth).");

        banner("2. Static factories everywhere: predict any JDK API from four names");

        System.out.printf("  %-12s | %-42s | %s%n", "valueOf", "Integer.valueOf, BigDecimal.valueOf", "convert exactly what is given");
        System.out.printf("  %-12s | %-42s | %s%n", "of", "List.of, Map.of, Set.of, EnumSet.of", "collect the arguments into one");
        System.out.printf("  %-12s | %-42s | %s%n", "from", "Instant.from, PaymentFactory.from (module 09)", "extract/decide from a source");
        System.out.printf("  %-12s | %-42s | %s%n", "getInstance", "Runtime.getRuntime, Calendar.getInstance", "the singleton-ish classic");
        System.out.println("This module follows suit: Money.of(\"2.50\") — MoneyValueObjectDemo's factory.");
        System.out.println("Why not constructors? Names carry meaning, factories can cache, validate,");
        System.out.println("and return subtypes — new Money(...) cannot decide anything.");

        banner("3. Composition over inheritance: has-a beats is-a");

        StatementPrinter csvPrinter = new StatementPrinter(new CsvFormatter());
        StatementPrinter jsonPrinter = new StatementPrinter(new JsonFormatter());
        System.out.println("step 1: csv  printer -> " + csvPrinter.print("TXN-1", new BigDecimal("2450.00")));
        System.out.println("step 2: json printer -> " + jsonPrinter.print("TXN-1", new BigDecimal("2450.00")));
        System.out.println("step 3: SAME StatementPrinter class — the behavior swapped at construction.");
        System.out.println("Why not extends CsvPrinter? Inheritance freezes the choice at COMPILE time,");
        System.out.println("and the fragile base class problem means a parent's change ripples down.");
        System.out.println("Composition = runtime choice + one job per class. Module 09's Decorator is");
        System.out.println("this same has-a shape, used to STACK behavior instead of swap it.");

        banner("4. Law of Demeter: only talk to your immediate friends");

        Customer customer = new Customer(new BankAccount(new Address("Amsterdam")));
        String trainWreck = customer.getAccount().getAddress().getCity();
        String oneHop = customer.city();
        System.out.println("train wreck : customer.getAccount().getAddress().getCity() = " + trainWreck);
        System.out.println("one hop     : customer.city()                              = " + oneHop);
        System.out.println("Both compile. Both return Amsterdam TODAY. The difference shows up when the");
        System.out.println("bank restructures: addresses move into profiles, getAccount()/getAddress()");
        System.out.println("disappear — every train-wreck call site in the codebase breaks at once,");
        System.out.println("while every city() call site never notices. The test simulates exactly that.");
        System.out.println("Rule: a method may call its OWN object, its parameters, and what it creates —");
        System.out.println("      not the friends-of-friends a long chain hands it.");

        banner("5. Null Object vs Optional: two honest ways to say 'nothing'");

        AuditSink off = new NullAuditSink();
        AuditSink on = new ListAuditSink();
        for (AuditSink sink : List.of(off, on)) {          // identical call sites — no null checks
            sink.record("transfer TXN-1");
            sink.record("transfer TXN-2");
        }
        System.out.println("NullAuditSink : size = " + off.size() + ", lastEntry = " + off.lastEntry());
        System.out.println("ListAuditSink : size = " + on.size() + ", lastEntry = " + on.lastEntry());
        System.out.println("Null Object  : a real object whose behavior is 'harmless nothing' — best for");
        System.out.println("               injected collaborators (logging off, preview mode, test doubles).");
        System.out.println("Optional     : a BOX that forces the caller to handle absence — best for");
        System.out.println("               RETURN types at API boundaries (java8-features 03).");
        System.out.println("Trap: a Null Object hides that nothing happened — for a COMPLIANCE audit,");
        System.out.println("      silence is dangerous; prefer Optional or an exception at that boundary.");

        banner("6. In one sentence");
        System.out.println("API idioms are small repeated promises — fluent reads, factories decide,");
        System.out.println("composition swaps, Demeter insulates, and 'nothing' gets a typed, honest shape.");
    }

    /** Kept to show the Optional side of scene 5 compiles the same promise as the Null Object. */
    static String lastEntryOf(Optional<AuditSink> maybeSink) {
        return maybeSink.map(AuditSink::lastEntry).orElse("(none)");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
