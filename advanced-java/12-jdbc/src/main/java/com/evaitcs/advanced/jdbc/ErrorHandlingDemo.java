package com.evaitcs.advanced.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Demo 7 — SQLException: checked, chained, and classified by SQLState.
 * (README section "SQLException"; ties to module 11's exception-translation
 * boundary and stage-2's ProblemDetail responses.)
 *
 * The claims:
 *   1. SQLException is CHECKED — the compiler forces a boundary decision (module 11)
 *   2. it carries SQLState (a STANDARD 5-character class code) and a vendor error
 *      code — enough to translate it into a domain exception without parsing messages
 *   3. it CHAINS: getNextException() links further failures (batches pile them up)
 *
 * NON-DESTRUCTIVE: the unique-violation INSERT runs inside a transaction that
 * ends in ROLLBACK (finally) — a rejected INSERT leaves no row anyway, and the
 * rollback guarantees it. The syntax error never reaches the data at all.
 */
public class ErrorHandlingDemo {

    public static void main(String[] args) throws SQLException {
        if (!Db.isAvailable()) {
            Db.printStartInstructions();
            return;
        }

        banner("1. A REAL unique violation: re-inserting a seeded email (rolled back)");
        try (Connection c = Db.connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO customer (name, email) VALUES (?, ?)")) {
                    ps.setString(1, "Dup Demo");
                    ps.setString(2, "alice@securebank.dev");   // uq_customer_email forbids this
                    ps.executeUpdate();
                    System.out.println("step 1: INSERT succeeded — unexpected! (seed may be missing?)");
                }
            } catch (SQLException e) {
                System.out.println("step 1: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.out.println("step 2: getSQLState()  = " + e.getSQLState() + "   <- STANDARD code, portable across databases");
                System.out.println("step 3: getErrorCode() = " + e.getErrorCode() + "      <- vendor code (PostgreSQL puts 0 here; the SQLState carries it)");
                System.out.println("step 4: classified     = " + classify(e.getSQLState()));
                int chainLength = 0;
                for (SQLException t = e; t != null; t = t.getNextException()) {
                    chainLength++;
                }
                System.out.println("step 5: exception chain length = " + chainLength
                        + " (getNextException links more — executeBatch chains one per failed row)");
                System.out.println("verdict: 23505 = unique_violation. Code can BRANCH on this number; parsing");
                System.out.println("         the human message string is brittle and database-specific. Never do it.");
            } finally {
                c.rollback();   // unconditional: a failed INSERT aborts the txn anyway; rollback makes it explicit
                System.out.println("step 6: rollback() — the customer table is exactly as the seed left it");
            }
        }

        banner("2. A syntax error — a different SQLState CLASS, classified by one switch");
        try (Connection c = Db.connect();
             Statement st = c.createStatement()) {
            st.execute("SELEKT 1");   // typo on purpose: the server rejects it at PARSE time
        } catch (SQLException e) {
            System.out.println("step 1: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("step 2: getSQLState() = " + e.getSQLState());
            System.out.println("step 3: classified    = " + classify(e.getSQLState()));
            System.out.println("verdict: the first TWO characters of the SQLState are the class — 23 = integrity,");
            System.out.println("         42 = syntax/access. One switch handles every database error you meet.");
        }

        banner("3. Why CHECKED, and where the translation belongs (module 11)");
        System.out.println("SQLException is checked: the compiler forces every call site to decide.");
        System.out.println("The right decision at a repository boundary (module 11, section 6):");
        System.out.println("  catch (SQLException e) { throw new DuplicateEmailException(email, e); }");
        System.out.println("— a DOMAIN exception the service layer understands, original kept as the cause.");
        System.out.println("Stage-2 goes one step further: the advice layer turns that domain exception into");
        System.out.println("an RFC 7807 ProblemDetail — a machine-readable 409 for the frontend.");
        System.out.println("In one sentence: SQLException is the border language; translate it AT the border,");
        System.out.println("never let SQLState codes leak into your services.");

        banner("4. In one sentence");
        System.out.println("Read getSQLState(), branch on its class, wrap into a domain exception, roll the");
        System.out.println("transaction back — that is the entire professional JDBC error story.");
    }

    /**
     * The Java 21 idiom for error classification: a switch with GUARDED patterns
     * (case String s when s.startsWith(...)). SQLState classes are standardized by
     * ISO/ANSI SQL, so the first two characters are portable across databases.
     */
    static String classify(String sqlState) {
        String state = (sqlState == null) ? "" : sqlState;
        return switch (state) {
            case String s when s.startsWith("23") -> "integrity constraint violation — the DATA broke a rule (unique, check, FK)";
            case String s when s.startsWith("42") -> "SQL error — the STATEMENT is wrong (syntax, unknown table, access)";
            case String s when s.startsWith("40") -> "transaction rollback — deadlock/serialization; retry the whole unit of work";
            case String s when s.startsWith("08") -> "connection exception — the LINK to the server broke";
            case String s when s.startsWith("22") -> "data exception — bad VALUE (numeric overflow, invalid text)";
            case "" -> "no SQLState — the driver could not classify the failure";
            default -> "SQLState " + state + " — look it up in the SQLState standard";
        };
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
