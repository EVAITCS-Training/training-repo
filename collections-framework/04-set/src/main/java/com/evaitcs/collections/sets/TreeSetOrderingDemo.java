package com.evaitcs.collections.sets;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Demo 3 — TreeSet's one special law: it NEVER calls equals().
 *
 * A TreeSet decides "is this a duplicate?" with compareTo (natural order) or with the
 * Comparator it was built with — and with NOTHING else. compareTo() == 0 means duplicate,
 * even when equals() screams "different objects!". This is core-java 10's
 * "consistency with equals" warning turned into a runnable crime scene:
 *
 *   banner 1: the natural order — and the ClassCastException when a class has NO order.
 *   banner 2: a ByAmount comparator silently SWALLOWS a distinct transaction, and
 *             contains() then returns true for an object that was never inserted.
 *   banner 3: the BigDecimal trap — 2.0 vs 2.00: equals says strangers (scale differs),
 *             compareTo says tie. HashSet keeps both; TreeSet keeps one. Same two
 *             objects, opposite verdicts, one law per engine.
 *
 * The comparator below is an ANONYMOUS INNER CLASS on purpose — the classic idiom from
 * core-java 10; the lambda rewrite waits in java8-features/01-lambdas.
 */
public class TreeSetOrderingDemo {

    public static void main(String[] args) {
        banner("1. Natural order — the no-arg TreeSet asks compareTo, by id");

        Set<Transaction> sorted = new TreeSet<Transaction>();
        for (Transaction t : Transaction.sampleStatement()) {
            sorted.add(t);
        }
        System.out.println("TreeSet                     : " + ThreeSetsSameDataDemo.idsOf(sorted));
        System.out.println("add(stranger, same id T-1040) -> "
                + sorted.add(new Transaction("T-1040", "Impostor", new BigDecimal("0.01"), 0L)));
        System.out.println("compareTo said 0 (tie), so the Set said 'duplicate'. It never looked at description or amount.");

        System.out.println();
        System.out.println("And a class with NO order at all — Account is not Comparable:");
        Set<Account> accounts = new TreeSet<Account>();
        try {
            accounts.add(Account.sampleAccounts().get(0));
        } catch (ClassCastException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName()
                    + " — the tree tried to cast Account to Comparable and failed, on the FIRST add.");
        }
        System.out.println("Fix: implement Comparable, or hand the TreeSet a Comparator — core-java 10, module 01's HierarchyDemo.");

        banner("2. Custom order, silent swallow — a ByAmount TreeSet loses a real transaction");

        Comparator<Transaction> byAmount = new Comparator<Transaction>() {
            @Override
            public int compare(Transaction a, Transaction b) {
                return a.getAmount().compareTo(b.getAmount());   // BigDecimal.compareTo: by VALUE (see banner 3)
            }
        };
        Set<Transaction> byAmountSet = new TreeSet<Transaction>(byAmount);

        Transaction latte = new Transaction("T-9001", "Latte", new BigDecimal("4.50"), 1_700_000_100_000L);
        Transaction espresso = new Transaction("T-9002", "Espresso", new BigDecimal("4.50"), 1_700_000_200_000L);

        System.out.println("add(latte)    returned " + byAmountSet.add(latte));
        System.out.println("add(espresso) returned " + byAmountSet.add(espresso) + " — REFUSED. Different id, different description...");
        System.out.println("latte.equals(espresso)          = " + latte.equals(espresso) + "    <- equals says: two DIFFERENT transactions");
        System.out.println("byAmount.compare(latte,espresso) = " + byAmount.compare(latte, espresso)
                + "         <- comparator says: TIE, and tie means duplicate");
        System.out.println("size = " + byAmountSet.size() + "; contains(espresso) = " + byAmountSet.contains(espresso)
                + " — TRUE for an object the Set does NOT hold!");
        System.out.println("This is the only Set where contains() can be true for something that was never inserted.");
        System.out.println("Fix when both keys matter: chain the comparison — by amount, THEN by id (core-java 10's ladder).");

        banner("3. The BigDecimal trap — 2.0 vs 2.00, same law, opposite verdicts");

        BigDecimal a = new BigDecimal("2.0");
        BigDecimal b = new BigDecimal("2.00");
        System.out.println("a.equals(b)    = " + a.equals(b) + "    <- BigDecimal.equals compares VALUE *AND SCALE*");
        System.out.println("a.compareTo(b) = " + a.compareTo(b) + "         <- compareTo compares numeric VALUE only");
        System.out.println("a.hashCode() == b.hashCode() ? " + (a.hashCode() == b.hashCode()) + " — equals and hashCode stay CONSISTENT (core-java 08).");

        Set<BigDecimal> hashed = new HashSet<BigDecimal>();
        System.out.println("HashSet: add(2.0) -> " + hashed.add(a) + ", add(2.00) -> " + hashed.add(b)
                + "   size = " + hashed.size() + " — it asked equals: strangers, both kept.");

        Set<BigDecimal> tree = new TreeSet<BigDecimal>();
        System.out.println("TreeSet: add(2.0) -> " + tree.add(a) + ", add(2.00) -> " + tree.add(b)
                + "   size = " + tree.size() + " — it asked compareTo: tie, one swallowed.");

        System.out.println("House rule: money is scale 2 EVERYWHERE (setScale at the edges), and anything that");
        System.out.println("ORDERS money uses compareTo — it is the comparison that behaves like arithmetic.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
