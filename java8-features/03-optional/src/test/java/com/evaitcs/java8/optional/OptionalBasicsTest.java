package com.evaitcs.java8.optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos and README. Run with:
 *   .\mvnw.cmd -pl 03-optional test
 * Each test prints the evidence it asserts, so the test run doubles as a demo.
 */
class OptionalBasicsTest {

    private final AccountRepository repo = new AccountRepository();
    private final LegacyAccountRepository legacy = new LegacyAccountRepository();
    private final List<Customer> customers = Customer.sampleCustomers();

    @Test
    @DisplayName("a miss returns Optional.empty() — not null, not an exception")
    void missingLookupIsEmptyNotNull() {
        Optional<Account> miss = repo.findById("ACC-9999");
        System.out.println("findById(\"ACC-9999\") = " + miss + ", isPresent=" + miss.isPresent());
        assertFalse(miss.isPresent());
        assertNotNull(miss, "the Optional itself is NEVER null — that is the whole point");
        assertNull(legacy.findById("ACC-9999"), "the legacy repo leaks a bare null for the same miss");
    }

    @Test
    @DisplayName("Optional.of(null) throws NPE at the creation site — fail fast")
    void ofRejectsNullImmediately() {
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> Optional.of(customers.get(1).getEmail())); // Bob's email is null
        System.out.println("Optional.of(null) threw " + e.getClass().getSimpleName() + " AT CREATION");
        // ofNullable TOLERATES the very same null — that is when to use which:
        assertFalse(Optional.ofNullable(customers.get(1).getEmail()).isPresent());
        System.out.println("Optional.ofNullable(null) = Optional.empty (no throw)");
    }

    @Test
    @DisplayName("orElse evaluates its argument EAGERLY even when the value is present; orElseGet is lazy")
    void orElseEagerOrElseGetLazy() {
        AtomicInteger fallbackCalls = new AtomicInteger();
        Supplier<BigDecimal> creditBureau = () -> {
            fallbackCalls.incrementAndGet();
            return new BigDecimal("1000.00");
        };
        Optional<BigDecimal> present = Account.seedAccounts().get(0).getCreditLimit(); // 5000.00, PRESENT

        BigDecimal viaOrElse = present.orElse(creditBureau.get()); // .get() runs NOW — the trap
        assertEquals(1, fallbackCalls.get(), "orElse's argument was computed even though the value was present");

        fallbackCalls.set(0);
        BigDecimal viaOrElseGet = present.orElseGet(creditBureau);
        assertEquals(0, fallbackCalls.get(), "orElseGet must NOT invoke the supplier when the value is present");

        assertEquals(viaOrElse, viaOrElseGet, "both routes still return the same answer");
        System.out.println("orElse ran the fallback 1 time; orElseGet ran it " + fallbackCalls.get()
                + " times; both returned " + viaOrElse.toPlainString());
    }

    @Test
    @DisplayName("orElseThrow delivers YOUR exception with YOUR message — unlike a message-less NPE")
    void orElseThrowCarriesMeaning() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> repo.findById("ACC-9999")
                        .orElseThrow(() -> new IllegalStateException("account ACC-9999 does not exist")));
        assertEquals("account ACC-9999 does not exist", e.getMessage());
        System.out.println("orElseThrow(Supplier) -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        // NOTE: the no-arg orElseThrow() does NOT exist in Java 8 — it arrives in Java 10.
        // In strict Java 8 the escape hatch is get(), covered by the next test.
    }

    @Test
    @DisplayName("get() on an empty Optional throws NoSuchElementException — a dressed-up null check")
    void getOnEmptyThrows() {
        NoSuchElementException e = assertThrows(NoSuchElementException.class,
                () -> repo.findById("ACC-9999").get());
        assertTrue(e.getMessage().contains("No value present"));
        System.out.println("empty.get() -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    @Test
    @DisplayName("flatMap flattens Optional<Optional<T>>; map on an Optional-returning function does not")
    void flatMapFlattensNestedOptionals() {
        Optional<Optional<BigDecimal>> nested =
                repo.findPrimaryAccountOf(customers.get(0)).map(Account::getCreditLimit);
        Optional<BigDecimal> flat =
                repo.findPrimaryAccountOf(customers.get(0)).flatMap(Account::getCreditLimit);
        System.out.println("map     -> " + nested + "   (useless double box)");
        System.out.println("flatMap -> " + flat + "   (one box, one answer)");
        assertEquals(new BigDecimal("5000.00"), flat.get());
        // map is still right for PLAIN functions — only Optional-returning mappers need flatMap:
        assertEquals("Alice Nguyen", repo.findById("ACC-1001").map(Account::getOwner).orElse("?"));
    }

    @Test
    @DisplayName("a chain short-circuits at the FIRST empty link — Dana (no account), Bob (no limit)")
    void chainShortCircuits() {
        for (Customer c : customers) {
            String decision = repo.findPrimaryAccountOf(c)
                    .flatMap(Account::getCreditLimit)
                    .filter(l -> l.compareTo(new BigDecimal("1000.00")) >= 0)
                    .map(l -> "approved " + l.toPlainString())
                    .orElse("no usable credit line");
            System.out.println("  " + c.getName() + " -> " + decision);
            if (c.getName().startsWith("Alice")) {
                assertEquals("approved 5000.00", decision);
            } else {
                assertEquals("no usable credit line", decision,
                        "Bob has no limit, Dana has no account — both must fall to orElse");
            }
        }
    }

    @Test
    @DisplayName("filter turns a PRESENT value that fails the predicate into empty")
    void filterDropsFailingValue() {
        Optional<BigDecimal> aliceLimit = Account.seedAccounts().get(0).getCreditLimit(); // 5000.00
        assertTrue(aliceLimit.isPresent());
        Optional<BigDecimal> rejected = aliceLimit.filter(l -> l.compareTo(new BigDecimal("10000.00")) >= 0);
        assertFalse(rejected.isPresent());
        System.out.println("5000.00.filter(>= 10000.00) -> " + rejected + " (present but rejected -> empty)");
    }

    @Test
    @DisplayName("before/after callers get the SAME answer for the same miss and the same hit")
    void beforeAfterAgree() {
        // miss: the pre-Java-8 if-wall vs the Optional chain
        Account raw = legacy.findById("ACC-9999");
        BigDecimal before = BigDecimal.ZERO;
        if (raw != null) {
            before = raw.getBalance();
        }
        BigDecimal after = repo.findById("ACC-9999").map(Account::getBalance).orElse(BigDecimal.ZERO);
        assertEquals(before, after);
        System.out.println("miss: if-wall = " + before + ", Optional chain = " + after);

        // hit: both worlds agree there too
        BigDecimal beforeHit = legacy.findById("ACC-1001").getBalance();
        BigDecimal afterHit = repo.findById("ACC-1001").map(Account::getBalance).orElse(BigDecimal.ZERO);
        assertEquals(beforeHit, afterHit);
        System.out.println("hit : if-wall = " + beforeHit + ", Optional chain = " + afterHit);
    }

    @Test
    @DisplayName("rule #3: collection lookups return an empty LIST — never Optional<List<T>>, never null")
    void collectionsAreNeverOptional() {
        List<String> miss = repo.recentStatementsFor("ACC-9999");
        assertNotNull(miss, "an empty list, not null");
        assertTrue(miss.isEmpty());
        int looped = 0;
        for (String ignored : miss) {
            looped++;
        }
        assertEquals(0, looped, "the caller loops over an empty list with zero unwrapping");
        assertEquals(2, repo.recentStatementsFor("ACC-1001").size(), "the hit case is a plain list too");
        System.out.println("recentStatementsFor(\"ACC-9999\") = " + miss + " — looped " + looped
                + " times with no null check anywhere");
    }
}
