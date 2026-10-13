package com.evaitcs.evolution.hibernate;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Demo 6 — THE QUERY TOUR: four ways to ask the same database, one mapping.
 * (README section "Queries: four ways to ask".)
 *
 *   banner 1: JPQL    — the query speaks ENTITIES and FIELDS ("from Account a where a.balance"),
 *                       not tables and columns; Hibernate generates the SQL per dialect.
 *   banner 2: Criteria — the same JPQL built as typed OBJECTS instead of a string
 *                       (the era's answer to "strings are not checked until runtime").
 *   banner 3: Native SQL — the escape hatch, still returning MANAGED entities
 *                       (the classic API called this addEntity; today the entity class
 *                       is just the second argument of createNativeQuery).
 *   banner 4: Bound parameters — advanced-java/12's injection lesson, ORM edition:
 *                       setParameter is a bind slot; string concatenation is an open door.
 *   banner 5: Constructor expression — JPQL projecting straight into a Java 21 RECORD,
 *                       destructured with a record pattern. Pre-records this needed a
 *                       hand-written DTO class with getters.
 *
 * Everything runs INSIDE one session + transaction so the lazy owner proxies in banner 1
 * resolve legally (remember demo 2's deadline).
 */
public class QueryTourDemo {

    public static void main(String[] args) {
        String db = DemoDb.freshDatabase();

        try (SessionFactory factory = BootstrapDemo.newFactory(db);
             Session session = factory.openSession()) {

            var tx = session.beginTransaction();

            banner("1. JPQL — the query speaks entities, not tables");
            List<Account> rich = session
                    .createQuery("from Account a where a.balance > :min order by a.balance desc", Account.class)
                    .setParameter("min", new BigDecimal("100.00"))
                    .list();
            for (Account a : rich) {
                // a.getCustomer() is a lazy proxy; getName() resolves it HERE, inside the
                // session — legal. (Alice is fetched once: same session = same proxy instance.)
                System.out.println("   id=" + a.getId() + "  balance=" + a.getBalance()
                        + "  owner=" + a.getCustomer().getName());
            }
            System.out.println("verdict: expected 5200.00, 1500.00, 300.00 descending — no SQL was hand-written.");

            banner("2. Criteria — the same question, built as objects");
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Account> cq = cb.createQuery(Account.class);
            Root<Account> root = cq.from(Account.class);
            cq.select(root)
                    .where(cb.greaterThan(root.<BigDecimal>get("balance"), new BigDecimal("1000.00")))
                    .orderBy(cb.desc(root.get("balance")));
            List<Account> big = session.createQuery(cq).list();
            for (Account a : big) {
                System.out.println("   id=" + a.getId() + "  balance=" + a.getBalance());
            }
            System.out.println("verdict: same result as JPQL with :min = 1000.00 — verbose, but typed and composable.");

            banner("3. Native SQL — the escape hatch, STILL returning managed entities");
            Account nativeHit = session
                    .createNativeQuery("select * from account where id = ?", Account.class)  // classic name: addEntity
                    .setParameter(1, 1L)
                    .uniqueResult();
            System.out.println("   row -> " + nativeHit);
            System.out.println("   managed by this session? " + session.contains(nativeHit)
                    + " -> native SQL still lands in the persistence context (dirty checking applies).");

            banner("4. Bound parameters are DATA — the injection lesson survives the ORM");
            long matches = session
                    .createQuery("from Customer c where c.email = :email", Customer.class)
                    .setParameter("email", "alice@securebank.dev' OR '1'='1")
                    .list().size();
            System.out.println("   email = \"alice@securebank.dev' OR '1'='1\" matched " + matches + " customers.");
            System.out.println("   verdict: the payload was just a strange string — setParameter, NEVER concatenation");
            System.out.println("            (advanced-java/12 built the attack; module 01's DAO blocked it the same way).");

            banner("5. Constructor expression — JPQL straight into a Java 21 record");
            List<AccountSummary> rows = session
                    .createQuery("""
                            select new com.evaitcs.evolution.hibernate.AccountSummary(a.id, a.balance) \
                            from Account a order by a.id""", AccountSummary.class)
                    .list();
            rows.forEach(row -> System.out.println("   " + row));
            // Java 21 record pattern: destructure the first row without calling accessors.
            if (rows.getFirst() instanceof AccountSummary(Long id, BigDecimal balance)) {
                System.out.println("   record pattern read: account " + id + " holds " + balance);
            }
            System.out.println("verdict: projections need no entity, no session afterwards — records were made for this.");

            tx.commit();

            System.out.println();
            System.out.println("Tour verdict: JPQL for the object graph, Criteria for composition, native SQL for the");
            System.out.println("occasional hand-tuned statement — and constructor expressions when you want DATA, not entities.");
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
