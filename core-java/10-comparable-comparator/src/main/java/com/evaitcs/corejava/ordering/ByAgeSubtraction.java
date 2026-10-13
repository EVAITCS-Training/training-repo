package com.evaitcs.corejava.ordering;

import java.util.Comparator;

/**
 * Subtraction on SMALL numbers — it works, and that is exactly the problem.
 *
 * Ages fit comfortably in an int, so `o1.getAge() - o2.getAge()` sorts customers
 * correctly every time you test it. Then the same pattern gets copy-pasted onto
 * timestamps or cents (see ByTimestampSubtractionBroken) where the difference no
 * longer fits, and the sign flips. The pattern's danger is invisible on toy data.
 *
 * java8-features/01-lambdas: Comparator.comparingInt(Customer::getAge) replaces this
 */
public class ByAgeSubtraction implements Comparator<Customer> {

    @Override
    public int compare(Customer c1, Customer c2) {
        return c1.getAge() - c2.getAge(); // fine HERE (ages are small) — still a bad habit
    }
}
