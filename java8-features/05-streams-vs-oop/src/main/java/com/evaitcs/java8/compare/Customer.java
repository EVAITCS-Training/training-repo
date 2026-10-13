package com.evaitcs.java8.compare;

/**
 * A SecureBank customer. Immutable, plain Java 8 — no records (those arrive in Java 16).
 *
 * WHY IMMUTABLE: streams shine when the data can't change under you. If a pipeline
 * stage mutated a Customer mid-iteration, both the loop version and the stream version
 * would misbehave — but the stream version would misbehave in ways that are much
 * harder to debug, because you never see the iteration happening.
 */
public final class Customer {

    private final String id;
    private final String name;
    private final int age;
    private final String city;

    public Customer(String id, String name, int age, String city) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.city = city;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getCity() { return city; }

    // equals/hashCode on id: tests compare List<Customer> results from the
    // imperative and stream implementations, and list equality walks element equals().
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        return id.equals(((Customer) o).id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return String.format("%s %-18s age %2d  %s", id, name, age, city);
    }
}
