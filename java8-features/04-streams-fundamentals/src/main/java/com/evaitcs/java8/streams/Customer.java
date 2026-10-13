package com.evaitcs.java8.streams;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * SecureBank domain — a customer of the bank.
 *
 * Java 8 only: no records, so an immutable class with getters.
 * memberSince is a LocalDate from java.time (module 08 covers why Date/Calendar died).
 */
public final class Customer {

    private final String id;
    private final String name;
    private final String tier;              // STANDARD / GOLD / PLATINUM
    private final LocalDate memberSince;

    public Customer(String id, String name, String tier, LocalDate memberSince) {
        this.id = id;
        this.name = name;
        this.tier = tier;
        this.memberSince = memberSince;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTier() { return tier; }
    public LocalDate getMemberSince() { return memberSince; }

    @Override
    public String toString() {
        return String.format("%s %-18s %-8s member since %s", id, name, tier, memberSince);
    }

    /** A fixed roster so every demo works with the same customers. */
    public static List<Customer> sampleCustomers() {
        return Arrays.asList(
                new Customer("C-01", "Ada Lovelace",       "PLATINUM", LocalDate.of(2015, 3, 12)),
                new Customer("C-02", "Grace Hopper",       "GOLD",     LocalDate.of(2016, 7, 1)),
                new Customer("C-03", "Alan Turing",        "GOLD",     LocalDate.of(2017, 11, 5)),
                new Customer("C-04", "Katherine Johnson",  "PLATINUM", LocalDate.of(2014, 1, 20)));
    }
}
