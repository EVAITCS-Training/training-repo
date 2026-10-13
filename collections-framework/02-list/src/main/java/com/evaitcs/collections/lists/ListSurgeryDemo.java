package com.evaitcs.collections.lists;

import java.math.BigDecimal;
import java.util.List;

/**
 * Demo 2 — THE LIST API AS SURGERY ON A BANK STATEMENT.
 *
 * A List is an ordered sequence: every element has a POSITION, and half of the API takes one.
 * This demo walks the whole operating table over a single statement — add (append/insert), set,
 * get, both removes, and the find trio indexOf/lastIndexOf/contains — printing the list after
 * every step so the shifting stays visible.
 *
 * The engine under every "find" operation (contains, indexOf, remove(Object)) is equals():
 * a freshly constructed Transaction with the same id IS that transaction — the contract
 * core-java 08 built, cashed in again (module 01 proved it for contains; here it drives the
 * whole search API). Amount comparisons use BigDecimal.compareTo, never equals — "3.75" and
 * "3.750" are equals()-different but compareTo-equal (the core-java 10 consistency lesson).
 */
public class ListSurgeryDemo {

    public static void main(String[] args) {
        banner("1. The patient — a week of statement, chronologically ordered");

        List<Transaction> statement = Transaction.sampleStatement();
        printWithPositions(statement);

        banner("2. add — append at the end, insert in the middle");

        statement.add(new Transaction("TXN-1007", "Card payment", new BigDecimal("45.99"), 1_700_518_400_000L));
        System.out.println("add(e) appended at the end (amortized O(1) on ArrayList): size = " + statement.size());
        // add(i, e) first makes room: on an ArrayList everything from position 2 onward slides one slot right.
        statement.add(2, new Transaction("TXN-1002b", "Rent — late fee correction", new BigDecimal("25.00"), 1_700_172_800_000L));
        System.out.println("add(2, e) inserted mid-list — everything at/after position 2 shifted RIGHT (O(n) on both engines):");
        printWithPositions(statement);

        banner("3. set and get — replace and read by position");

        // Transaction is immutable (all fields final) — "correcting a row" means REPLACING the object.
        Transaction wrong = statement.get(4);              // get(i): O(1) on an ArrayList — it IS arr[i]
        Transaction right = new Transaction(wrong.getId(), "ATM withdrawal (corrected)", wrong.getAmount(), wrong.getTimestampMillis());
        Transaction previous = statement.set(4, right);    // set returns whatever occupied the slot before
        System.out.println("get(4)      -> " + wrong);
        System.out.println("set(4, ...) -> old: " + previous);
        System.out.println("               new: " + statement.get(4));
        System.out.println("set() is NOT structural: same slot, same size — no shifting, no modCount bump (module 03).");

        banner("4. remove — two different operations wearing one name");

        Transaction byIndex = statement.remove(2);         // remove(int index): by POSITION; RETURNS the element
        System.out.println("remove(2) took out position 2 and RETURNED it: " + byIndex + "   (size now " + statement.size() + ")");

        // remove(Object) scans with equals() — this fresh object was never stored anywhere, yet it
        // IS the row with id TXN-1005, because identity here means id (core-java 08 at work).
        Transaction equalStranger = new Transaction("TXN-1005", "constructed seconds ago, never stored", new BigDecimal("3.75"), 0L);
        boolean found = statement.remove(equalStranger);   // remove(Object): by VALUE; RETURNS boolean
        System.out.println("remove(new Transaction(\"TXN-1005\", ...)) returned " + found + " — an equal stranger removed the original row.");
        printWithPositions(statement);
        System.out.println("On a List<Integer> the two overloads collide — remove(1) is the SLOT, not the value: RemovePitfallsDemo banner 2.");

        banner("5. The find trio — indexOf, lastIndexOf, contains (all powered by equals)");

        // The real-world story: a double posting. Same id (so EQUALS-equal), two rows.
        Transaction insurance = statement.get(statement.size() - 2);
        statement.add(new Transaction(insurance.getId(), "Insurance — posted twice (bank bug)", insurance.getAmount(),
                insurance.getTimestampMillis() + 60_000L));
        System.out.println("the bank double-posted " + insurance.getId() + ":");
        System.out.println("  indexOf(txn)     = " + statement.indexOf(insurance) + "   <- FIRST equal position");
        System.out.println("  lastIndexOf(txn) = " + statement.lastIndexOf(insurance) + "   <- LAST equal position");
        System.out.println("  contains(txn)    = " + statement.contains(insurance) + "   <- shorthand for indexOf(...) >= 0");
        System.out.println("  contains(stranger with same id) = "
                + statement.contains(new Transaction(insurance.getId(), "never stored anywhere", BigDecimal.ZERO, 0L)));
        statement.remove(insurance);
        System.out.println("remove(Object) took only the FIRST occurrence — the duplicate now sits at "
                + statement.indexOf(insurance) + ".");
        System.out.println("De-duplicating properly is module 04's job (Set); sorting the statement is module 07's (core-java 10's harvest).");
    }

    /** Prints the statement with position numbers — positions are what a List adds to a bag. */
    static void printWithPositions(List<Transaction> statement) {
        for (int i = 0; i < statement.size(); i++) {
            System.out.println("  [" + i + "] " + statement.get(i));
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
