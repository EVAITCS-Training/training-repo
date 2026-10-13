package com.evaitcs.collections.sorting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Demo 3 — shuffle (and why it must be seeded here), plus the small games: min/max/frequency/disjoint.
 *
 * Collections.shuffle(list) is a Fisher-Yates walk: from the end toward the front, swap
 * each slot with a random earlier one. With no Random argument it builds a fresh unseeded
 * generator — a DIFFERENT permutation every run. That is exactly what you want in a card
 * game and exactly what you DO NOT want in a teaching repo: this demo (and its test) pass
 * new Random(42), because a seeded Random replays the SAME permutation every time, on
 * every machine. Same rule as everywhere in this course: demos and tests print
 * deterministic evidence — the seed is what buys it.
 *
 * The rest of the toolbox is the "small games" tier: min/max (with or without a
 * comparator), frequency (counts by EQUALS — module 01's engine), disjoint (no elements in
 * common). They are one-liners that replace hand-rolled loops students keep rewriting;
 * java8-features/04-streams-fundamentals later replaces most of them again with
 * stream().min()/filter()/count() — the loops die twice.
 */
public class ShuffleAndGamesDemo {

    public static void main(String[] args) {
        banner("1. Seeded shuffle — new Random(42) replays the SAME permutation, every run");

        List<String> a = idsOf(Transaction.sampleStatement());
        List<String> b = new ArrayList<String>(a);
        Collections.shuffle(a, new Random(42));      // seeded: deterministic
        Collections.shuffle(b, new Random(42));      // same seed: same result
        System.out.println("before              : " + idsOf(Transaction.sampleStatement()));
        System.out.println("shuffle(a, Random(42)): " + a);
        System.out.println("shuffle(b, Random(42)): " + b);
        System.out.println("identical? " + a.equals(b) + "   <- the seed is the contract, not luck");
        System.out.println("Collections.shuffle(list) with NO Random differs on every run — fine for games,");
        System.out.println("fatal for tests. This course seeds every shuffle: new Random(42).");

        banner("2. min/max — natural order, or any comparator you hand them");

        List<Transaction> statement = Transaction.sampleStatement();
        System.out.println("min by id (natural order)      : " + Collections.min(statement).getId());
        System.out.println("max by id                      : " + Collections.max(statement).getId());

        // Classic idiom: anonymous comparator (core-java 10; no lambdas in main code).
        Comparator<Transaction> byTimestamp = new Comparator<Transaction>() {
            @Override
            public int compare(Transaction a, Transaction b) {
                return Long.compare(a.getTimestampMillis(), b.getTimestampMillis());
            }
        };
        System.out.println("earliest by timestamp          : " + Collections.min(statement, byTimestamp).getId() + " (ts=500, Salary)");
        System.out.println("latest by timestamp            : " + Collections.max(statement, byTimestamp).getId() + " (ts=3000, Groceries)");
        System.out.println("One linear scan each — O(n). (A TreeSet would answer in O(log n) but keeps");
        System.out.println("EVERY element sorted; for a one-off question, min/max is the right tool — module 08.)");

        banner("3. frequency — counting by EQUALS, not by ==");

        // Which account does the statement touch most often? Tags per line, duplicates included.
        List<String> touchedAccounts = Arrays.asList("ACC-1005", "ACC-1027", "ACC-1005", "ACC-1011", "ACC-1005");
        System.out.println("statement touches: " + touchedAccounts);
        System.out.println("frequency(ACC-1005) = " + Collections.frequency(touchedAccounts, "ACC-1005"));
        System.out.println("frequency(ACC-9999) = " + Collections.frequency(touchedAccounts, "ACC-9999") + "   (absent = 0, never an error)");
        System.out.println("It counts with equals() — a stranger Account with the same id would count too (module 01).");

        banner("4. disjoint — do these two collections share ANY element?");

        List<String> weekday = Arrays.asList("T-1001", "T-1002", "T-1003");
        List<String> weekend = Arrays.asList("T-1004", "T-1005");
        List<String> auditCopy = Arrays.asList("T-1003", "T-1009");
        System.out.println("weekday = " + weekday + ", weekend = " + weekend);
        System.out.println("disjoint(weekday, weekend)   = " + Collections.disjoint(weekday, weekend) + "   (no overlap)");
        System.out.println("disjoint(weekday, auditCopy) = " + Collections.disjoint(weekday, auditCopy) + "  (they share T-1003)");
        System.out.println("true means NOTHING in common; false means at least one shared element (equals again).");
    }

    /** Small helper: the ids of a statement, in list order, for one-line printing. */
    private static List<String> idsOf(List<Transaction> transactions) {
        List<String> ids = new ArrayList<String>();
        for (Transaction t : transactions) {
            ids.add(t.getId());
        }
        return ids;
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
