package com.evaitcs.java8.optional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Optional is a pipeline: map / filter / flatMap transform or narrow the value INSIDE the
 * box, and nothing after the first empty link ever runs. One missing hop short-circuits
 * the whole chain safely — that is act one's defensive if-wall, declared once, up front.
 *
 * The pipeline this class builds (flowchart is in the README):
 *
 *   Customer --findPrimaryAccountOf--> Optional<Account>
 *            --flatMap(getCreditLimit)--> Optional<BigDecimal>
 *            --filter(>= 1000.00)--> policy gate
 *            --map(format)--orElse(default)--> a human-readable decision
 */
public class ChainingDemo {

    /** Bank policy: a credit line is only usable if it is at least this big. */
    private static final BigDecimal MIN_LINE = new BigDecimal("1000.00");

    public static void main(String[] args) {
        AccountRepository repo = new AccountRepository();

        // ---------- map vs flatMap: the nesting trap ----------
        // getCreditLimit() ITSELF returns an Optional, so map() wraps it a second time:
        Optional<Optional<BigDecimal>> nested =
                repo.findPrimaryAccountOf(customer("Alice")).map(Account::getCreditLimit);
        System.out.println("map(getCreditLimit)    -> " + nested);
        System.out.println("  Optional<Optional<..>> is a useless type: the OUTER box is 'present' even when");
        System.out.println("  the inner one is empty. flatMap expects a function that returns an Optional");
        System.out.println("  and flattens one level — use flatMap whenever the mapper is itself a lookup.");

        Optional<BigDecimal> flat =
                repo.findPrimaryAccountOf(customer("Alice")).flatMap(Account::getCreditLimit);
        System.out.println("flatMap(getCreditLimit) -> " + flat + "   <- one box, one answer");
        System.out.println();

        // ---------- the same pipeline for all three customers ----------
        // Alice: account + 5000.00 limit -> approved.  Bob: account, NO limit -> short-circuits.
        // Dana: NO account at all -> short-circuits at the very first hop. No if-checks anywhere.
        for (Customer c : Customer.sampleCustomers()) {
            String decision = repo.findPrimaryAccountOf(c)                 // Optional<Account>
                    .flatMap(Account::getCreditLimit)                      // Optional<BigDecimal>
                    .filter(limit -> limit.compareTo(MIN_LINE) >= 0)       // policy gate
                    .map(limit -> "approved up to " + limit.toPlainString())
                    .orElse("no usable credit line");                      // terminal: value or default
            System.out.printf("%-14s -> %s%n", c.getName(), decision);
        }

        // ---------- ifPresent: a terminal for SIDE EFFECTS ----------
        System.out.println();
        repo.findPrimaryAccountOf(customer("Alice"))
                .flatMap(Account::getCreditLimit)
                .ifPresent(limit -> System.out.println("ifPresent: mailing Alice her new limit card for "
                        + limit.toPlainString()));
        repo.findPrimaryAccountOf(customer("Dana"))
                .ifPresent(a -> System.out.println("you will NEVER see this line"));
        System.out.println("(Dana has no account, so the ifPresent above silently skipped — no exception, no if-check.)");

        // ---------- filter on its own: keeps the value only if it passes ----------
        System.out.println();
        Optional<BigDecimal> aliceLimit = repo.findById("ACC-1001").flatMap(Account::getCreditLimit);
        System.out.println("5000.00.filter(>= 10000.00) -> "
                + aliceLimit.filter(l -> l.compareTo(new BigDecimal("10000.00")) >= 0)
                + "   (a PRESENT value that fails the predicate becomes empty)");
    }

    /** Looks up one of the three sample customers by first name — keeps main() readable. */
    private static Customer customer(String firstName) {
        for (Customer c : Customer.sampleCustomers()) {
            if (c.getName().startsWith(firstName)) {
                return c;
            }
        }
        throw new IllegalArgumentException("no such demo customer: " + firstName);
    }
}
