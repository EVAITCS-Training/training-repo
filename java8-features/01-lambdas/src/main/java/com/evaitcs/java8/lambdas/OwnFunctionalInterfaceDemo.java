package com.evaitcs.java8.lambdas;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Writing your OWN functional interface — because "functional interface" is not magic,
 * it is just: an interface with exactly ONE abstract method (a "SAM" — Single Abstract Method).
 *
 * @FunctionalInterface is optional but recommended: it turns "accidentally added a second
 * abstract method six months later" into a compile error instead of a broken API.
 */
public class OwnFunctionalInterfaceDemo {

    /**
     * A money-checking policy. One abstract method => lambdas can implement it.
     * Notice we also get to name the concept ("MoneyPolicy") — good API design:
     * callers read `transfer(amount, MoneyPolicy)` and know a lambda belongs there.
     */
    @FunctionalInterface
    interface MoneyPolicy {
        boolean allows(BigDecimal amount);
        // An interface may still have default/static methods and still be "functional" —
        // only ABSTRACT methods are counted, and there must be exactly one.
    }

    /**
     * Business method that ACCEPTS behavior as a parameter — the strategy pattern, minus
     * the classes. Pre-Java-8 you'd pass a MoneyPolicyImpl object here; now the caller
     * just hands us an arrow. (This loop is exactly `filter` from module 04, hand-rolled.)
     */
    static List<Transaction> keepIf(List<Transaction> transactions, MoneyPolicy policy) {
        List<Transaction> kept = new ArrayList<>();
        for (Transaction t : transactions) {
            if (policy.allows(t.getAmount())) {
                kept.add(t);
            }
        }
        return kept;
    }

    public static void main(String[] args) {
        List<Transaction> statement = Transaction.sampleStatement();
        BigDecimal limit = new BigDecimal("100.00");

        // Three policies, three lambdas — NO new classes anywhere:
        MoneyPolicy underLimit   = amount -> amount.compareTo(limit) <= 0;      // expression body
        MoneyPolicy overLimit    = amount -> amount.compareTo(limit) > 0;
        MoneyPolicy always       = amount -> true;                              // ignores its parameter

        print("under 100", keepIf(statement, underLimit));
        print("over 100", keepIf(statement, overLimit));
        print("everything", keepIf(statement, always));

        // A functional-interface-typed variable can be passed straight through:
        MoneyPolicy chosen = underLimit;
        System.out.println();
        System.out.println("stored in a variable and reused: " + keepIf(statement, chosen).size() + " kept");
    }

    private static void print(String label, List<Transaction> txs) {
        System.out.println();
        System.out.println(label + " (" + txs.size() + "):");
        for (Transaction t : txs) {
            System.out.println("  " + t);
        }
    }
}
