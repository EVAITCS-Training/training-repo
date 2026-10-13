package com.evaitcs.java8.compare;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * TASK 4 — "the account with the largest balance".
 *
 * Another reduction (many -> one), but the combining rule is "keep the bigger one"
 * instead of "add". The interesting part isn't the max — it's what happens when
 * the list is EMPTY.
 *
 * TEACHING POINT: the imperative version must return null (or throw) on an empty list,
 * and every caller inherits that ambiguity. The stream version returns Optional<Account>
 * — the "maybe there is no winner" case is IN THE TYPE, so the compiler makes callers
 * deal with it. This ties straight back to module 03.
 */
public class TaskMax {

    /*
     * LOOP ANATOMY:
     *
     *   (1) Account bestSoFar = null;        <- accumulator again, but it starts as NULL
     *                                              because there's no sensible "identity account".
     *   (2) for (Account a : accounts) {     <- iteration, by hand
     *   (3)     if (bestSoFar == null ||     <- THE EDGE CASE, tangled into the comparison.
     *               a.getBalance().compareTo(bestSoFar.getBalance()) > 0) {
     *   (4)         bestSoFar = a;           <- reassign the accumulator
     *   (5)     }
     *   (6) }
     *   (7) return bestSoFar;                <- returns NULL when accounts is empty.
     *
     * Trap to name out loud: line (7) hands the caller a value that might be null,
     * but the SIGNATURE doesn't say so. Someone writes `maxAccount(...).getId()` and
     * gets a NullPointerException in production — on the one day the accounts table is empty.
     * The `bestSoFar == null` check in (3) is easy to forget; forget it and the FIRST
     * iteration NPEs instead.
     */
    public static Account imperative(List<Account> accounts) {
        Account bestSoFar = null;                          // (1)
        for (Account a : accounts) {                       // (2)
            if (bestSoFar == null ||                        // (3) null guard + comparison
                    a.getBalance().compareTo(bestSoFar.getBalance()) > 0) {
                bestSoFar = a;                             // (4)
            }
        }
        return bestSoFar;                                  // (7) possibly null — silently
    }

    /*
     * Same anatomy, mapped one-to-one:
     *   the bestSoFar accumulation (1)(4) -> max(comparator) does the "keep bigger" fold
     *   the comparison (3) -> Comparator.comparing(Account::getBalance), passed IN
     *   the for (2) -> GONE (internal iteration, again)
     *   the null return (7) -> Optional<Account>. THE EDGE CASE IS NOW IN THE TYPE.
     *
     * max() returns Optional because an empty stream has no maximum — but instead of
     * handing back a null that compiles fine and explodes later, it hands back a box
     * the caller MUST open. Empty list -> Optional.empty(), not a NullPointerException.
     *
     * Comparator.comparing(Account::getBalance) reads as English: "compare accounts BY
     * balance" — the same method-reference trick from module 01.
     */
    public static Optional<Account> withStreams(List<Account> accounts) {
        return accounts.stream()
                .max(Comparator.comparing(Account::getBalance));
    }

    public static void main(String[] args) {
        List<Account> accounts = SampleBank.accounts();

        Account imperative = imperative(accounts);
        Optional<Account> streams = withStreams(accounts);

        System.out.println("QUESTION: which account has the largest balance?");
        System.out.println();
        System.out.println("IMPERATIVE (bestSoFar loop, returns Account or null):");
        System.out.println("  " + imperative);
        System.out.println();
        System.out.println("STREAMS    (max(comparing), returns Optional<Account>):");
        System.out.println("  " + streams);
        System.out.println();
        System.out.println("MATCH? " + streams.equals(Optional.ofNullable(imperative)));

        // Now the edge case the type system was invented for: NO accounts at all.
        System.out.println();
        System.out.println("EDGE CASE — empty account list:");
        Account emptyImperative = imperative(Collections.<Account>emptyList());
        Optional<Account> emptyStreams = withStreams(Collections.<Account>emptyList());
        System.out.println("  IMPERATIVE returned: " + emptyImperative + "   <- caller must KNOW it can be null");
        System.out.println("  STREAMS    returned: " + emptyStreams + "  <- compiler FORCES the caller to handle it");
        System.out.println("  Both are 'no winner'; only Optional makes that impossible to ignore.");
        System.out.println();
        System.out.println("POINT: Optional kills the empty-list edge case (see module 03).");
    }
}
