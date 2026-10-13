package com.evaitcs.corejava.ordering;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 10-comparable-comparator test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC — including the failures: the raw-list
 * ClassCastException, the subtraction overflow and the NullPointerException all happen
 * on chosen values, every single run. A contract violation is a bug you can test for.
 */
class OrderingContractTest {

    private static final long EARLY_TS = 1_697_850_000_000L;
    private static final long LATE_TS = 1_700_000_000_000L; // differ by > Integer.MAX_VALUE millis

    private static List<String> idsOf(List<Transaction> statement) {
        List<String> ids = new ArrayList<String>();
        for (Transaction t : statement) {
            ids.add(t.getId());
        }
        return ids;
    }

    @Test
    @DisplayName("natural order: Collections.sort with no comparator sorts Transactions by id")
    void naturalOrderSortsById() {
        List<Transaction> statement = Transaction.sampleStatement();
        Collections.sort(statement);
        System.out.println("sorted ids: " + idsOf(statement));
        assertEquals(Arrays.asList("T-1005", "T-1011", "T-1027", "T-1033", "T-1040"), idsOf(statement));
    }

    @Test
    @DisplayName("compareTo returns the SIGN, not -1/0/1, and is antisymmetric")
    void compareToReturnsSignNotMagicNumbers() {
        Transaction a = new Transaction("T-1005", "Interest", new BigDecimal("3.11"), EARLY_TS);
        Transaction b = new Transaction("T-1040", "Payroll", new BigDecimal("2450.00"), LATE_TS);

        int c = a.compareTo(b);
        System.out.println("a.compareTo(b) = " + c + " (String.compareTo char difference — NOT -1)");
        assertTrue(c < 0, "a sorts before b");
        assertNotEquals(-1, c, "the magnitude is an implementation detail; only the sign is the contract");
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(new Transaction("T-1005", "other", BigDecimal.ONE, 0L)), "same id ties");

        // sgn(x.compareTo(y)) == -sgn(y.compareTo(x)) for every ordered pair
        List<Transaction> statement = Transaction.sampleStatement();
        for (Transaction x : statement) {
            for (Transaction y : statement) {
                assertEquals(Integer.signum(x.compareTo(y)), -Integer.signum(y.compareTo(x)),
                        "antisymmetry broken for " + x.getId() + "/" + y.getId());
            }
        }
        System.out.println("antisymmetry holds for all " + (statement.size() * statement.size()) + " ordered pairs");
    }

    @Test
    @DisplayName("custom comparator: ByAmountDesc sorts the SAME list biggest amount first")
    void customComparatorSortsByAmountDesc() {
        List<Transaction> statement = Transaction.sampleStatement();
        Collections.sort(statement, new ByAmountDesc());
        System.out.println("by amount desc: " + idsOf(statement));
        assertEquals(Arrays.asList("T-1040", "T-1011", "T-1027", "T-1033", "T-1005"), idsOf(statement));
        assertEquals(0, statement.get(0).getAmount().compareTo(new BigDecimal("2450.00")));
    }

    @Test
    @DisplayName("sorting WITH a comparator does NOT require Comparable (Account has none)")
    void sortWithComparatorDoesNotNeedComparable() {
        List<Account> accounts = new ArrayList<Account>(Arrays.asList(
                new Account("A-01", "Dana", new BigDecimal("100.00")),
                new Account("A-03", "Ana", new BigDecimal("42.00"))));
        assertFalse(accounts.get(0) instanceof Comparable, "Account deliberately implements no Comparable");

        Collections.sort(accounts, new ByOwner());
        List<String> owners = new ArrayList<String>();
        for (Account a : accounts) {
            owners.add(a.getOwner());
        }
        System.out.println("accounts sorted by an OUTSIDE comparator: " + owners);
        assertEquals(Arrays.asList("Ana", "Dana"), owners);
    }

    @Test
    @DisplayName("sorting a raw list of non-Comparable elements throws ClassCastException — deterministically")
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void sortingNonComparableRawListThrowsCCE() {
        List statement = new ArrayList(); // raw: switches off the compile-time seatbelt
        statement.add(new SortingProblemDemo.Transaction("T-1002", new BigDecimal("1200.00")));
        statement.add(new SortingProblemDemo.Transaction("T-1001", new BigDecimal("2450.00")));

        // anonymous Executable — the classic shape, no lambdas in this course yet
        ClassCastException e = assertThrows(ClassCastException.class, new Executable() {
            @Override
            public void execute() {
                Collections.sort(statement);
            }
        });
        System.out.println("caught: " + e.getMessage());
        assertTrue(e.getMessage().contains("cannot be cast to"),
                "message must name the failed cast, got: " + e.getMessage());
        assertTrue(e.getMessage().contains("Comparable"),
                "message must name Comparable as the missing interface, got: " + e.getMessage());
    }

    @Test
    @DisplayName("TreeMap/TreeSet use compareTo, NOT equals — a compareTo-tie 'loses' an element (module 08)")
    void sortedCollectionsUseCompareToNotEquals() {
        Transaction rent = new Transaction("T-1099", "Rent", new BigDecimal("1200.00"), EARLY_TS);
        Transaction correction = new Transaction("T-1099", "Rent correction", new BigDecimal("-0.01"), EARLY_TS);

        assertFalse(rent.equals(correction), "distinct objects, equals not overridden -> identity (module 08)");
        assertEquals(0, rent.compareTo(correction), "same id -> compareTo says TIE");

        Set<Transaction> hash = new HashSet<Transaction>(Arrays.asList(rent, correction));
        Set<Transaction> tree = new TreeSet<Transaction>(Arrays.asList(rent, correction));
        System.out.println("HashSet size (equals)    = " + hash.size());
        System.out.println("TreeSet size (compareTo) = " + tree.size() + "  <- one transaction 'lost'");
        assertEquals(2, hash.size());
        assertEquals(1, tree.size());

        // Same law in a TreeMap keyed by amount-descending comparator: equal BY COMPARATOR
        // means "same key", regardless of equals.
        TreeMap<Transaction, String> byAmount = new TreeMap<Transaction, String>(new ByAmountDesc());
        byAmount.put(rent, "first");
        byAmount.put(new Transaction("T-0001", "Same amount, different everything else",
                new BigDecimal("1200.00"), LATE_TS), "second");
        System.out.println("TreeMap(ByAmountDesc) size after two 1200.00 keys = " + byAmount.size()
                + ", value = " + byAmount.get(rent));
        assertEquals(1, byAmount.size());
        assertEquals("second", byAmount.get(rent), "the second put REPLACED the first entry");
    }

    @Test
    @DisplayName("the subtraction comparator overflows and sorts WRONG — deterministic, chosen values")
    void subtractionComparatorOverflows() {
        Transaction early = new Transaction("T-1001", "Opening balance", new BigDecimal("0.01"), EARLY_TS);
        Transaction late = new Transaction("T-2001", "Payroll", new BigDecimal("2450.00"), LATE_TS);

        long realDiff = late.getTimestampMillis() - early.getTimestampMillis();
        int castDiff = (int) realDiff;
        System.out.println("real long diff = " + realDiff + " ; after (int) cast = " + castDiff + " (sign flipped)");
        assertTrue(realDiff > 0 && castDiff < 0, "the cast must flip the sign — that is the trap");

        ByTimestampSubtractionBroken broken = new ByTimestampSubtractionBroken();
        assertTrue(broken.compare(late, early) < 0, "broken comparator LIES: says late < early");

        List<Transaction> wrongOrder = new ArrayList<Transaction>(Arrays.asList(late, early));
        Collections.sort(wrongOrder, broken);
        System.out.println("broken order : " + idsOf(wrongOrder) + "  <- WRONG (newer first)");
        assertEquals(Arrays.asList("T-2001", "T-1001"), idsOf(wrongOrder), "wrong order is deterministic");

        List<Transaction> rightOrder = new ArrayList<Transaction>(Arrays.asList(late, early));
        Collections.sort(rightOrder, new ByTimestamp()); // Long.compare inside
        System.out.println("correct order: " + idsOf(rightOrder));
        assertEquals(Arrays.asList("T-1001", "T-2001"), idsOf(rightOrder));
    }

    @Test
    @DisplayName("chained comparator breaks a timestamp tie by amount, regardless of insertion order")
    void chainedComparatorTieBreaksByAmount() {
        Transaction refund = new Transaction("T-1033", "Refund", new BigDecimal("45.99"), EARLY_TS);
        Transaction interest = new Transaction("T-1005", "Interest", new BigDecimal("3.11"), EARLY_TS);

        List<Transaction> chained = new ArrayList<Transaction>(Arrays.asList(refund, interest)); // tie-prone order
        Collections.sort(chained, new ByTimestampThenAmountThenId());
        System.out.println("tie broken by amount: " + idsOf(chained));
        assertEquals(Arrays.asList("T-1005", "T-1033"), idsOf(chained), "3.11 must precede 45.99");

        List<Transaction> otherWay = new ArrayList<Transaction>(Arrays.asList(interest, refund));
        Collections.sort(otherWay, new ByTimestampThenAmountThenId());
        assertEquals(idsOf(chained), idsOf(otherWay), "order must NOT depend on insertion order");
    }

    @Test
    @DisplayName("reverseOrder() inverts the COMPARISON; reverse() only MUTATES the list")
    void reverseOrderVersusReverse() {
        List<Transaction> sortedDesc = Transaction.sampleStatement();
        Collections.sort(sortedDesc, Collections.<Transaction>reverseOrder());

        List<Transaction> sortedThenFlipped = Transaction.sampleStatement();
        Collections.sort(sortedThenFlipped);
        Collections.reverse(sortedThenFlipped);

        System.out.println("reverseOrder sort : " + idsOf(sortedDesc));
        System.out.println("sort then reverse : " + idsOf(sortedThenFlipped));
        assertEquals(Arrays.asList("T-1040", "T-1033", "T-1027", "T-1011", "T-1005"), idsOf(sortedDesc));
        assertEquals(idsOf(sortedDesc), idsOf(sortedThenFlipped),
                "same result ONLY because reverse() ran on an already-sorted list");
    }

    @Test
    @DisplayName("null fields crash a naive comparator (NPE); the null-safe one has a policy")
    void nullFieldsNeedAnExplicitPolicy() {
        Account joint = new Account("A-02", null, new BigDecimal("7.50"));
        List<Account> accounts = new ArrayList<Account>(Arrays.asList(
                new Account("A-01", "Dana", new BigDecimal("100.00")),
                joint,
                new Account("A-03", "Ana", new BigDecimal("42.00"))));

        NullPointerException e = assertThrows(NullPointerException.class, new Executable() {
            @Override
            public void execute() {
                Collections.sort(accounts, new ByOwner());
            }
        });
        System.out.println("naive ByOwner threw: " + e.getClass().getName());

        Collections.sort(accounts, new ByOwnerNullSafe());
        List<String> ids = new ArrayList<String>();
        for (Account a : accounts) {
            ids.add(a.getId());
        }
        System.out.println("null-safe order (nulls first): " + ids);
        assertEquals(Arrays.asList("A-02", "A-03", "A-01"), ids, "null owner first, then Ana, then Dana");
    }
}
