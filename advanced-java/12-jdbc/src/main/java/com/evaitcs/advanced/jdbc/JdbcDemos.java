package com.evaitcs.advanced.jdbc;

import java.sql.SQLException;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 12-jdbc compile exec:java
 *
 * THE GATE: if PostgreSQL is not reachable, this prints the start instructions
 * and RETURNS NORMALLY (exit code 0, no stack trace) — the build never fails
 * because the database is down (README "Prerequisites").
 *
 * The order is the argument: first the moving parts and the self-registering
 * driver, then reading (cursor + typed getters + RowMapper), then the security
 * centerpiece (injection), then transactions (rollback-protected), batching,
 * metadata (how ORMs see), and finally SQLException handling.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 12-jdbc compile exec:java "-Dexec.mainClass=com.evaitcs.advanced.jdbc.InjectionDemo"
 */
public class JdbcDemos {

    public static void main(String[] args) throws SQLException {
        if (!Db.isAvailable()) {
            System.out.println("############################################################");
            System.out.println("# 12-jdbc — database not reachable; nothing to demo.");
            System.out.println("############################################################");
            Db.printStartInstructions();
            return;   // normal return — requirement (A): the reactor must always build
        }

        System.out.println("Connected plan: every write demo below ROLLBACKs or uses a TEMP table.");
        System.out.println("The stage-1 seed data (Alice/Bob/Carol) is READ-ONLY for this module.");

        section("ConnectionDemo — DriverManager, the self-registering driver, and one live Connection");
        ConnectionDemo.main(args);

        section("ReadingDemo — the ResultSet cursor, typed getters, java.time, and the RowMapper loop");
        ReadingDemo.main(args);

        section("InjectionDemo — string concatenation vs bind parameters: data can never become code");
        InjectionDemo.main(args);

        section("TransactionDemo — auto-commit, the atomic transfer (rolled back), savepoints");
        TransactionDemo.main(args);

        section("BatchDemo — addBatch/executeBatch: N round-trips become one (rolled back)");
        BatchDemo.main(args);

        section("MetadataDemo — DatabaseMetaData and ResultSetMetaData: how an ORM sees your columns");
        MetadataDemo.main(args);

        section("ErrorHandlingDemo — SQLException: SQLState classes, chained exceptions, translation");
        ErrorHandlingDemo.main(args);
    }

    static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
