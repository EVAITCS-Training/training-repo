package com.evaitcs.evolution.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.math.BigDecimal;

/**
 * Demo 2 — THE ORM BARGAIN: change an object inside a transaction, commit, and Hibernate
 * writes the UPDATE. You wrote no SQL. (README section "The ORM bargain: dirty checking".)
 *
 * This is the exact job module 01's DAO did by hand — ps.setBigDecimal(...); ps.executeUpdate()
 * — and it is the reason ORM won. The mechanism is DIRTY CHECKING: when a session flushes,
 * Hibernate compares each managed entity's current fields against the SNAPSHOT it took at
 * load, and emits an UPDATE only for the columns that changed. show_sql is ON here so you
 * watch that UPDATE appear with no code from us.
 *
 * Era note: stage-2 does this same thing behind a Spring Data repository save(); the flush
 * happens at the @Transactional commit boundary. Here we drive the transaction by hand so
 * the boundary is visible.
 */
public class DirtyCheckingDemo {

    public static void main(String[] args) {
        String db = DemoDb.freshDatabase();

        // show_sql ON: the emitted SELECT and the automatic UPDATE ARE the lesson.
        try (SessionFactory factory = BootstrapDemo.newFactory(db)) {

            banner("1. The contract: find() returns a MANAGED entity, not a dead copy");
            System.out.println("step 1: open a session + transaction (a unit of work).");
            System.out.println("step 2: find account 3 (Bob's CHECKING, seeded at 300.00).");
            System.out.println("step 3: the SELECT above is the LAST SQL we ask for by name.");
            try (Session session = factory.openSession()) {
                var tx = session.beginTransaction();
                Account bob = session.find(Account.class, 3L);
                System.out.println("        loaded: " + bob + ", version " + bob.getVersion());

                banner("2. Dirty checking: set the balance, commit, WATCH the UPDATE appear");
                System.out.println("step 1: bob.setBalance(350.00) — one Java call, NO SQL yet.");
                bob.setBalance(new BigDecimal("350.00"));
                System.out.println("step 2: commit() triggers a FLUSH — Hibernate diffs the entity");
                System.out.println("        against its load-time snapshot, sees balance changed, and emits:");
                System.out.println("        (the UPDATE below is written by HIBERNATE, not by us)");
                tx.commit();   // <- the UPDATE ... SET balance=?, version=? WHERE id=? AND version=? fires here
                System.out.println("step 3: verdict — we changed a field; the database changed to match.");
            }

            banner("3. Proof it really landed: read the row back through raw SQL");
            BigDecimal fromSql = DemoDb.balanceOf(db, 3);
            System.out.println("DemoDb.balanceOf(db, 3) = " + fromSql + "  (seeded 300.00, set to 350.00)");
            System.out.println("verdict: " + fromSql.toPlainString() + " -> dirty checking persisted the change.");

            banner("4. When does a flush happen? (README 'The ORM bargain' walkthrough)");
            System.out.println("Hibernate synchronizes the persistence context to the database at three moments:");
            System.out.println("  (a) when the transaction COMMITS — what you just saw;");
            System.out.println("  (b) BEFORE a query runs — so your query sees your own pending writes;");
            System.out.println("  (c) when you call session.flush() EXPLICITLY — rarely needed, occasionally vital.");
            System.out.println("Until one of those, setBalance lives only in memory. That gap is the whole model.");

            banner("5. What module 01 needed vs what this needed");
            System.out.println("module 01, to move one balance: prepareStatement(\"UPDATE account SET balance=?...\"),");
            System.out.println("  bind every column by index, executeUpdate, and remember the version bump yourself.");
            System.out.println("module 02: bob.setBalance(...); tx.commit();  — Hibernate wrote the UPDATE and the version.");
            System.out.println("verdict: THAT is the ORM bargain. The price is demos 3-5: the state machine.");
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
