package com.evaitcs.evolution.hibernate;

import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

/**
 * Demo 4 — LAZY IS A PROMISE, N+1 IS BREAKING IT ONE TRIP AT A TIME.
 * (README section "Lazy loading and N+1" and its two-flow diagram.)
 *
 * Customer.getAccounts() is declared fetch = LAZY, so Hibernate hands back a proxy-backed
 * collection — a PLACEHOLDER that owes you a SELECT the first time code touches it. Two
 * lessons live in that design:
 *   1. the promise has a DEADLINE (the session that owns it): touch it after close and you
 *      get LazyInitializationException, the most famous error in Hibernate — and the root
 *      of stage-2's spring.jpa.open-in-view debates;
 *   2. one promise per row means one TRIP per row: 3 customers = 1 + 3 SELECTs (the N+1).
 *      A JPQL "join fetch" keeps all the promises in ONE trip.
 *
 * Both factories here carry a StatementCounter (the show_sql lines let you SEE each trip;
 * the counter lets you COUNT it — the same numbers the test asserts exactly).
 */
public class LazyAndNPlusOneDemo {

    public static void main(String[] args) {
        String db = DemoDb.freshDatabase();
        StatementCounter counter = new StatementCounter();

        try (SessionFactory factory = BootstrapDemo.newFactory(db, counter)) {

            banner("1. Lazy is a PROMISE: getAccounts() returns a placeholder, not rows");
            try (Session session = factory.openSession()) {
                var tx = session.beginTransaction();
                Customer bob = session.find(Customer.class, 2L);
                System.out.println("step 1: find(Customer, 2) printed ONE select — the customer row only.");
                List<Account> accounts = bob.getAccounts();
                System.out.println("step 2: getAccounts() BEFORE touching it -> " + accounts.getClass().getSimpleName()
                        + " (a PersistentBag placeholder). Statements so far: " + counter.count() + ".");
                System.out.println("step 3: .size() is 'touching it' — the promised SELECT fires now:");
                int n = accounts.size();
                System.out.println("        Bob has " + n + " account(s). Statements so far: " + counter.count() + ".");
                System.out.println("verdict: LAZY deferred the trip until the value was actually needed.");
                tx.commit();
            }

            banner("2. The promise has a DEADLINE: the session that owns it");
            Customer alice;
            try (Session session = factory.openSession()) {
                var tx = session.beginTransaction();
                alice = session.find(Customer.class, 1L);   // deliberately DO NOT touch accounts
                tx.commit();
            }   // session closed: alice is detached, her accounts bag still uninitialized
            System.out.println("step 1: loaded Alice, closed the session WITHOUT touching accounts.");
            try {
                alice.getAccounts().size();
                System.out.println("UNEXPECTED: no exception — the lesson would be broken.");
            } catch (LazyInitializationException e) {
                System.out.println("step 2: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.out.println("step 3: WHY: the session that could have kept the promise is closed, and a");
                System.out.println("        detached object has no connection to load through.");
                System.out.println("        This is THE most famous Hibernate error — and the root of stage-2's");
                System.out.println("        spring.jpa.open-in-view debate: keep sessions open into the view, or don't.");
            }

            banner("3. N+1: three customers, one session, the naive loop");
            counter.reset();
            try (Session session = factory.openSession()) {
                var tx = session.beginTransaction();
                List<Customer> customers = session.createQuery("from Customer", Customer.class).list();
                System.out.println("step 1: 'from Customer' — 1 select. Statements: " + counter.count() + ".");
                for (Customer c : customers) {
                    System.out.println("step 2: " + c.getName() + " -> " + c.getAccounts().size()
                            + " account(s). Statements: " + counter.count() + ".");
                }
                System.out.println("        (Carol's empty collection costs a SELECT too — the promise is per row.)");
                tx.commit();
            }
            System.out.println("verdict: " + counter.count() + " statements = 1 + 3. With 1000 customers it is 1 + 1000 —");
            System.out.println("         the N+1 SELECT problem, hidden behind innocent-looking getters.");

            banner("4. The fix: JOIN FETCH — keep every promise in ONE trip");
            counter.reset();
            try (Session session = factory.openSession()) {
                var tx = session.beginTransaction();
                List<Customer> customers = session.createQuery(
                        "select c from Customer c left join fetch c.accounts", Customer.class).list();
                System.out.println("step 1: one JPQL query with 'left join fetch' — statements: " + counter.count() + ".");
                int total = 0;
                for (Customer c : customers) {
                    total += c.getAccounts().size();   // collections arrived pre-initialized: NO extra SELECT
                }
                System.out.println("step 2: " + customers.size() + " customers, " + total
                        + " accounts total — touching every collection added nothing.");
                System.out.println("        Note LEFT join fetch: an inner join fetch would silently DROP Carol (zero accounts).");
                tx.commit();
            }
            System.out.println("verdict: " + counter.count() + " statement instead of 4 — lazy is a promise, fetch join is keeping it in one trip.");
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
