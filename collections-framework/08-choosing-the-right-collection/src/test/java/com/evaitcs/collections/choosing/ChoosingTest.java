package com.evaitcs.collections.choosing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 08-choosing-the-right-collection test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Two house rules are visible here. (1) Determinism: everything asserted is a contract
 * or a chosen-value fact — iteration order is asserted ONLY where the container
 * promises it (LinkedHashSet insertion order), never for HashMap/HashSet, and NO test
 * asserts a timing (the stopwatch lives in EmpiricalContainsDemo, printed with caveats).
 * (2) Reflection stays on the surface: getMethod + Modifier reads PUBLIC method flags;
 * nothing here ever calls setAccessible or pokes into java.util internals.
 */
class ChoosingTest {

    @Test
    @DisplayName("Vector pays for locks (public methods synchronized); ArrayList does not")
    void vectorIsSynchronizedArrayListIsNot() throws Exception {
        // Public-API reflection: the synchronized flag is part of a method's public
        // modifiers, so reading it needs no privileges and touches no internals.
        Method vectorGet = Vector.class.getMethod("get", int.class);
        Method vectorAdd = Vector.class.getMethod("add", Object.class);
        Method vectorInsert = Vector.class.getMethod("insertElementAt", Object.class, int.class);
        Method listGet = ArrayList.class.getMethod("get", int.class);
        Method listAdd = ArrayList.class.getMethod("add", Object.class);

        assertTrue(Modifier.isSynchronized(vectorGet.getModifiers()), "Vector.get(int) holds a lock per call");
        assertTrue(Modifier.isSynchronized(vectorAdd.getModifiers()), "Vector.add(E) holds a lock per call");
        // The honest nuance: add(int,E) is NOT itself flagged synchronized — it delegates
        // to insertElementAt, which IS. Every path to the data still pays the lock.
        assertFalse(Modifier.isSynchronized(Vector.class.getMethod("add", int.class, Object.class).getModifiers()),
                "add(int,E) delegates instead of locking itself...");
        assertTrue(Modifier.isSynchronized(vectorInsert.getModifiers()), "...and the delegatee IS synchronized");

        assertFalse(Modifier.isSynchronized(listGet.getModifiers()), "ArrayList.get(int): no lock");
        assertFalse(Modifier.isSynchronized(listAdd.getModifiers()), "ArrayList.add(E): no lock");
        System.out.println("Vector.get/add(E)/insertElementAt synchronized=true; ArrayList.get/add=false — same Big-O row, one pays a lock");
    }

    @Test
    @DisplayName("Hashtable bans null keys AND values (NPE); HashMap accepts both")
    void hashtableBansNullsHashMapAllowsThem() {
        final Hashtable<String, String> table = new Hashtable<String, String>();
        assertThrows(NullPointerException.class, () -> table.put(null, "value"), "null key -> NPE, on principle");
        assertThrows(NullPointerException.class, () -> table.put("key", null), "null value -> NPE too");

        Map<String, String> map = new HashMap<String, String>();
        map.put(null, "null key accepted");
        map.put("key", null);
        assertEquals("null key accepted", map.get(null));
        assertTrue(map.containsKey("key"));
        assertNull(map.get("key"), "stored null — and get() cannot tell it from 'absent' (module 05's fine print)");
        assertFalse(map.containsKey("never-added"), "...containsKey settles the ambiguity");
        System.out.println("Hashtable: NPE on both nulls; HashMap: both legal, with the get()==null ambiguity to manage");
    }

    @Test
    @DisplayName("the Integer cache: valueOf(127)==valueOf(127) is TRUE, valueOf(128)==valueOf(128) is FALSE")
    void integerCacheIdentityTrap() {
        Integer a127 = Integer.valueOf(127);
        Integer b127 = Integer.valueOf(127);
        Integer a128 = Integer.valueOf(128);
        Integer b128 = Integer.valueOf(128);

        assertSame(a127, b127, "inside the cache (-128..127): the SAME shared object");
        assertTrue(a127 == b127, "== is reference comparison — and here it 'works', by cache luck");
        assertFalse(a128 == b128, "one past the cache: two objects, == says false for equal VALUES");
        assertEquals(a128, b128, "equals() compares values — the only correct comparison, cached or not");
        assertSame(Integer.valueOf(-128), Integer.valueOf(-128), "the cache's low end is -128...");
        assertNotSame(Integer.valueOf(-129), Integer.valueOf(-129), "...one below it: fresh objects again");
        System.out.println("valueOf(127) == valueOf(127): true (cached) | valueOf(128) == valueOf(128): false (not cached)");
        System.out.println("Never compare boxed values with == — unbox or use equals(). (core-java 02, harvested.)");
    }

    @Test
    @DisplayName("List<Integer>: remove(0) removes the SLOT; remove(Integer.valueOf(0)) removes the VALUE")
    void removeOverloadsOnListOfInteger() {
        List<Integer> nums = new ArrayList<Integer>(Arrays.asList(
                Integer.valueOf(10), Integer.valueOf(0), Integer.valueOf(30)));

        Integer bySlot = nums.remove(0);                       // int literal -> remove(int INDEX)
        assertEquals(Integer.valueOf(10), bySlot, "slot 0 held 10 — the INDEX overload won the int literal");
        assertEquals(Arrays.asList(Integer.valueOf(0), Integer.valueOf(30)), nums);

        boolean byValue = nums.remove(Integer.valueOf(0));     // Integer -> remove(Object VALUE)
        assertTrue(byValue, "the value 0 was found and removed...");
        assertEquals(Arrays.asList(Integer.valueOf(30)), nums,
                "the VALUE overload finds 0 wherever it lives; the INDEX overload only ever means 'slot 0'");
        System.out.println("[10,0,30]: remove(0) -> 10 (slot); remove(Integer.valueOf(0)) -> value 0 gone; left: " + nums);
    }

    @Test
    @DisplayName("three getters, three contracts: leak mutates the bank, copy doesn't, window throws but sees")
    void defensiveCopyVsWindowVsLeak() {
        AccountRegistry registry = new AccountRegistry();
        assertEquals(3, registry.size());

        // The leak: the getter WAS the internal list, so the caller writes the bank's state.
        List<Account> leaky = registry.getAccountsLeaky();
        leaky.add(new Account("ACC-3000", "Mallory", BigDecimal.ONE));
        assertEquals(4, registry.size(), "the 'getter' mutated the registry — that is the bug");
        leaky.remove(leaky.size() - 1);

        // The snapshot: caller mutations die in the copy; internal changes never arrive.
        List<Account> copy = registry.getAccountsCopy();
        copy.add(new Account("ACC-4000", "Fay", BigDecimal.ONE));
        assertEquals(3, registry.size(), "the copy absorbed the write — internal state untouched");
        registry.addAccount(new Account("ACC-5000", "Gus", BigDecimal.ONE));
        assertEquals(4, copy.size(), "Fay lives in the copy, Gus never reached it — the snapshot froze at call time");
        assertFalse(copy.contains(new Account("ACC-5000", "anyone", BigDecimal.ZERO)), "Gus is in the registry, not in the old copy");

        // The window (module 07): writes throw, internal changes show THROUGH.
        List<Account> readOnly = registry.getAccountsReadOnly();
        assertEquals(4, readOnly.size(), "the window sees the live list (Dana, Ana, Bo, Gus)");
        assertThrows(UnsupportedOperationException.class,
                () -> readOnly.add(new Account("ACC-6000", "Mallory", BigDecimal.ONE)));
        registry.addAccount(new Account("ACC-7000", "Hal", BigDecimal.ONE));
        assertEquals(5, readOnly.size(), "the window GREW — unmodifiable describes the view, not the data");
        assertTrue(readOnly.contains(new Account("ACC-7000", "anyone", BigDecimal.ZERO)), "equals by id finds Hal through the window");
        System.out.println("leak: caller's add wrote the registry (3 -> 4); copy: Fay died in the copy, Gus never reached it; window: threw on add, then grew to 5 with Hal");
    }

    @Test
    @DisplayName("the scenario picks behave as promised: LinkedHashSet keeps first-seen order, HashMap finds by id")
    void scenarioPicksBehaveAsPromised() {
        // Scenario 2: unique customer ids, first-seen order. LinkedHashSet is asserted on
        // iteration order BECAUSE it promises insertion order (module 04) — a HashSet's
        // order would be unspecified and this test would be flaky by design.
        Set<String> seen = new LinkedHashSet<String>();
        for (String id : Arrays.asList("C-07", "C-03", "C-07", "C-11", "C-03")) {
            seen.add(id);
        }
        assertEquals(3, seen.size(), "duplicates collapsed by equals/hashCode");
        assertEquals(Arrays.asList("C-07", "C-03", "C-11"), new ArrayList<String>(seen),
                "first-seen order preserved, exactly");

        // Scenario 3: id -> Account, one hop. The lookup works with ANY equal key because
        // HashMap locates by hashCode and confirms by equals (module 05 / core-java 08).
        Map<String, Account> byId = new HashMap<String, Account>();
        for (Account a : Account.sampleAccounts()) {
            byId.put(a.getId(), a);
        }
        Account found = byId.get("ACC-1011");
        assertEquals("Bo", found.getOwner());
        assertNull(byId.get("ACC-9999"), "absent key -> null, never an exception");

        // A stranger Account with the same id IS the same account — the value side of equals too.
        Map<Account, String> owners = new HashMap<Account, String>();
        owners.put(new Account("ACC-1027", "Dana", new BigDecimal("2450.00")), "dana@securebank");
        assertEquals("dana@securebank", owners.get(new Account("ACC-1027", "different owner", BigDecimal.ZERO)),
                "found by equals-by-id despite different fields (module 01's engine, module 05's buckets)");
        System.out.println("LinkedHashSet: [C-07, C-03, C-11] first-seen order; HashMap: Bo found by id, stranger key works");
    }
}
