package com.evaitcs.corejava.ordering;

/**
 * A SecureBank customer — exists for exactly one demo: SubtractionTrapDemo.
 *
 * WHY ages: `(o1.getAge() - o2.getAge())` WORKS for ages, because ages are small.
 * That is what makes the subtraction pattern seductive — it passes every test you
 * write on toy data, then flips its sign on timestamps and cents. Same pattern,
 * bigger numbers, broken sort.
 */
public class Customer {

    private final String name;
    private final int age;

    public Customer(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public String toString() {
        return name + " (" + age + ")";
    }
}
