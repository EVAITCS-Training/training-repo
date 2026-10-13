package com.evaitcs.java8.collectors;

/**
 * A SecureBank customer. Immutable, plain Java 8 — no records (those arrive in Java 16).
 *
 * WHY IMMUTABLE: collectors MUTATE their container, never the elements. If a collector
 * could also change what's inside the bucket, "collect twice, get the same answer" would
 * stop being true — and every downstream collector in this module leans on that.
 *
 * `city` is THE field of this module: it is the duplicate key that explodes toMap, the
 * classifier for every groupingBy, and the report header students actually care about.
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

    // equals/hashCode on id: tests compare collected buckets (List<Customer>) and list
    // equality walks element equals(). Same convention as module 05.
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
