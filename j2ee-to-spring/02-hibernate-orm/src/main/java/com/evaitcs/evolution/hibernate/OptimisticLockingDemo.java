package com.evaitcs.evolution.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Demo 5 — OPTIMISTIC LOCKING: @Version turns stage-1's version column into an
 * automatic lost-update detector. (README section "Optimistic locking: @Version".)
 *
 * Module 01's DAO took the PESSIMISTIC route: SELECT ... FOR UPDATE locked both rows, so
 * a concurrent transfer WAITED. Here nobody locks anything. Instead every UPDATE carries
 * WHERE version = <what I read>, and if the row moved in the meantime the WHERE matches
 * zero rows — Hibernate sees the zero count and refuses the stale write with an
 * OptimisticLockException. Second writer FAILS FAST instead of waiting (or worse,
 * silently overwriting).
 *
 * The interleaving below is DETERMINISTIC on purpose (house rule): one thread, two
 * sessions, scripted order — s2's "concurrency" is simulated by simply reading before
 * s1 commits. No sleeps, no races, same output every run.
 *
 * show_sql is ON: watch both UPDATEs — s1's matches (version 0 -> 1), s2's WHERE
 * version = 0 finds nothing.
 */
public class OptimisticLockingDemo {

    public static void main(String[] args) {
        String db = DemoDb.freshDatabase();

        try (SessionFactory factory = BootstrapDemo.newFactory(db);
             Session s1 = factory.openSession();
             Session s2 = factory.openSession()) {

            banner("1. The setup: stage-1's version column, now managed by @Version");
            System.out.println("account 1 (Alice CHECKING) in the seed: balance " + DemoDb.balanceOf(db, 1)
                    + ", version " + versionOf(db, 1) + ".");
            System.out.println("No SELECT ... FOR UPDATE anywhere in this demo — that was module 01's pessimistic way.");

            banner("2. Two sessions read the SAME row — both get version 0");
            Transaction tx1 = s1.beginTransaction();
            Transaction tx2 = s2.beginTransaction();
            Account a1 = s1.find(Account.class, 1L);
            Account a2 = s2.find(Account.class, 1L);
            System.out.println("step 1: s1 sees balance " + a1.getBalance() + ", version " + a1.getVersion() + ".");
            System.out.println("step 2: s2 sees balance " + a2.getBalance() + ", version " + a2.getVersion()
                    + " — the same, soon-to-be-stale snapshot.");

            banner("3. s1 moves first: UPDATE ... SET version = 1 WHERE id = 1 AND version = 0");
            a1.setBalance(new BigDecimal("1600.00"));
            tx1.commit();
            System.out.println("step 1: s1 committed — its UPDATE matched (version was still 0).");
            System.out.println("step 2: database now: balance " + DemoDb.balanceOf(db, 1)
                    + ", version " + versionOf(db, 1) + "; a1.getVersion() = " + a1.getVersion() + ".");

            banner("4. s2 commits on its STALE read — its WHERE matches no row");
            a2.setBalance(new BigDecimal("1700.00"));   // computed from a balance nobody has anymore
            System.out.println("step 1: s2 sets 1700.00 and commits — watch its UPDATE carry WHERE version = 0.");
            try {
                tx2.commit();
                System.out.println("UNEXPECTED: s2 committed — the lost update would have landed.");
            } catch (RuntimeException e) {
                if (tx2.isActive()) {
                    tx2.rollback();
                }
                System.out.println("step 2: CAUGHT -> " + e.getClass().getName());
                System.out.println("        message: " + flatten(e.getMessage()));
                if (e.getCause() != null) {
                    System.out.println("        cause: " + e.getCause().getClass().getName()
                            + " (Hibernate's native name; the JPA wrapper above is what stage-2's");
                    System.out.println("        @Transactional boundary sees — Spring renames it again:");
                    System.out.println("        ObjectOptimisticLockingFailureException).");
                }
                System.out.println("step 3: WHY: the UPDATE affected 0 rows (the row is at version 1, not 0),");
                System.out.println("        so Hibernate refused the write instead of losing s1's update.");
            }

            banner("5. Verdict, and the contrast with module 01");
            System.out.println("database: balance " + DemoDb.balanceOf(db, 1) + ", version " + versionOf(db, 1)
                    + " -> s1's write won; s2's stale write was REJECTED.");
            System.out.println("module 01 (pessimistic): FOR UPDATE made the second transaction WAIT, then proceed.");
            System.out.println("module 02 (optimistic):  no locks held; the second writer FAILS FAST and must retry or give up.");
            System.out.println("Both are valid: pessimistic when conflicts are common and waiting is cheap;");
            System.out.println("optimistic when conflicts are rare and holding row locks across a web request is not.");
        }
    }

    /** Raw-SQL read of the lock column — the truth outside Hibernate's eyes. */
    static long versionOf(String db, long accountId) {
        try (Connection c = DemoDb.connect(db);
             var ps = c.prepareStatement("SELECT version FROM account WHERE id = ?")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("no account " + accountId);
                }
                return rs.getLong("version");
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Hibernate exception messages can span lines; demos print them on one. */
    private static String flatten(String message) {
        return message == null ? "(none)" : message.replaceAll("\\s+", " ");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
