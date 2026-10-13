package com.evaitcs.java8.compare;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The fixed sample world every task in this module runs against.
 *
 * WHY ONE SHARED DATASET: when the imperative and stream versions of a task print
 * different-looking output, we want the difference to be the CODE, never the data.
 * Same six customers, same eight accounts, same fifteen transactions everywhere —
 * including in the tests, which is what makes "both return EQUAL results" provable.
 *
 * The data is chosen so every task has something interesting to show:
 *   - deposits above and below 100           (TaskFilter)
 *   - mixed-case names                        (TaskTransform)
 *   - amounts with cents, so BigDecimal scale matters (TaskSum)
 *   - a clear max balance, and an EMPTY-list edge case we test separately (TaskMax)
 *   - three cities with uneven customer counts (TaskCount)
 *   - three customers under 30 with differing account counts (TaskChain)
 *   - transactions above and below the 2000 suspicious threshold (TaskAnyMatch)
 */
public final class SampleBank {

    private SampleBank() { } // static-only: nobody should ever instantiate the bank

    public static List<Customer> customers() {
        return Arrays.asList(
                new Customer("C1", "Ada Lovelace",      36, "Austin"),
                new Customer("C2", "Grace Hopper",      28, "Austin"),
                new Customer("C3", "Alan Turing",       41, "Denver"),
                new Customer("C4", "Katherine Johnson", 24, "Austin"),
                new Customer("C5", "Linus Pauling",     55, "Seattle"),
                new Customer("C6", "Barbara Liskov",    29, "Seattle"));
    }

    public static List<Account> accounts() {
        return Arrays.asList(
                new Account("A1", "C1", Account.Type.CHECKING, new BigDecimal("2500.00")),
                new Account("A2", "C1", Account.Type.SAVINGS,  new BigDecimal("12000.50")),
                new Account("A3", "C2", Account.Type.CHECKING, new BigDecimal("840.25")),
                new Account("A4", "C3", Account.Type.SAVINGS,  new BigDecimal("31000.00")),
                new Account("A5", "C4", Account.Type.CHECKING, new BigDecimal("150.75")),
                new Account("A6", "C5", Account.Type.CHECKING, new BigDecimal("4200.00")),
                new Account("A7", "C5", Account.Type.SAVINGS,  new BigDecimal("8750.25")),
                new Account("A8", "C6", Account.Type.CHECKING, new BigDecimal("990.10")));
    }

    public static List<Transaction> transactions() {
        return Arrays.asList(
                new Transaction("T01", "A1", "Payroll",              new BigDecimal("2500.00"), true),
                new Transaction("T02", "A1", "Rent",                 new BigDecimal("1200.00"), false),
                new Transaction("T03", "A3", "Groceries",            new BigDecimal("86.40"),   false),
                new Transaction("T04", "A3", "Interest",             new BigDecimal("3.11"),    true),
                new Transaction("T05", "A2", "Transfer to savings",  new BigDecimal("500.00"),  true),
                new Transaction("T06", "A4", "Certificate deposit",  new BigDecimal("25000.00"),true),
                new Transaction("T07", "A4", "ATM withdrawal",       new BigDecimal("300.00"),  false),
                new Transaction("T08", "A5", "Coffee",               new BigDecimal("4.75"),    false),
                new Transaction("T09", "A5", "Refund",               new BigDecimal("45.99"),   true),
                new Transaction("T10", "A6", "Wire received",        new BigDecimal("5400.00"), true),
                new Transaction("T11", "A6", "Utility bill",         new BigDecimal("210.33"),  false),
                new Transaction("T12", "A7", "Dividend",             new BigDecimal("120.44"),  true),
                new Transaction("T13", "A2", "Wire sent",            new BigDecimal("3200.00"), false),
                new Transaction("T14", "A8", "Online purchase",      new BigDecimal("59.99"),   false),
                new Transaction("T15", "A1", "Cash withdrawal",      new BigDecimal("2600.00"), false));
    }

    /**
     * The "join" used by TaskChain: which accounts belong to one owner?
     *
     * Written with a plain loop ON PURPOSE. This helper is the pre-Java-8 way of
     * linking two collections, and TaskChain's imperative version leans on exactly
     * this shape. The stream version does the same join inside its pipeline with
     * map + flatMap — compare the two and you see what flatMap is really for.
     */
    public static List<Account> accountsOwnedBy(String ownerId) {
        List<Account> owned = new ArrayList<>();
        for (Account a : accounts()) {
            if (a.getOwnerId().equals(ownerId)) {
                owned.add(a);
            }
        }
        return owned;
    }
}
