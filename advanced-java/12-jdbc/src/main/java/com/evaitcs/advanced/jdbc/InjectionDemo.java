package com.evaitcs.advanced.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Demo 3 — SQL INJECTION, defused. THE SECURITY CENTERPIECE. (README section
 * "SQL injection and PreparedStatement".)
 *
 * The claim: string-concatenated SQL lets DATA be parsed as CODE; a
 * PreparedStatement with ? bind parameters makes that structurally impossible —
 * the value travels to the server separately from the SQL text and is bound as
 * ONE literal.
 *
 * NON-DESTRUCTIVE, LOUDLY: the classic payload mentions DROP TABLE account, so
 * the concatenated SQL is only ever PRINTED, never executed — executing it is
 * precisely the disaster this demo teaches you to prevent. Everything actually
 * executed here targets a session-local TEMP TABLE (temp_demo) that vanishes
 * when the connection closes, plus read-only SELECTs on the real schema.
 */
public class InjectionDemo {

    /** The textbook injection payload — a NAME a user typed into a web form. */
    static final String PAYLOAD = "Robert'); DROP TABLE account;--";

    public static void main(String[] args) throws SQLException {
        if (!Db.isAvailable()) {
            Db.printStartInstructions();
            return;
        }

        try (Connection c = Db.connect()) {
            c.setAutoCommit(true);   // fine here: every write below targets the TEMP table only

            // TEMP table = session-local. It is invisible to every other connection and
            // is DESTROYED when this connection closes — the write path cannot touch the seed.
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TEMP TABLE temp_demo (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, note VARCHAR(200) NOT NULL)");
            }
            try {
                banner("1. The vulnerable way: concatenation — PRINTED, NEVER EXECUTED");
                String naive = "INSERT INTO temp_demo (note) VALUES ('" + PAYLOAD + "')";
                System.out.println("step 1: user 'name'      = " + PAYLOAD);
                System.out.println("step 2: assembled SQL    = " + naive);
                System.out.println("step 3: how PostgreSQL PARSES that text — three statements, not one:");
                System.out.println("          1. INSERT INTO temp_demo (note) VALUES ('Robert')   <- what we wanted");
                System.out.println("          2. DROP TABLE account                               <- what the ATTACKER wanted");
                System.out.println("          3. --')                                             <- comment; the tail is ignored");
                System.out.println("verdict: concatenation hands the user a SQL editor. We only PRINT this string —");
                System.out.println("         executing it against the real schema is the catastrophe, so we never do.");

                banner("2. Even a HONEST apostrophe breaks concatenation (executed — harmless, TEMP table)");
                String honest = "O'Brien";
                String naiveHonest = "INSERT INTO temp_demo (note) VALUES ('" + honest + "')";
                System.out.println("step 1: assembled SQL    = " + naiveHonest);
                try (Statement st = c.createStatement()) {
                    st.execute(naiveHonest);
                    System.out.println("step 2: executed with no error — unexpected!");
                } catch (SQLException e) {
                    System.out.println("step 2: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    System.out.println("        WHY: the apostrophe in O'Brien closed the string literal early;");
                    System.out.println("        'Brien' is now dangling SQL text -> syntax error (SQLState " + e.getSQLState() + ").");
                    System.out.println("verdict: no attacker needed — escaping by hand is a bug factory. Use bind parameters.");
                }

                banner("3. The safe way: PreparedStatement binds the payload as ONE literal value");
                // The ? placeholder is a HOLE in the SQL. The server parses and plans the
                // statement with the hole still in it; the value is sent SEPARATELY and can
                // only ever fill the hole — it can never add statements, quotes, or comments.
                String safe = "INSERT INTO temp_demo (note) VALUES (?)";
                System.out.println("step 1: SQL text sent    = " + safe + "   (parsed BEFORE the value exists)");
                try (PreparedStatement ps = c.prepareStatement(safe)) {
                    ps.setString(1, PAYLOAD);            // setString — the driver also sends the TYPE
                    int inserted = ps.executeUpdate();
                    System.out.println("step 2: bound value      = " + PAYLOAD);
                    System.out.println("step 3: executeUpdate()  = " + inserted + " row inserted, no syntax error, no DROP");
                }
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT note FROM temp_demo ORDER BY id")) {
                    rs.next();
                    String back = rs.getString("note");
                    System.out.println("step 4: SELECTed back    = " + back);
                    System.out.println("step 5: byte-for-byte identical = " + PAYLOAD.equals(back));
                    System.out.println("verdict: the payload went in as DATA and came out as DATA. Injection is impossible.");
                }

                banner("4. Proof the real schema survived: the account table is still there");
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT count(*) FROM account")) {
                    rs.next();
                    long count = rs.getLong(1);
                    System.out.println("step 1: SELECT count(*) FROM account = " + count + " (seed rows, untouched)");
                    System.out.println("verdict: nothing was dropped, nothing was mutated — TEMP table + bound parameters.");
                }

                banner("5. Bonus: why PreparedStatement is also FASTER and more correct");
                System.out.println("- plan caching: the server parses/plans 'INSERT ... VALUES (?)' ONCE and reuses the");
                System.out.println("  plan for every execution; concatenated SQL is a NEW statement string every time.");
                System.out.println("- correct typing: setBigDecimal(1, money) binds a numeric, not a string — no locale");
                System.out.println("  or quote-escaping bugs, and the server checks the type against the column.");
                System.out.println("- In one sentence: PreparedStatement is safer AND faster — concatenation is never the answer.");
            } finally {
                // Hygiene: drop the TEMP table explicitly. Even without this, closing the
                // connection would destroy it — TEMP tables are session-local by definition.
                try (Statement st = c.createStatement()) {
                    st.execute("DROP TABLE IF EXISTS temp_demo");
                }
            }
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
