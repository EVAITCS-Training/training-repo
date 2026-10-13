package com.evaitcs.advanced.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

/**
 * Demo 4 — TRANSACTIONS: auto-commit, the atomic bank transfer, and savepoints.
 * (README section "Transactions and auto-commit"; ties to stage-1
 * queries/04-transactions-locking.sql and stage-2's @Transactional.)
 *
 * The claims:
 *   1. autoCommit=true (the default) commits EVERY statement alone — a two-statement
 *      transfer is two independent bets, and a crash between them loses money
 *   2. setAutoCommit(false) + commit()/rollback() makes the debit and the credit
 *      ONE atomic unit: all of it, or none of it
 *   3. uncommitted changes are invisible to OTHER connections (READ COMMITTED)
 *   4. a Savepoint rolls back PART of a transaction without losing the rest
 *
 * NON-DESTRUCTIVE, LOUDLY: the transfer runs against the REAL seed balances —
 * so the transaction ends in ROLLBACK, every time, unconditionally (finally).
 * The demo prints the inside-transaction view AND the after-rollback view: the
 * seed data proves atomicity precisely BECAUSE it is unchanged at the end.
 */
public class TransactionDemo {

    private static final BigDecimal AMOUNT = new BigDecimal("100.00");

    public static void main(String[] args) throws SQLException {
        if (!Db.isAvailable()) {
            Db.printStartInstructions();
            return;
        }

        banner("1. The default: autoCommit=true — every statement commits ALONE");
        try (Connection c = Db.connect()) {
            System.out.println("step 1: c.getAutoCommit() = " + c.getAutoCommit());
            System.out.println("step 2: in this mode each executeUpdate() is its own transaction:");
            System.out.println("        debit commits, THEN credit commits — a crash in between means");
            System.out.println("        the money left one account and never arrived in the other.");
            System.out.println("verdict: for a transfer you must group the two statements yourself.");
        }

        banner("2. The atomic transfer — debit + credit in ONE transaction, then ROLLBACK");
        try (Connection c = Db.connect()) {
            long checkingId = accountId(c, "alice@securebank.dev", "CHECKING");
            long savingsId  = accountId(c, "alice@securebank.dev", "SAVINGS");
            System.out.println("step 1: found Alice's accounts — CHECKING id=" + checkingId + ", SAVINGS id=" + savingsId);
            System.out.println("step 2: seed balances        " + balances(c));

            c.setAutoCommit(false);   // from here on, NOTHING is permanent until commit()
            try {
                try (PreparedStatement debit = c.prepareStatement(
                        "UPDATE account SET balance = balance - ?, version = version + 1 WHERE id = ?");
                     PreparedStatement credit = c.prepareStatement(
                        "UPDATE account SET balance = balance + ?, version = version + 1 WHERE id = ?")) {
                    debit.setBigDecimal(1, AMOUNT);
                    debit.setLong(2, checkingId);
                    System.out.println("step 3: debit  " + AMOUNT + " -> " + debit.executeUpdate() + " row");
                    credit.setBigDecimal(1, AMOUNT);
                    credit.setLong(2, savingsId);
                    System.out.println("step 4: credit " + AMOUNT + " -> " + credit.executeUpdate() + " row");
                }
                // Same connection, open transaction: our own uncommitted changes ARE visible to us.
                System.out.println("step 5: INSIDE the txn (this connection):  " + balances(c));

                // A SECOND connection sees the committed world only (isolation level
                // READ COMMITTED, PostgreSQL's default): the transfer has not happened yet.
                try (Connection other = Db.connect()) {
                    System.out.println("step 6: another connection sees:         " + balances(other)
                            + "   <- uncommitted = invisible (queries/04 teaches this by hand)");
                }

                // THE NON-DESTRUCTIVE RULE: this demo must leave the seed EXACTLY as it
                // found it, so the transaction ends in rollback — not commit. Atomicity
                // is proven by the seed balances snapping back to 1500.00 / 5200.00.
                c.rollback();
                System.out.println("step 7: rollback() — both UPDATEs undone TOGETHER, as one unit");
            } finally {
                if (!c.getAutoCommit()) {
                    c.rollback();   // belt-and-braces: an exception above must never leave the txn open
                }
            }
            System.out.println("step 8: AFTER rollback:                  " + balances(c));
            System.out.println("verdict: atomic (both or neither) AND non-destructive (seed untouched).");
            System.out.println("In stage-2, @Transactional on the service method is exactly this");
            System.out.println("setAutoCommit(false) / commit / rollback dance — managed for you.");
        }

        banner("3. Savepoints — roll back PART of a transaction (TEMP table, safe to COMMIT)");
        // Savepoints need a WRITE that may stay committed, so this one runs against a
        // TEMP table created INSIDE the transaction with ON COMMIT DROP: committing is
        // harmless because the table (and its rows) dies with the transaction/session.
        try (Connection c = Db.connect()) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TEMP TABLE temp_sp (note VARCHAR(20) NOT NULL) ON COMMIT DROP");
                st.execute("INSERT INTO temp_sp (note) VALUES ('first')");
                Savepoint afterFirst = c.setSavepoint("after_first");   // a named checkpoint mid-transaction
                st.execute("INSERT INTO temp_sp (note) VALUES ('second')");
                System.out.println("step 1: inserted 'first', set savepoint, inserted 'second'");
                System.out.println("step 2: rows before partial rollback = " + tempRows(c));

                c.rollback(afterFirst);   // undo ONLY what happened after the savepoint
                System.out.println("step 3: rollback(savepoint) — 'second' undone, 'first' KEPT");
                System.out.println("step 4: rows after partial rollback  = " + tempRows(c));

                c.commit();               // commits 'first' — and ON COMMIT DROP destroys the TEMP table
                System.out.println("step 5: commit() — transaction done; the TEMP table dropped itself");
                System.out.println("verdict: savepoints = checkpoints inside ONE transaction; stage-1 queries/04");
                System.out.println("         uses them by hand, JDBC exposes them as setSavepoint/rollback(sp).");
            } catch (SQLException e) {
                c.rollback();             // any failure: undo everything, leave nothing behind
                throw e;
            } finally {
                if (!c.getAutoCommit()) {
                    c.rollback();
                }
            }
        }

        banner("4. In one sentence");
        System.out.println("auto-commit makes every statement a lone bet; a transaction groups the bets so");
        System.out.println("a transfer is all-or-nothing — and a demo transaction ends in ROLLBACK so the");
        System.out.println("shared seed data survives teaching.");
    }

    /** Looks up a seed account id by owner email + type (parameterized — see InjectionDemo). */
    private static long accountId(Connection c, String email, String type) throws SQLException {
        String sql = """
                SELECT a.id
                FROM account a
                JOIN customer c ON c.id = a.customer_id
                WHERE c.email = ? AND a.type = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("seed account not found: " + email + " / " + type);
                }
                return rs.getLong("id");
            }
        }
    }

    /** One-line view of Alice's two balances, e.g. {CHECKING=1500.00, SAVINGS=5200.00}. */
    private static String balances(Connection c) throws SQLException {
        String sql = """
                SELECT a.type, a.balance
                FROM account a
                JOIN customer cu ON cu.id = a.customer_id
                WHERE cu.email = 'alice@securebank.dev'
                ORDER BY a.type
                """;
        StringBuilder sb = new StringBuilder("{");
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(rs.getString("type")).append('=').append(rs.getBigDecimal("balance"));
            }
        }
        return sb.append('}').toString();
    }

    /** Comma-joined contents of the temp_sp demo table. */
    private static String tempRows(Connection c) throws SQLException {
        StringBuilder sb = new StringBuilder("[");
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT note FROM temp_sp ORDER BY note")) {
            while (rs.next()) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }
                sb.append(rs.getString("note"));
            }
        }
        return sb.append(']').toString();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
