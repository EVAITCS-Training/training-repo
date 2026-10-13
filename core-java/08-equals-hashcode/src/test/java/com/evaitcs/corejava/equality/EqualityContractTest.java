package com.evaitcs.corejava.equality;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 08-equals-hashcode test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC. The "broken" failures (get -> null, set size
 * wrong) are not flaky: default Object.hashCode is identity-based, so two distinct objects
 * land in different buckets and the lookup misses every single time. That reliability is
 * the whole point — a contract violation is a bug you can write a test for.
 */
class EqualityContractTest {

    // ---------------------------------------------------------------------
    // The FIVE contract properties, each on the FIXED class (AccountKey).
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("reflexive: x.equals(x) is true")
    void reflexive() {
        AccountKey x = new AccountKey("BR-01", "1001");
        System.out.println("x.equals(x) = " + x.equals(x));
        assertTrue(x.equals(x), "an object must equal itself");
    }

    @Test
    @DisplayName("symmetric: x.equals(y) == y.equals(x) for equal-content keys")
    void symmetric() {
        AccountKey x = new AccountKey("BR-01", "1001");
        AccountKey y = new AccountKey("BR-01", "1001");
        System.out.println("x.equals(y) = " + x.equals(y) + ", y.equals(x) = " + y.equals(x));
        assertTrue(x.equals(y));
        assertTrue(y.equals(x));
        assertEquals(x.equals(y), y.equals(x), "equality must not depend on direction");
    }

    @Test
    @DisplayName("transitive: x~y and y~z implies x~z")
    void transitive() {
        AccountKey x = new AccountKey("BR-01", "1001");
        AccountKey y = new AccountKey("BR-01", "1001");
        AccountKey z = new AccountKey("BR-01", "1001");
        boolean chain = x.equals(y) && y.equals(z);
        System.out.println("x~y=" + x.equals(y) + ", y~z=" + y.equals(z) + ", x~z=" + x.equals(z));
        assertTrue(chain);
        assertTrue(x.equals(z), "x~y and y~z must give x~z");
    }

    @Test
    @DisplayName("consistent: repeated calls agree, and equal objects keep equal hashCodes")
    void consistent() {
        AccountKey x = new AccountKey("BR-01", "1001");
        AccountKey y = new AccountKey("BR-01", "1001");
        boolean stable = true;
        for (int i = 0; i < 100; i++) {
            stable &= x.equals(y);
        }
        System.out.println("x.equals(y) 100 times = all " + stable
                + "; x.hashCode()=" + x.hashCode() + ", y.hashCode()=" + y.hashCode());
        assertTrue(stable, "equals must not flicker across calls");
        assertEquals(x.hashCode(), y.hashCode(), "equal objects MUST have equal hashCodes");
    }

    @Test
    @DisplayName("null: x.equals(null) is false (never true, never throws)")
    void nullIsFalse() {
        AccountKey x = new AccountKey("BR-01", "1001");
        System.out.println("x.equals(null) = " + x.equals(null));
        assertFalse(x.equals(null), "nothing equals null");
    }

    @Test
    @DisplayName("non-match: a genuinely different key is NOT equal")
    void differentKeysAreNotEqual() {
        AccountKey x = new AccountKey("BR-01", "1001");
        AccountKey w = new AccountKey("BR-09", "9999");
        System.out.println("x.equals(w) = " + x.equals(w) + " (different account)");
        assertFalse(x.equals(w));
        assertFalse(x.equals("BR-01/1001"), "a different TYPE must not equal an AccountKey");
    }

    // ---------------------------------------------------------------------
    // The FIXED class survives the HashMap round-trip with an equal-but-distinct key.
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("FIXED: put with one key, get with an EQUAL-but-distinct key finds the value")
    void fixedHashMapRoundTrip() {
        Map<AccountKey, Transaction> balances = new HashMap<AccountKey, Transaction>();
        AccountKey stored = new AccountKey("BR-01", "1001");
        balances.put(stored, Transaction.payroll());

        AccountKey lookup = new AccountKey("BR-01", "1001");   // equal content, distinct object
        assertFalse(stored == lookup, "must be two different objects, not the same reference");

        Transaction found = balances.get(lookup);
        System.out.println("get(equal-but-distinct key) = " + found);
        assertNotNull(found, "the equal key MUST find the stored value");
        assertEquals("T-1001", found.getId());
        assertTrue(balances.containsKey(lookup));
    }

    @Test
    @DisplayName("FIXED: a HashSet dedupes equal-content keys to size 1")
    void fixedHashSetDedupes() {
        Set<AccountKey> seen = new HashSet<AccountKey>();
        seen.add(new AccountKey("BR-01", "1001"));
        seen.add(new AccountKey("BR-01", "1001"));
        seen.add(new AccountKey("BR-01", "1001"));
        System.out.println("set size after adding the SAME account 3x = " + seen.size());
        assertEquals(1, seen.size(), "equal elements must collapse to one");
    }

    // ---------------------------------------------------------------------
    // The BROKEN class (equals only) FAILS lookup — deterministically.
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("BROKEN: get with an equal key returns NULL (identity hash -> wrong bucket)")
    void brokenHashMapLosesTheValue() {
        Map<BrokenAccountKey, Transaction> balances = new HashMap<BrokenAccountKey, Transaction>();
        BrokenAccountKey stored = new BrokenAccountKey("BR-01", "1001");
        balances.put(stored, Transaction.payroll());

        BrokenAccountKey lookup = new BrokenAccountKey("BR-01", "1001");
        // Prove equals is CORRECT — the failure is purely the missing hashCode.
        assertTrue(stored.equals(lookup), "equals itself works");
        System.out.println("equals=" + stored.equals(lookup)
                + " but stored.hashCode=" + stored.hashCode()
                + " lookup.hashCode=" + lookup.hashCode());

        Transaction found = balances.get(lookup);
        System.out.println("BROKEN get(equal key) = " + found + "  <-- the map lost your money");
        assertNull(found, "missing hashCode => equal key hashes to another bucket => not found");
        assertFalse(balances.containsKey(lookup));
        assertEquals(1, balances.size(), "the entry is still in there, just unreachable");
    }

    @Test
    @DisplayName("BROKEN: a HashSet accepts 'duplicates' — size grows past the distinct count")
    void brokenHashSetDoesNotDedupe() {
        Set<BrokenAccountKey> seen = new HashSet<BrokenAccountKey>();
        seen.add(new BrokenAccountKey("BR-01", "1001"));
        seen.add(new BrokenAccountKey("BR-01", "1001"));
        seen.add(new BrokenAccountKey("BR-01", "1001"));
        System.out.println("BROKEN set size after adding the SAME account 3x = " + seen.size());
        assertEquals(3, seen.size(), "identity hashCode => three distinct buckets => no dedupe");
    }

    // ---------------------------------------------------------------------
    // The other traps the module names, each proven.
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("DEFAULT: a class overriding nothing uses identity — equal content is NOT equal")
    void defaultIsIdentity() {
        DefaultIdentityDemo.PlainKey a = new DefaultIdentityDemo.PlainKey("BR-01", "1001");
        DefaultIdentityDemo.PlainKey b = new DefaultIdentityDemo.PlainKey("BR-01", "1001");
        System.out.println("default a.equals(b) = " + a.equals(b)
                + "; identityHashCode " + System.identityHashCode(a) + " vs " + System.identityHashCode(b));
        assertFalse(a.equals(b), "Object.equals is == : two objects are never equal");
        assertTrue(a.equals(a), "...but an object still equals itself");
    }

    @Test
    @DisplayName("OVERLOAD TRAP: equals(MyType) is skipped by collections that call equals(Object)")
    void overloadTrapIsNeverCalledByMap() {
        ProperEqualsDemo.OverloadTrapKey t1 = new ProperEqualsDemo.OverloadTrapKey("1001");
        ProperEqualsDemo.OverloadTrapKey t2 = new ProperEqualsDemo.OverloadTrapKey("1001");

        // Directly, with a statically-typed arg, the overload runs and says true...
        System.out.println("t1.equals(t2) [overload] = " + t1.equals(t2));
        assertTrue(t1.equals(t2), "the overload itself is correct");

        // ...but through Object (how a Map calls it) the inherited identity equals runs.
        Object asObject = t2;
        System.out.println("t1.equals((Object) t2) [Object.equals] = " + t1.equals(asObject));
        assertFalse(t1.equals(asObject), "no override => identity => false");

        Map<ProperEqualsDemo.OverloadTrapKey, String> map =
                new HashMap<ProperEqualsDemo.OverloadTrapKey, String>();
        map.put(t1, "payroll");
        System.out.println("map.get(new equal key) = " + map.get(new ProperEqualsDemo.OverloadTrapKey("1001")));
        assertNull(map.get(new ProperEqualsDemo.OverloadTrapKey("1001")),
                "the overload is dead code to a HashMap");
    }

    @Test
    @DisplayName("SYMMETRY TRAP: instanceof hierarchy gives p.equals(e)=true but e.equals(p)=false")
    void symmetryTrapIsAsymmetric() {
        SymmetryTrapDemo.Person person = new SymmetryTrapDemo.Person("Ada");
        SymmetryTrapDemo.Employee employee = new SymmetryTrapDemo.Employee("Ada", 7);

        boolean pEqE = person.equals(employee);
        boolean eEqP = employee.equals(person);
        System.out.println("person.equals(employee)=" + pEqE + ", employee.equals(person)=" + eEqP);

        assertTrue(pEqE, "Employee passes `instanceof Person`");
        assertFalse(eEqP, "Person fails `instanceof Employee`");
        assertNotEquals(pEqE, eEqP, "asymmetric — the contract is broken");
    }

    private static void assertNotEquals(boolean a, boolean b, String msg) {
        assertTrue(a != b, msg);
    }

    @Test
    @DisplayName("MUTABLE KEY: mutate a field after put, and get returns null (key moved buckets)")
    void mutableKeyTrap() {
        MutableKeyTrapDemo.MutableAccountKey key = new MutableKeyTrapDemo.MutableAccountKey("BR-01", "1001");
        Map<MutableKeyTrapDemo.MutableAccountKey, Transaction> balances =
                new HashMap<MutableKeyTrapDemo.MutableAccountKey, Transaction>();
        balances.put(key, Transaction.payroll());

        assertNotNull(balances.get(key), "found before mutation");
        int hashBefore = key.hashCode();

        key.setBranchCode("BR-02");   // mutate the SAME object the map holds
        int hashAfter = key.hashCode();

        System.out.println("hashCode before=" + hashBefore + ", after mutation=" + hashAfter
                + "; get(key) now = " + balances.get(key));
        assertNotEquals(hashBefore, hashAfter, "the hashCode changed when the field changed");
        assertNull(balances.get(key), "the entry is filed under the OLD hash; get looks in the NEW bucket");
        assertEquals(1, balances.size(), "the entry still exists, just unreachable");
    }

    private static void assertNotEquals(int a, int b, String msg) {
        assertTrue(a != b, msg);
    }

    @Test
    @DisplayName("COLLISION: unequal keys may share a hash and still both be stored")
    void collisionsAreHandled() {
        String aa = "Aa";
        String bb = "BB";
        assertEquals(aa.hashCode(), bb.hashCode(), "the textbook String collision");
        assertFalse(aa.equals(bb), "...but they are NOT equal");

        Map<String, String> map = new HashMap<String, String>();
        map.put(aa, "first");
        map.put(bb, "second");
        System.out.println("\"Aa\".hashCode()==\"BB\".hashCode()==" + aa.hashCode()
                + "; map size=" + map.size() + ", get(Aa)=" + map.get(aa) + ", get(BB)=" + map.get(bb));
        assertEquals(2, map.size(), "both must survive the collision");
        assertEquals("first", map.get(aa));
        assertEquals("second", map.get(bb));
    }

    @Test
    @DisplayName("compareTo TEASER: HashSet (equals) keeps 2, TreeSet (compareTo) keeps 1")
    void compareToVersusEqualsForSortedSets() {
        BigDecimal twoOh = new BigDecimal("2.0");
        BigDecimal twoOhOh = new BigDecimal("2.00");
        assertFalse(twoOh.equals(twoOhOh), "equals is scale-sensitive");
        assertEquals(0, twoOh.compareTo(twoOhOh), "compareTo says numerically equal");

        Set<BigDecimal> hashSet = new HashSet<BigDecimal>();
        hashSet.add(twoOh);
        hashSet.add(twoOhOh);
        Set<BigDecimal> treeSet = new TreeSet<BigDecimal>();
        treeSet.add(twoOh);
        treeSet.add(twoOhOh);

        System.out.println("HashSet size=" + hashSet.size() + ", TreeSet size=" + treeSet.size());
        assertEquals(2, hashSet.size(), "HashSet uses equals -> both distinct");
        assertEquals(1, treeSet.size(), "TreeSet uses compareTo -> 'loses' one");
    }
}
