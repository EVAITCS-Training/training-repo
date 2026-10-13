package com.evaitcs.java8.compare;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * TASK 6 — "total balance of accounts owned by customers under 30".
 *
 * The boss fight. This query crosses TWO collections (customers -> accounts), so the
 * imperative version needs NESTED loops and a hand-rolled join. The stream version
 * needs one new idea to do the same crossing: flatMap.
 *
 * TEACHING POINT: map turns one element into ONE result. But "the accounts of this
 * customer" is a LIST — zero, one, or many accounts. If we map() that, we get a
 * stream OF LISTS (Stream<List<Account>>), which we can't reduce. flatMap takes each
 * of those little lists and FLATTENS them into a single continuous stream of accounts.
 * This is where flatMap finally clicks for students — see the picture below.
 */
public class TaskChain {

    private static final int AGE_LIMIT = 30;

    /*
     * LOOP ANATOMY — nested loops with a hand-rolled join:
     *
     *   (1) BigDecimal total = BigDecimal.ZERO;          <- accumulator (from TaskSum)
     *   (2) for (Customer c : customers) {               <- OUTER loop: walk customers
     *   (3)     if (c.getAge() < AGE_LIMIT) {            <- the age filter
     *   (4)         for (Account a : accounts) {         <- INNER loop: walk ALL accounts...
     *   (5)             if (a.getOwnerId().equals(c.getId())) {  <- ...to find THIS customer's
     *   (6)                 total = total.add(a.getBalance());   <- the join, by hand
     *   (7)             }
     *   (8)         }
     *   (9)     }
     *  (10) }
     *  (11) return total;
     *
     * Trap to name out loud: the inner loop scans the WHOLE accounts list once PER young
     * customer — that's O(customers x accounts). Fine for 6x8; a disaster for a million.
     * The two `if`s (age + owner-match) and the two `for`s are FOUR control-flow decisions
     * the reader must hold in their head at once, and the actual question — "sum balances
     * of young customers' accounts" — is buried in the middle at line (6).
     */
    public static BigDecimal imperative(List<Customer> customers, List<Account> accounts) {
        BigDecimal total = BigDecimal.ZERO;                    // (1)
        for (Customer c : customers) {                         // (2) outer: customers
            if (c.getAge() < AGE_LIMIT) {                      // (3) age filter
                for (Account a : accounts) {                   // (4) inner: ALL accounts
                    if (a.getOwnerId().equals(c.getId())) {    // (5) the join
                        total = total.add(a.getBalance());     // (6) accumulate
                    }
                }
            }
        }
        return total;                                          // (11)
    }

    /*
     * The pipeline, one stage per idea — read it top to bottom like the sentence:
     *
     *   customers.stream()                    start with customers
     *     .filter(c -> c.getAge() < AGE_LIMIT)   keep only the young ones   (line 3 above)
     *     .map(c -> accountsOwnedBy(c.getId()))  each customer -> THEIR LIST of accounts
     *     .flatMap(List::stream)                 flatten those lists into ONE stream of accounts
     *     .map(Account::getBalance)              each account -> its balance (TaskTransform)
     *     .reduce(ZERO, BigDecimal::add);        add them up (TaskSum)
     *
     * THE flatMap PICTURE (draw this on the board before running the code):
     *
     *   after .map(c -> accountsOwnedBy(...))  you have a STREAM OF LISTS:
     *
     *       Grace(28)   -> [A3]
     *       Katherine(24)-> [A5]
     *       Barbara(29) -> [A8]
     *       (Ada 36, Alan 41, Linus 55 were filtered out)
     *
     *   that's really:   [ [A3], [A5], [A8] ]     <- Stream<List<Account>>  (3 separate lists)
     *
     *   flatMap(List::stream) opens every list and pours the elements into ONE stream:
     *
     *                          v v v
     *                    [ A3, A5, A8 ]           <- Stream<Account>        (one flat stream)
     *
     *   The name is literal: map gives a stream where each item is a collection;
     *   flatMap FLATTENS those collections into a single stream. Rule of thumb to say
     *   out loud: "whenever the mapping function returns a LIST/STREAM and you want the
     *   elements, not the lists — you need flatMap."
     *
     * Note the join (lines 4-6 above) is now just SampleBank.accountsOwnedBy(id): the
     * "find this customer's accounts" lookup moved into a named method, so the pipeline
     * reads as WHAT we want, not HOW to search.
     */
    public static BigDecimal withStreams(List<Customer> customers, final List<Account> accounts) {
        return customers.stream()
                .filter(c -> c.getAge() < AGE_LIMIT)
                .map(c -> accountsOwnedBy(c.getId(), accounts))  // Stream<List<Account>>
                .flatMap(List::stream)                           // Stream<Account>  (flattened)
                .map(Account::getBalance)                        // Stream<BigDecimal>
                .reduce(BigDecimal.ZERO, BigDecimal::add);       // one total
    }

    /**
     * The join, isolated so the pipeline stays readable. We pass the accounts list in
     * (instead of calling SampleBank.accounts()) so this method is honest about which
     * data it searches — and so the test can feed it any list it likes.
     */
    private static List<Account> accountsOwnedBy(String ownerId, List<Account> accounts) {
        List<Account> owned = new ArrayList<>();
        for (Account a : accounts) {
            if (a.getOwnerId().equals(ownerId)) {
                owned.add(a);
            }
        }
        return owned;
    }

    public static void main(String[] args) {
        List<Customer> customers = SampleBank.customers();
        List<Account> accounts = SampleBank.accounts();

        BigDecimal imperative = imperative(customers, accounts);
        BigDecimal streams = withStreams(customers, accounts);

        System.out.println("QUESTION: what is the total balance of accounts owned by customers UNDER 30?");
        System.out.println("(under 30 = Grace 28 [A3], Katherine 24 [A5], Barbara 29 [A8])");
        System.out.println();
        System.out.println("IMPERATIVE (nested loops + hand join):              " + imperative.toPlainString());
        System.out.println("STREAMS    (filter.map.flatMap.map.reduce):         " + streams.toPlainString());
        System.out.println();
        System.out.println("MATCH? " + (imperative.compareTo(streams) == 0));
        System.out.println("POINT: flatMap turns a stream OF LISTS into ONE stream.");
        System.out.println("       The nested loops + join collapse into a pipeline that reads like the question.");
    }
}
