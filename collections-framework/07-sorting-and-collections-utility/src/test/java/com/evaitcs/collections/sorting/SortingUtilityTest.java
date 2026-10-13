package com.evaitcs.collections.sorting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 07-sorting-and-collections-utility test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC — including the shuffle, which is why it is
 * seeded with new Random(42): a seeded generator replays the same permutation on every
 * run and every machine. The unsorted-binarySearch garbage is asserted on a chosen list,
 * not on whatever the RNG felt like today.
 */
class SortingUtilityTest {

    /** Classic idiom, same as the demo: named class, no lambda. Long.compare — never (int)(a-b). */
    private static class ByTimestamp implements Comparator<Transaction> {
        @Override
        public int compare(Transaction a, Transaction b) {
            return Long.compare(a.getTimestampMillis(), b.getTimestampMillis());
        }
    }

    private static List<String> idsOf(List<Transaction> transactions) {
        List<String> ids = new ArrayList<String>();
        for (Transaction t : transactions) {
            ids.add(t.getId());
        }
        return ids;
    }

    @Test
    @DisplayName("sort is STABLE: the ts=2000 tie keeps arrival order (T-1001 before T-1004)")
    void sortIsStableOnTies() {
        List<Transaction> statement = Transaction.sampleStatement();

        // Natural order first: Comparable by id (core-java 10) — exact, asserted.
        Collections.sort(statement);
        assertEquals(Arrays.asList("T-1001", "T-1002", "T-1003", "T-1004", "T-1005"), idsOf(statement));

        // The stability experiment: sort a FRESH statement by timestamp. T-1001 (input
        // index 0) and T-1004 (input index 3) TIE at ts=2000. A stable sort must keep
        // their relative order; an unstable one (like the classic quicksort) may not.
        List<Transaction> byTime = Transaction.sampleStatement();
        Collections.sort(byTime, new ByTimestamp());
        assertEquals(Arrays.asList("T-1005", "T-1002", "T-1001", "T-1004", "T-1003"), idsOf(byTime),
                "500, 1000, 2000(T-1001 first — arrival order), 2000(T-1004), 3000");
        System.out.println("by timestamp: " + idsOf(byTime) + " — the tie kept T-1001 before T-1004 (TimSort is stable)");
    }

    @Test
    @DisplayName("binarySearch on a SORTED list returns the exact index of the key")
    void binarySearchFindsExactIndex() {
        List<String> ids = Arrays.asList("T-1001", "T-1002", "T-1003", "T-1004", "T-1005");

        int index = Collections.binarySearch(ids, "T-1004");
        assertEquals(3, index, ">= 0 means found, at exactly this index");
        assertEquals("T-1004", ids.get(index));
        assertEquals(0, Collections.binarySearch(ids, "T-1001"), "the first element is index 0 — and 0 is NOT a miss");
        System.out.println("binarySearch(T-1004) -> " + index + "; binarySearch(T-1001) -> 0 (found, not 'absent')");
    }

    @Test
    @DisplayName("a miss returns -(insertionPoint)-1 — verified against a manual scan")
    void missEncodingIsInsertionPointMinusOne() {
        List<String> ids = new ArrayList<String>(Arrays.asList("T-1001", "T-1002", "T-1003", "T-1004", "T-1005"));
        String key = "T-10028";   // sorts after T-1002 (longer, same prefix) and before T-1003 ('2' < '3')

        int result = Collections.binarySearch(ids, key);
        assertTrue(result < 0, "the key is absent, so the result is negative");

        // Manual computation of the insertion point: the first index whose element is
        // GREATER than the key. This is the spec binarySearch promises to encode.
        int insertionPoint = ids.size();
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i).compareTo(key) > 0) {
                insertionPoint = i;
                break;
            }
        }
        assertEquals(2, insertionPoint, "T-1003 at index 2 is the first element greater than T-10028");
        assertEquals(-(insertionPoint) - 1, result, "the encoding formula, exactly: -(ip)-1");
        assertEquals(-3, result);

        // And the decoded point is MEANINGFUL: inserting there keeps the list sorted.
        ids.add(insertionPoint, key);
        List<String> resorted = new ArrayList<String>(ids);
        Collections.sort(resorted);
        assertEquals(resorted, ids, "add(ip, key) left the list sorted — the whole point of the encoding");
        System.out.println("miss result = " + result + " -> decoded insertionPoint = " + (-(result) - 1) + "; list after insert: " + ids);
    }

    @Test
    @DisplayName("binarySearch on an UNSORTED list: silently wrong, no exception (chosen data)")
    void binarySearchOnUnsortedIsSilentGarbage() {
        List<String> unsorted = Arrays.asList("T-1005", "T-1001", "T-1004", "T-1002", "T-1003");

        int result = Collections.binarySearch(unsorted, "T-1003");
        assertEquals(-1, result, "reports 'not found'...");
        assertTrue(unsorted.contains("T-1003"), "...for an element that IS in the list (index 4)");
        assertEquals(4, unsorted.indexOf("T-1003"));

        int result2 = Collections.binarySearch(unsorted, "T-1005");
        assertEquals(-6, result2, "and decodes to insertionPoint 5 for the element AT INDEX 0");
        System.out.println("unsorted: search T-1003 -> " + result + " (present at index 4!); search T-1005 -> " + result2
                + " (present at index 0!) — garbage, silently, every run");
    }

    @Test
    @DisplayName("seeded shuffle: two Random(42) shuffles agree, and the result is a pure rearrangement")
    void seededShuffleIsReproduciblePermutation() {
        List<String> a = idsOf(Transaction.sampleStatement());
        List<String> b = idsOf(Transaction.sampleStatement());

        Collections.shuffle(a, new Random(42));
        Collections.shuffle(b, new Random(42));
        assertEquals(a, b, "same seed -> same permutation, in exact list order");

        // A shuffle must REARRANGE, never lose/duplicate/mint: the sorted forms must match
        // the sorted original — that is "same multiset" without depending on any order.
        List<String> original = idsOf(Transaction.sampleStatement());
        List<String> shuffledSorted = new ArrayList<String>(a);
        Collections.sort(shuffledSorted);
        Collections.sort(original);
        assertEquals(original, shuffledSorted, "the shuffle is a permutation of the original");
        System.out.println("Random(42) shuffle (identical twice): " + a);
    }

    @Test
    @DisplayName("unmodifiableList is a live WINDOW: add throws, but parent mutations show through")
    void unmodifiableIsALiveWindowNotACopy() {
        List<Account> internal = Account.sampleAccounts();
        List<Account> readOnly = Collections.unmodifiableList(internal);

        assertThrows(UnsupportedOperationException.class,
                () -> readOnly.add(new Account("ACC-3000", "Mallory", BigDecimal.ONE)));
        assertThrows(UnsupportedOperationException.class, () -> readOnly.remove(0));

        // THE trap: the wrapper never copied anything — mutating the ORIGINAL is visible
        // through the "read-only" view. Unmodifiable describes the window, not the data.
        assertEquals(3, readOnly.size());
        internal.add(new Account("ACC-3000", "Mallory", BigDecimal.ONE));
        assertEquals(4, readOnly.size(), "the original grew — and the 'read-only' view shows it");
        assertTrue(readOnly.contains(new Account("ACC-3000", "different owner", BigDecimal.ZERO)),
                "contains() by equals-by-id (module 01): Mallory is visible through the window");
        System.out.println("readOnly.add -> UnsupportedOperationException, yet readOnly.size() = " + readOnly.size()
                + " after internal.add — a live window, not a copy");

        // The snapshot fix: copy FIRST, then wrap — the wire is cut.
        List<Account> frozen = Collections.unmodifiableList(new ArrayList<Account>(internal));
        internal.add(new Account("ACC-4000", "Late", BigDecimal.ONE));
        assertEquals(4, frozen.size(), "the snapshot never hears about later changes");
    }

    @Test
    @DisplayName("emptyList/singletonList/nCopies are TRUE immutables — even set() throws")
    void immutableHelpersAreFrozen() {
        List<Account> none = Collections.emptyList();
        assertEquals(0, none.size());
        assertThrows(UnsupportedOperationException.class, () -> none.add(Account.sampleAccounts().get(0)));

        Account dana = Account.sampleAccounts().get(0);
        List<Account> one = Collections.singletonList(dana);
        assertEquals(1, one.size());
        assertSame(dana, one.get(0));
        assertThrows(UnsupportedOperationException.class,
                () -> one.set(0, new Account("ACC-9999", "Mallory", BigDecimal.ZERO)),
                "even REPLACING the only slot throws — there is no array, just the element");

        List<Account> three = Collections.nCopies(3, dana);
        assertEquals(3, three.size());
        assertEquals(Arrays.asList(dana, dana, dana), three);
        assertSame(three.get(0), three.get(2), "nCopies stores ONE instance n times — ==, not copies");
        assertThrows(UnsupportedOperationException.class, () -> three.set(1, dana));
        System.out.println("emptyList/singletonList/nCopies: every mutator throws, from every direction — no original exists");
    }

    @Test
    @DisplayName("min/max/frequency/disjoint — exact answers on statement data")
    void gamesGiveExactAnswers() {
        List<Transaction> statement = Transaction.sampleStatement();

        assertEquals("T-1001", Collections.min(statement).getId(), "natural order = by id");
        assertEquals("T-1005", Collections.max(statement).getId());
        assertEquals("T-1005", Collections.min(statement, new ByTimestamp()).getId(), "earliest ts=500");
        assertEquals("T-1003", Collections.max(statement, new ByTimestamp()).getId(), "latest ts=3000");

        List<String> touched = Arrays.asList("ACC-1005", "ACC-1027", "ACC-1005", "ACC-1011", "ACC-1005");
        assertEquals(3, Collections.frequency(touched, "ACC-1005"), "counted by equals(), not ==");
        assertEquals(0, Collections.frequency(touched, "ACC-9999"), "absent = 0, never an error");

        List<String> weekday = Arrays.asList("T-1001", "T-1002", "T-1003");
        List<String> weekend = Arrays.asList("T-1004", "T-1005");
        assertTrue(Collections.disjoint(weekday, weekend), "nothing in common");
        assertFalse(Collections.disjoint(weekday, Arrays.asList("T-1003", "T-1009")), "they share T-1003");
        System.out.println("min/max exact (by id AND by timestamp), frequency(ACC-1005)=3, disjoint both ways");
    }
}
