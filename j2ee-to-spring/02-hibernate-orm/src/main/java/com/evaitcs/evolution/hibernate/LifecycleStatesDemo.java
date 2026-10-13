package com.evaitcs.evolution.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Demo 3 — THE STATE MACHINE: transient -> persistent -> detached -> removed, and merge()
 * to cross back. (README section "Entity lifecycle: the four states" and its state diagram.)
 *
 * This is the price of the ORM bargain from demo 2. A managed entity is not a bag of data;
 * it is an object in ONE OF FOUR STATES, and what a setter DOES depends entirely on which
 * state it is in. The loudest trap in all of Hibernate lives here: mutate a DETACHED
 * entity and the change is silently, permanently lost — no exception, no UPDATE, nothing.
 * We prove that with raw SQL, then undo it with merge() (which has its own classic gotcha:
 * it returns the managed COPY, it does not re-manage the argument you passed).
 *
 * show_sql is ON so you see the INSERT at persist, the UPDATE at merge-commit, and the
 * DELETE at remove-commit.
 */
public class LifecycleStatesDemo {

    public static void main(String[] args) {
        String db = DemoDb.freshDatabase();

        try (SessionFactory factory = BootstrapDemo.newFactory(db)) {

            banner("1. TRANSIENT — a plain object no session has ever heard of");
            Account scratch = new Account();
            scratch.setType(Account.AccountType.SAVINGS);
            scratch.setBalance(new BigDecimal("100.00"));
            System.out.println("step 1: new Account(), set type + balance.");
            System.out.println("step 2: id = " + scratch.getId() + " -> null identity, no snapshot, not in any session.");
            System.out.println("verdict: transient. Setting fields now is just Java; nothing will reach the DB.");

            banner("2. PERSISTENT — persist() inside a transaction; INSERT at flush");
            Long newId;
            try (Session session = factory.openSession()) {
                var tx = session.beginTransaction();
                Customer bob = session.find(Customer.class, 2L);   // account.customer_id is NOT NULL
                scratch.setCustomer(bob);
                System.out.println("step 1: attach a customer, then session.persist(scratch).");
                session.persist(scratch);
                System.out.println("step 2: with GenerationType.IDENTITY, the INSERT runs NOW (Hibernate needs");
                System.out.println("        the DB-generated key back) — you saw it above. id = " + scratch.getId());
                tx.commit();   // flush; the row is committed
                newId = scratch.getId();
            }
            System.out.println("step 3: verdict — DemoDb.balanceOf(db, " + newId + ") = " + DemoDb.balanceOf(db, newId)
                    + " -> the INSERT landed.");
            System.out.println("        While that session was open, scratch was PERSISTENT (managed).");

            banner("3. DETACHED — the session closed; mutations go NOWHERE");
            // The try-with-resources above closed the session. scratch is now DETACHED:
            // it still holds its id and field values, but no session tracks it anymore.
            scratch.setBalance(new BigDecimal("999.99"));
            System.out.println("step 1: session is closed, so scratch is detached.");
            System.out.println("step 2: scratch.setBalance(999.99) — a perfectly legal Java call...");
            System.out.println("step 3: DemoDb.balanceOf(db, " + newId + ") = " + DemoDb.balanceOf(db, newId)
                    + "  (STILL 100.00).");
            System.out.println("verdict: the change was SILENTLY LOST. No exception, no UPDATE. This is THE trap.");

            banner("4. merge() — reattach a detached instance (and its gotcha)");
            try (Session session = factory.openSession()) {
                var tx = session.beginTransaction();
                Account managed = session.merge(scratch);   // copies scratch's state onto a managed instance
                System.out.println("step 1: Account managed = session.merge(scratch).");
                System.out.println("step 2: managed == scratch ? " + (managed == scratch)
                        + "  -> FALSE. merge returns the MANAGED copy; scratch stays detached.");
                System.out.println("step 3: managed.getBalance() = " + managed.getBalance()
                        + " (scratch's 999.99 was copied in); commit flushes the UPDATE.");
                tx.commit();
            }
            System.out.println("step 4: verdict — DemoDb.balanceOf(db, " + newId + ") = " + DemoDb.balanceOf(db, newId)
                    + " -> merge carried the detached change across.");
            System.out.println("        Gotcha to photograph: always use the RETURN of merge(), never the argument.");

            banner("5. REMOVED — remove(); DELETE at flush");
            try (Session session = factory.openSession()) {
                var tx = session.beginTransaction();
                Account victim = session.find(Account.class, newId);   // persistent again
                session.remove(victim);                                 // -> removed; DELETE scheduled
                tx.commit();                                            // flush issues the DELETE
            }
            System.out.println("step 1: find it (persistent), remove() it (-> removed), commit (DELETE above).");
            System.out.println("step 2: verdict — accountExists(db, " + newId + ") = " + accountExists(db, newId)
                    + " -> the row is gone.");

            banner("6. The four states on one page (the README state diagram)");
            System.out.println("transient  : new, no id, no session.      persist() -> persistent.");
            System.out.println("persistent : managed by an OPEN session;  setters are tracked; flush writes them.");
            System.out.println("detached   : session closed; setters are  SILENTLY IGNORED. merge() -> persistent copy.");
            System.out.println("removed    : remove() called; still has an id; flush issues the DELETE.");
        }
    }

    /** Raw-SQL existence check: proves the DELETE landed without going back through Hibernate. */
    static boolean accountExists(String db, long id) {
        try (Connection c = DemoDb.connect(db);
             var ps = c.prepareStatement("SELECT COUNT(*) FROM account WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
