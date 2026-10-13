package com.evaitcs.collections.sets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 04-set test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC. The golden rule this module preaches, this
 * test class obeys: NEVER assert a HashSet's iteration order (it depends on hashes, not
 * insertion) — order is asserted on LinkedHashSet (insertion) and TreeSet (sorted), or a
 * HashSet is compared through new TreeSet<>(set). The NPE, the ClassCastException and the
 * corrupted-set lookups all happen on chosen values, every single run.
 */
class SetContractTest {

    /** ids in iteration order — only ever called on ORDERED sets, so the result is deterministic. */
    private static List<String> idsOf(Set<Transaction> transactions) {
        List<String> ids = new ArrayList<String>();
        for (Transaction t : transactions) {
            ids.add(t.getId());
        }
        return ids;
    }

    @Test
    @DisplayName("duplicates are rejected by equals(): the stranger with the same id never gets in")
    void duplicateByEqualsRejected() {
        Set<Transaction> set = new LinkedHashSet<Transaction>(Transaction.sampleStatement());
        assertEquals(5, set.size());

        Transaction impostor = new Transaction("T-1040", "NOT Payroll", new BigDecimal("0.01"), 1_700_000_999_000L);
        assertFalse(set.add(impostor), "add() returns false: an equal element (by id) is already inside");
        assertEquals(5, set.size(), "size unchanged — a Set REFUSES, it never overwrites");
        assertTrue(set.contains(impostor), "contains says true — but it found the ORIGINAL, via equals");

        // The identity proof: the stored element is still the original Payroll, not the impostor.
        for (Transaction t : set) {
            if (t.getId().equals("T-1040")) {
                assertEquals("Payroll", t.getDescription(), "the original survived; the impostor was never stored");
            }
        }
        System.out.println("add(impostor) = false, size = " + set.size() + ", stored T-1040 is still 'Payroll'");
    }

    @Test
    @DisplayName("LinkedHashSet keeps EXACTLY the insertion order — that is the guarantee you pay the list for")
    void linkedHashSetKeepsInsertionOrder() {
        List<Transaction> statement = Transaction.sampleStatement();
        Set<Transaction> linked = new LinkedHashSet<Transaction>(statement);

        assertEquals(idsOf(linked), idsOf(new LinkedHashSet<Transaction>(statement)),
                "stable across runs: insertion order, every time");
        // Compare against the List's own order element by element:
        int i = 0;
        for (Transaction t : linked) {
            assertEquals(statement.get(i).getId(), t.getId(), "position " + i + " matches the List");
            i++;
        }
        System.out.println("insertion order == statement order: " + idsOf(linked));
    }

    @Test
    @DisplayName("TreeSet sorts by compareTo — and compareTo() == 0 collapses elements equals() would keep")
    void treeSetSortsAndCollapsesOnCompareToZero() {
        Set<Transaction> sorted = new TreeSet<Transaction>(Transaction.sampleStatement());
        assertEquals(Arrays.asList("T-1005", "T-1011", "T-1027", "T-1033", "T-1040"), idsOf(sorted),
                "natural order: Transaction.compareTo, by id (core-java 10)");

        // Same id, DIFFERENT amount/description/timestamp — compareTo says tie, so TreeSet says duplicate:
        Transaction first = new Transaction("T-7777", "Deposit", new BigDecimal("10.00"), 1_700_000_100_000L);
        Transaction second = new Transaction("T-7777", "Withdrawal", new BigDecimal("99.99"), 1_700_000_200_000L);
        Set<Transaction> collapsed = new TreeSet<Transaction>();
        assertTrue(collapsed.add(first));
        assertFalse(collapsed.add(second), "compareTo() == 0 means duplicate — the tree never asked equals()");
        assertEquals(1, collapsed.size());
        System.out.println("sorted ids " + idsOf(sorted) + "; two same-id transactions collapsed to size " + collapsed.size());
    }

    @Test
    @DisplayName("comparator collapse: a ByAmount TreeSet swallows a DISTINCT transaction — contains() true for something never inserted")
    void comparatorCollapseWithoutEquals() {
        Comparator<Transaction> byAmount = new Comparator<Transaction>() {
            @Override
            public int compare(Transaction a, Transaction b) {
                return a.getAmount().compareTo(b.getAmount());
            }
        };
        Transaction latte = new Transaction("T-9001", "Latte", new BigDecimal("4.50"), 1_700_000_100_000L);
        Transaction espresso = new Transaction("T-9002", "Espresso", new BigDecimal("4.50"), 1_700_000_200_000L);

        assertFalse(latte.equals(espresso), "equals() (by id): two DIFFERENT transactions");

        Set<Transaction> set = new TreeSet<Transaction>(byAmount);
        assertTrue(set.add(latte));
        assertFalse(set.add(espresso), "the comparator says tie (0) — and for a TreeSet, tie IS duplicate");
        assertEquals(1, set.size());
        assertTrue(set.contains(espresso), "contains() is TRUE for an object the Set does not hold — comparator, not equals");
        System.out.println("equals=false, compare=0: size " + set.size() + ", contains(the swallowed one) = " + set.contains(espresso));
    }

    @Test
    @DisplayName("BigDecimal 2.0 vs 2.00: HashSet keeps BOTH (equals), TreeSet keeps ONE (compareTo)")
    void bigDecimalScaleTrap() {
        BigDecimal a = new BigDecimal("2.0");
        BigDecimal b = new BigDecimal("2.00");
        assertFalse(a.equals(b), "BigDecimal.equals compares value AND scale");
        assertEquals(0, a.compareTo(b), "compareTo compares numeric value only");

        Set<BigDecimal> hashed = new HashSet<BigDecimal>();
        hashed.add(a);
        hashed.add(b);
        assertEquals(2, hashed.size(), "HashSet asked equals: strangers — both kept");

        Set<BigDecimal> sorted = new TreeSet<BigDecimal>();
        sorted.add(a);
        sorted.add(b);
        assertEquals(1, sorted.size(), "TreeSet asked compareTo: tie — one swallowed");

        // Deterministic membership without ever iterating the HashSet in an assertion:
        assertTrue(sorted.contains(a), "the first-added instance survived...");
        assertTrue(sorted.contains(b), "...and contains(b) is true TOO — compareTo 0 means 'present' to a TreeSet");
        System.out.println("HashSet size = " + hashed.size() + ", TreeSet size = " + sorted.size()
                + " — same two objects, opposite verdicts, one law each");
    }

    @Test
    @DisplayName("set algebra: addAll is union, retainAll intersection, removeAll difference — and they MUTATE")
    void setAlgebraIsExact() {
        Set<String> october = new LinkedHashSet<String>(Arrays.asList("ACC-1005", "ACC-1011", "ACC-1027", "ACC-1033"));
        Set<String> november = new LinkedHashSet<String>(Arrays.asList("ACC-1027", "ACC-1033", "ACC-1040", "ACC-1046"));

        Set<String> union = new TreeSet<String>(october);
        union.addAll(november);
        assertEquals(new TreeSet<String>(Arrays.asList("ACC-1005", "ACC-1011", "ACC-1027", "ACC-1033", "ACC-1040", "ACC-1046")), union);

        Set<String> intersection = new TreeSet<String>(october);
        intersection.retainAll(november);
        assertEquals(new TreeSet<String>(Arrays.asList("ACC-1027", "ACC-1033")), intersection, "the repeat offenders");

        Set<String> difference = new TreeSet<String>(october);
        difference.removeAll(november);
        assertEquals(new TreeSet<String>(Arrays.asList("ACC-1005", "ACC-1011")), difference);

        // The mutation warning, proven: the originals only survived because we copied first.
        assertEquals(4, october.size(), "october untouched — we mutated COPIES");
        assertTrue(october.containsAll(intersection));
        System.out.println("union " + union + " / intersection " + intersection + " / difference " + difference);
    }

    @Test
    @DisplayName("null: one null is a legal HashSet element; a natural-order TreeSet throws NPE")
    void nullsPerEngine() {
        Set<String> hashed = new HashSet<String>();
        assertTrue(hashed.add(null), "HashSet allows null (it hashes to bucket 0 by convention)");
        assertFalse(hashed.add(null), "...but only ONE: the second null is a duplicate of the first");
        assertEquals(1, hashed.size());
        assertTrue(hashed.contains(null), "and it is findable — LinkedHashSet behaves identically");

        Set<String> sorted = new TreeSet<String>();
        assertThrows(NullPointerException.class, () -> sorted.add(null),
                "the tree must COMPARE the element — comparing null throws, even on an empty TreeSet");
        assertEquals(0, sorted.size(), "nothing was stored");
        System.out.println("HashSet: contains(null)=" + hashed.contains(null) + " size=" + hashed.size()
                + "; TreeSet.add(null) -> NullPointerException (caught)");
    }

    @Test
    @DisplayName("NavigableSet views: headSet/subSet contents are exact, and writes go THROUGH the view")
    void navigableViewsAreLiveWindows() {
        NavigableSet<String> ids = new TreeSet<String>();
        for (Transaction t : Transaction.sampleStatement()) {
            ids.add(t.getId());
        }
        // sorted: T-1005, T-1011, T-1027, T-1033, T-1040
        assertEquals("T-1005", ids.first());
        assertEquals("T-1040", ids.last());
        assertEquals("T-1027", ids.floor("T-1028"), "closest <=");
        assertEquals("T-1033", ids.ceiling("T-1028"), "closest >=");
        assertEquals("T-1011", ids.lower("T-1027"), "STRICTLY below");
        assertEquals("T-1033", ids.higher("T-1027"), "strictly above");
        assertNull(ids.floor("T-1000"), "nothing that low: null, not an exception");

        SortedSet<String> head = ids.headSet("T-1027");            // EXCLUSIVE to-bound
        assertEquals(new TreeSet<String>(Arrays.asList("T-1005", "T-1011")), new TreeSet<String>(head));
        assertEquals(new TreeSet<String>(Arrays.asList("T-1011", "T-1027")),
                new TreeSet<String>(ids.subSet("T-1011", "T-1033")), "half-open [from, to)");

        head.add("T-1020");
        assertTrue(ids.contains("T-1020"), "the view is a LIVE WINDOW: head.add wrote into the original");
        assertThrows(IllegalArgumentException.class, () -> head.add("T-1099"), "outside the view's range");
        System.out.println("head " + head + " after writing through; full set now " + ids);
    }

    @Test
    @DisplayName("corrupted set: mutate an element's id after insert — it is IN the set and UNREACHABLE")
    void mutatingIdentityFieldsCorruptsTheSet() {
        Set<CorruptedSetDemo.Key> keys = new HashSet<CorruptedSetDemo.Key>();
        CorruptedSetDemo.Key key = new CorruptedSetDemo.Key("K-1");
        keys.add(key);
        assertTrue(keys.contains(new CorruptedSetDemo.Key("K-1")), "before the mutation: found via equals+hashCode");

        key.setId("K-2");   // <-- the one fatal line

        assertFalse(keys.contains(new CorruptedSetDemo.Key("K-1")),
                "right bucket, wrong answer: the element's equals now says 'I am K-2'");
        assertEquals(1, keys.size(), "the element never left — the set is corrupted, not emptied");
        assertFalse(keys.remove(new CorruptedSetDemo.Key("K-2")),
                "you cannot even remove it by the new name: that hashes to a different bucket");
        assertEquals(1, keys.size());

        // Iteration still hands it back — proof the element is physically inside:
        boolean sawIt = false;
        for (CorruptedSetDemo.Key k : keys) {
            sawIt = sawIt || k == key;
        }
        assertTrue(sawIt, "the iterator finds what lookup cannot");
        System.out.println("after setId(\"K-2\"): contains(old)=false, size=" + keys.size() + ", iterator still sees it");
    }
}
