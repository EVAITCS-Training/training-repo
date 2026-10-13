package com.evaitcs.collections.why;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 01-why-collections test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC — including the failures: the
 * ArrayStoreException, the UnsupportedOperationException from Arrays.asList and the
 * identity-vs-equals contains results happen on chosen values, every single run.
 */
class WhyCollectionsTest {

    private static List<String> idsOf(Collection<Account> accounts) {
        List<String> ids = new ArrayList<String>();
        for (Account a : accounts) {
            ids.add(a.getId());
        }
        return ids;
    }

    @Test
    @DisplayName("arrays are fixed size: the only 'add' is allocate-and-copy")
    void arraysAreFixedSize() {
        Account[] accounts = new Account[2];
        assertEquals(2, accounts.length);

        // The manual "resize" the demo performs: length NEVER grows in place.
        Account[] bigger = Arrays.copyOf(accounts, 3);
        assertEquals(2, accounts.length, "the original is untouched");
        assertEquals(3, bigger.length, "a brand-new array had to be allocated");
        System.out.println("length before = " + accounts.length + ", after copyOf(3) = " + bigger.length);

        // And out-of-bounds access is a runtime crash, not a grow:
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> accounts[2] = new Account("ACC-1", "X", BigDecimal.ONE));
    }

    @Test
    @DisplayName("removing from an array is manual shifting; List.remove does it for you")
    void removingFromArrayIsManualShifting() {
        Account[] accounts = Account.sampleAccounts().toArray(new Account[0]);
        String removedManually = accounts[1].getId();

        System.arraycopy(accounts, 2, accounts, 1, accounts.length - 2);
        accounts[accounts.length - 1] = null;

        List<Account> list = Account.sampleAccounts();
        Account removedByList = list.remove(1);

        assertEquals(removedManually, removedByList.getId(), "same element gone");
        assertEquals(2, list.size(), "the List tracks its OWN size...");
        assertEquals(3, accounts.length, "...the array still claims its capacity — null tail included");
        System.out.println("after remove(1): list = " + idsOf(list) + ", array length = " + accounts.length);
    }

    @Test
    @DisplayName("arrays are covariant: wrong element type is a RUNTIME ArrayStoreException")
    void covariantArraysFailAtRuntime() {
        String[] names = { "Dana", "Ana" };
        Object[] objects = names;                     // compiles — covariance
        ArrayStoreException e = assertThrows(ArrayStoreException.class,
                () -> objects[0] = Integer.valueOf(42));
        System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());

        // The List world is invariant: List<String> is NOT a List<Object>, so the
        // compiler rejects the same program. (Shown as a type fact, not a mutation.)
        List<String> strings = new ArrayList<String>();
        Collection<?> widened = strings;              // only via wildcard — and it is read-only-ish
        assertTrue(widened.isEmpty());
        System.out.println("List<String> -> List<Object> does not compile; the seatbelt is the type system.");
    }

    @Test
    @DisplayName("Arrays.asList is a fixed-size VIEW: set() works, add() throws")
    void asListIsAFixedSizeView() {
        Account[] trio = Account.sampleAccounts().toArray(new Account[0]);
        List<Account> view = Arrays.asList(trio);

        Account eve = new Account("ACC-2000", "Eve", new BigDecimal("5.00"));
        view.set(0, eve);                             // allowed: same slot
        assertEquals(eve, trio[0], "the write went THROUGH the view into the array");

        assertThrows(UnsupportedOperationException.class, () -> view.add(eve));
        assertThrows(UnsupportedOperationException.class, () -> view.remove(0));
        System.out.println("set() wrote through to trio[0] = " + trio[0] + "; add()/remove() -> UnsupportedOperationException");

        // The copy idiom is a real, growable List:
        List<Account> real = new ArrayList<Account>(view);
        real.add(eve);
        assertEquals(4, real.size());
    }

    @Test
    @DisplayName("the hierarchy: every container IS a Collection IS an Iterable (except Map)")
    void hierarchyClaims() {
        List<Account> array = new ArrayList<Account>();
        assertTrue(array instanceof Collection);
        assertTrue(array instanceof Iterable);

        LinkedList<Account> linked = new LinkedList<Account>();
        assertTrue(linked instanceof List, "LinkedList's first hat: indexed List");
        assertTrue(linked instanceof Deque, "LinkedList's second hat: two-ended Deque");
        assertTrue(linked instanceof Queue, "Deque extends Queue — so offer()/poll() are visible too");

        assertFalse(new java.util.HashMap<String, String>() instanceof Collection,
                "Map is NOT a Collection — it hands out Collections via keySet()/values()/entrySet()");
        System.out.println("ArrayList: List+Collection+Iterable; LinkedList: List AND Deque/Queue; HashMap: not a Collection");
    }

    @Test
    @DisplayName("swap the implementation, keep the code: ArrayList vs LinkedList behave identically as List")
    void swapImplementationSameCode() {
        List<Account> a = new ArrayList<Account>(Account.sampleAccounts());
        List<Account> l = new LinkedList<Account>(Account.sampleAccounts());

        assertEquals(a, l, "List.equals is by elements+order, not by implementation class");
        assertEquals(idsOf(a), idsOf(l));
        System.out.println("same List code, two engines: " + idsOf(a) + " == " + idsOf(l));
    }

    @Test
    @DisplayName("contains/remove use equals(), not ==: an equal stranger is found and removed")
    void containsUsesEqualsNotIdentity() {
        List<Account> accounts = Account.sampleAccounts();
        Account stranger = new Account("ACC-1011", "Not Bo At All", BigDecimal.ZERO);

        assertTrue(accounts.contains(stranger), "equals by id -> found");
        assertTrue(accounts.remove(stranger), "and removed the ORIGINAL Bo");
        assertFalse(accounts.contains(new Account("ACC-1011", "x", BigDecimal.ZERO)));
        assertEquals(Arrays.asList("ACC-1027", "ACC-1005"), idsOf(accounts));
        System.out.println("stranger " + stranger.getId() + " removed the original; ids now " + idsOf(accounts));

        // Control group: no equals override -> identity only (Object.equals is ==).
        List<Object> raw = new ArrayList<Object>();
        Object original = new Object();
        raw.add(original);
        assertTrue(raw.contains(original), "the very instance is found");
        assertFalse(raw.contains(new Object()), "an equal-looking stranger is NOT");
        assertNotEquals(original, new Object());
    }

    @Test
    @DisplayName("List.equals is order-sensitive; Set.equals is not; List never equals Set")
    void collectionEqualityRules() {
        List<Account> a = Account.sampleAccounts();
        List<Account> b = Account.sampleAccounts();
        assertEquals(a, b, "identically built lists are equal");

        Collections.swap(b, 0, 1);
        assertNotEquals(a, b, "one swap and List.equals says NO — order is part of a List's identity");

        Set<Account> s1 = new LinkedHashSet<Account>(a);
        Set<Account> s2 = new LinkedHashSet<Account>(b);
        assertEquals(s1, s2, "Set.equals: same elements, any order");

        assertFalse(a.equals(s1), "List.equals checks 'is it a List?' first — cross-interface is always false");
        assertFalse(s1.equals(a), "...and symmetric: Set.equals asks for a Set");
        System.out.println("List order-sensitivity and Set order-insensitivity, plus the cross-interface wall: proven");
    }

    @Test
    @DisplayName("toArray: typed overload gives Account[], no-arg gives Object[]")
    void toArrayBridge() {
        List<Account> accounts = Account.sampleAccounts();

        Account[] typed = accounts.toArray(new Account[0]);
        Object[] untyped = accounts.toArray();

        assertEquals(3, typed.length);
        assertEquals(Account.class, typed[0].getClass(), "typed array — usable where Account[] is demanded");
        assertEquals(Object[].class, untyped.getClass(), "no-arg overload is Object[] only");
        System.out.println("typed: " + Arrays.toString(typed));
    }

    @Test
    @DisplayName("queue interface on LinkedList: offer/poll is FIFO, same object as the List above")
    void oneObjectSeveralInterfaces() {
        Queue<Account> fifo = new LinkedList<Account>();
        for (Account a : Account.sampleAccounts()) {
            fifo.offer(a);
        }
        assertEquals("ACC-1027", fifo.poll().getId(), "first in, first out");
        assertEquals("ACC-1005", fifo.poll().getId());
        assertEquals(1, fifo.size());
        System.out.println("LinkedList used AS a Queue: FIFO order proven (module 06 goes deep)");
    }
}
