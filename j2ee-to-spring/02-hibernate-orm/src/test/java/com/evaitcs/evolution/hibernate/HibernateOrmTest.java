package com.evaitcs.evolution.hibernate;

import jakarta.persistence.OptimisticLockException;
import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF over a REAL Hibernate SessionFactory on in-memory H2: no mocks, every
 * claim backed by a query or by raw SQL through DemoDb. Run with:
 *   .\mvnw.cmd -pl 02-hibernate-orm test
 *
 * Determinism notes (house rules):
 *   - every test method gets its OWN fresh database AND fresh SessionFactory
 *     (@BeforeEach) — Hibernate bootstrap on H2 is fast, so this is cheap and total;
 *   - the factory is closed in @AfterEach — the JVM exits clean, no lingering pools;
 *   - the concurrency tests (optimistic locking) are SINGLE-THREADED and scripted: two
 *     sessions interleaved by hand, no sleeps, no races, identical every run;
 *   - the N+1 test asserts EXACT statement counts via the StatementInspector, so the
 *     lesson is a number, not a vibe;
 *   - show_sql is OFF here (the inspector counts instead); the demos turn it ON.
 */
class HibernateOrmTest {

    private String db;
    private SessionFactory factory;

    @BeforeEach
    void freshWorld() {
        db = DemoDb.freshDatabase();                          // brand-new schema + seed
        factory = BootstrapDemo.newQuietFactory(db);          // show_sql off, one line
    }

    @AfterEach
    void closeFactory() {
        factory.close();                                      // releases the registry + pool
    }

    @Test
    @DisplayName("find() returns the seeded account as a managed entity with the exact scale-2 balance")
    void findReturnsSeededAccount() {
        try (Session s = factory.openSession()) {
            var tx = s.beginTransaction();
            Account a = s.find(Account.class, 3L);            // Bob's CHECKING
            tx.commit();

            assertNotNull(a, "account 3 exists in the seed");
            assertEquals(DemoDb.BOB_CHECKING, a.getBalance(), "300.00, byte-for-byte");
            assertEquals(2, a.getBalance().scale(), "money keeps scale 2 off a DECIMAL(15,2) column");
            assertEquals(Account.AccountType.CHECKING, a.getType());
            System.out.println("find(Account,3) -> " + a + "; balance scale = " + a.getBalance().scale());
        }
    }

    @Test
    @DisplayName("dirty checking: setBalance inside a transaction persists at commit with no SQL from us")
    void dirtyCheckingPersistsAfterCommit() {
        try (Session s = factory.openSession()) {
            var tx = s.beginTransaction();
            Account a = s.find(Account.class, 3L);
            a.setBalance(new BigDecimal("350.00"));           // one Java call; Hibernate writes the UPDATE
            tx.commit();
        }

        // Proof 1: raw SQL, outside Hibernate, sees the new balance.
        assertEquals(new BigDecimal("350.00"), DemoDb.balanceOf(db, 3));

        // Proof 2: a SECOND factory (a fresh persistence context) reads the same 350.00.
        try (SessionFactory second = BootstrapDemo.newQuietFactory(db);
             Session s2 = second.openSession()) {
            var tx2 = s2.beginTransaction();
            Account reloaded = s2.find(Account.class, 3L);
            tx2.commit();
            assertEquals(new BigDecimal("350.00"), reloaded.getBalance(), "the change outlived its session");
            System.out.println("dirty checking: 300.00 -> 350.00; second factory reads " + reloaded.getBalance());
        }
    }

    @Test
    @DisplayName("a detached entity's mutation is silently lost, but merge() carries it to the database")
    void detachedMutationLostMergePersists() {
        Account detached;
        Long id;
        try (Session s = factory.openSession()) {
            var tx = s.beginTransaction();
            Customer bob = s.find(Customer.class, 2L);
            detached = new Account();
            detached.setCustomer(bob);
            detached.setType(Account.AccountType.SAVINGS);
            detached.setBalance(new BigDecimal("100.00"));
            s.persist(detached);
            tx.commit();
            id = detached.getId();
        }
        // The session is closed now, so `detached` is DETACHED.
        assertEquals(new BigDecimal("100.00"), DemoDb.balanceOf(db, id));

        detached.setBalance(new BigDecimal("999.99"));        // legal Java, but no session tracks it
        assertEquals(new BigDecimal("100.00"), DemoDb.balanceOf(db, id),
                "the detached mutation must NOT reach the database");

        try (Session s = factory.openSession()) {
            var tx = s.beginTransaction();
            Account managed = s.merge(detached);              // copy onto a managed instance
            assertNotSame(detached, managed, "merge returns the managed COPY, not the argument");
            tx.commit();
        }
        assertEquals(new BigDecimal("999.99"), DemoDb.balanceOf(db, id), "merge + commit carried it across");
        System.out.println("detached setBalance(999.99) lost (db stayed 100.00); after merge db = "
                + DemoDb.balanceOf(db, id));
    }

    @Test
    @DisplayName("persist() assigns the identity id and INSERTs the row, provable through raw SQL")
    void persistAssignsIdentityAndInserts() {
        Long id;
        try (Session s = factory.openSession()) {
            var tx = s.beginTransaction();
            Customer carol = s.find(Customer.class, 3L);      // Carol has no accounts in the seed
            Account a = new Account();
            a.setCustomer(carol);
            a.setType(Account.AccountType.CHECKING);
            a.setBalance(new BigDecimal("77.00"));

            assertNull(a.getId(), "transient: no identity yet");
            s.persist(a);
            assertNotNull(a.getId(), "IDENTITY generation assigns the id at persist");
            id = a.getId();
            tx.commit();
        }
        assertEquals(new BigDecimal("77.00"), DemoDb.balanceOf(db, id), "the INSERT really landed");
        System.out.println("persist assigned id " + id + "; DemoDb.balanceOf reads " + DemoDb.balanceOf(db, id));
    }

    @Test
    @DisplayName("touching a lazy collection after the session closes throws LazyInitializationException")
    void lazyCollectionThrowsAfterClose() {
        Customer alice;
        try (Session s = factory.openSession()) {
            var tx = s.beginTransaction();
            alice = s.find(Customer.class, 1L);               // do NOT touch accounts
            tx.commit();
        }
        // Session closed; alice.getAccounts() is still an uninitialized proxy-backed bag.
        LazyInitializationException e = assertThrows(LazyInitializationException.class,
                () -> alice.getAccounts().size());
        System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    @Test
    @DisplayName("N+1 is exact: the naive loop costs 1+3 statements, a join fetch costs 1")
    void nPlusOneStatementCountsAreExact() {
        StatementCounter counter = new StatementCounter();
        try (SessionFactory counting = BootstrapDemo.newQuietFactory(db, counter)) {

            // Naive: one SELECT for the customers, then one per accounts collection.
            counter.reset();
            try (Session s = counting.openSession()) {
                var tx = s.beginTransaction();
                List<Customer> customers = s.createQuery("from Customer", Customer.class).list();
                int totalAccounts = 0;
                for (Customer c : customers) {
                    totalAccounts += c.getAccounts().size();   // forces each lazy load
                }
                tx.commit();
                assertEquals(3, customers.size(), "Alice, Bob, Carol");
                assertEquals(3, totalAccounts, "2 + 1 + 0 across the three customers");
            }
            assertEquals(4, counter.count(), "N+1: 1 (customers) + 3 (one collection load each)");
            System.out.println("naive loop -> " + counter.count() + " statements (1 + 3)");

            // Fixed: one JPQL join fetch loads every collection in a single trip.
            counter.reset();
            try (Session s = counting.openSession()) {
                var tx = s.beginTransaction();
                List<Customer> customers =
                        s.createQuery("select c from Customer c left join fetch c.accounts", Customer.class).list();
                int totalAccounts = 0;
                for (Customer c : customers) {
                    totalAccounts += c.getAccounts().size();   // already initialized: no extra SELECT
                }
                tx.commit();
                assertEquals(3, customers.size(), "LEFT join keeps Carol, who has zero accounts");
                assertEquals(3, totalAccounts);
            }
            assertEquals(1, counter.count(), "join fetch: exactly one statement");
            System.out.println("join fetch -> " + counter.count() + " statement (the fix)");
        }
    }

    @Test
    @DisplayName("optimistic locking: two sessions read one row, and the second committer throws OptimisticLockException")
    void secondCommitterHitsOptimisticLock() {
        try (Session s1 = factory.openSession();
             Session s2 = factory.openSession()) {

            var tx1 = s1.beginTransaction();
            var tx2 = s2.beginTransaction();
            Account a1 = s1.find(Account.class, 1L);           // both read version 0
            Account a2 = s2.find(Account.class, 1L);
            assertEquals(0L, a1.getVersion());
            assertEquals(0L, a2.getVersion());

            a1.setBalance(new BigDecimal("1600.00"));
            tx1.commit();                                      // version 0 -> 1 in the database
            assertEquals(1L, a1.getVersion(), "s1's flush bumped the in-memory version too");

            a2.setBalance(new BigDecimal("1700.00"));           // computed from a now-stale snapshot
            OptimisticLockException e = assertThrows(OptimisticLockException.class, tx2::commit,
                    "s2's UPDATE ... WHERE version = 0 matches zero rows");
            if (tx2.isActive()) {
                tx2.rollback();
            }

            assertEquals(new BigDecimal("1600.00"), DemoDb.balanceOf(db, 1),
                    "s1's write won; s2's stale write was refused, not silently applied");
            System.out.println("CAUGHT -> " + e.getClass().getName() + "; db balance = " + DemoDb.balanceOf(db, 1));
        }
    }

    @Test
    @DisplayName("@Version increments by one when a managed account is updated and committed")
    void versionIncrementsAfterUpdate() {
        try (Session s = factory.openSession()) {
            var tx = s.beginTransaction();
            Account a = s.find(Account.class, 2L);
            assertEquals(0L, a.getVersion(), "seeded at version 0");
            a.setBalance(new BigDecimal("5300.00"));
            tx.commit();
            assertEquals(1L, a.getVersion(), "the flush wrote version + 1 and updated the entity");
        }
        // A fresh session reads the committed version straight from the row.
        try (Session s = factory.openSession()) {
            var tx = s.beginTransaction();
            Account reloaded = s.find(Account.class, 2L);
            tx.commit();
            assertEquals(1L, reloaded.getVersion(), "the database column now holds 1");
            assertEquals(new BigDecimal("5300.00"), reloaded.getBalance());
            System.out.println("version 0 -> 1 after update; reloaded " + reloaded);
        }
    }

    @Test
    @DisplayName("JPQL returns the accounts ordered by balance descending, as the exact id sequence")
    void jpqlOrderedResultExactIds() {
        try (Session s = factory.openSession()) {
            var tx = s.beginTransaction();
            List<Long> ids = s
                    .createQuery("from Account a where a.balance > :min order by a.balance desc", Account.class)
                    .setParameter("min", new BigDecimal("0.00"))
                    .list().stream().map(Account::getId).toList();
            tx.commit();
            assertEquals(List.of(2L, 1L, 3L), ids, "5200.00 (acct 2), 1500.00 (acct 1), 300.00 (acct 3)");
            System.out.println("JPQL order by balance desc -> ids " + ids);
        }
    }

    @Test
    @DisplayName("a JPQL constructor expression projects straight into a Java record with exact fields")
    void constructorExpressionProjectsToRecord() {
        try (Session s = factory.openSession()) {
            var tx = s.beginTransaction();
            List<AccountSummary> rows = s.createQuery(
                            "select new com.evaitcs.evolution.hibernate.AccountSummary(a.id, a.balance) "
                                    + "from Account a where a.id = :id", AccountSummary.class)
                    .setParameter("id", 1L)
                    .list();
            tx.commit();

            assertEquals(1, rows.size());
            AccountSummary row = rows.getFirst();
            assertEquals(Long.valueOf(1L), row.id(), "record component id");
            assertEquals(DemoDb.ALICE_CHECKING, row.balance(), "record component balance, scale 2");
            System.out.println("constructor expression -> " + row);
        }
    }

    @Test
    @DisplayName("a native SQL query still returns a MANAGED entity that the session contains")
    void nativeQueryReturnsManagedEntity() {
        try (Session s = factory.openSession()) {
            var tx = s.beginTransaction();
            Account a = s.createNativeQuery("select * from account where id = ?", Account.class)
                    .setParameter(1, 1L)
                    .uniqueResult();
            assertTrue(s.contains(a), "the native result is in the persistence context, not a dead DTO");
            assertEquals(DemoDb.ALICE_CHECKING, a.getBalance());
            tx.commit();
            System.out.println("native SQL -> " + a + "; session.contains = " + s.contains(a));
        }
    }
}
