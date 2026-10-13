package com.evaitcs.advanced.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Demo 6 — METADATA: how the database describes itself, and how an ORM learns
 * your columns without being told. (README section "Metadata".)
 *
 * Two different "meta" objects, easy to confuse:
 *   ResultSetMetaData — about the COLUMNS of one query result (name, type, scale).
 *                       This is what Hibernate reads to auto-map rows onto fields.
 *   DatabaseMetaData  — about the SERVER and driver (product, version, capabilities).
 *                       This is what ddl-auto=validate and health checks consult.
 *
 * NON-DESTRUCTIVE: metadata reads only — SELECT * FROM account plus getMetaData().
 */
public class MetadataDemo {

    public static void main(String[] args) throws SQLException {
        if (!Db.isAvailable()) {
            Db.printStartInstructions();
            return;
        }

        try (Connection c = Db.connect()) {

            banner("1. DatabaseMetaData — the server's answer sheet about ITSELF");
            DatabaseMetaData md = c.getMetaData();
            System.out.println("step 1: product          = " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            System.out.println("step 2: driver           = " + md.getDriverName() + " " + md.getDriverVersion());
            System.out.println("step 3: URL              = " + md.getURL());
            System.out.println("step 4: supports txns    = " + md.supportsTransactions());
            System.out.println("step 5: default isolation= " + isolationName(md.getDefaultTransactionIsolation()));
            System.out.println("verdict: tools (DBeaver, Flyway, Hibernate's validate, Boot's health check)");
            System.out.println("         all ask THIS object what the server can do before they do anything.");

            banner("2. ResultSetMetaData — the account table's columns, discovered at runtime");
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM account ORDER BY id")) {
                // Works even before next(): the COLUMN information is known as soon as the
                // statement is parsed — zero rows would be fine. That is why an ORM can map
                // a result it has not read yet.
                ResultSetMetaData rmd = rs.getMetaData();
                System.out.println("step 1: column count = " + rmd.getColumnCount());
                System.out.printf("step 2: %-14s %-10s %-9s %-6s %s%n", "LABEL", "SQL TYPE", "PRECISION", "SCALE", "JAVA CLASS");
                for (int i = 1; i <= rmd.getColumnCount(); i++) {
                    System.out.printf("        %-14s %-10s %-9d %-6d %s%n",
                            rmd.getColumnLabel(i),
                            rmd.getColumnTypeName(i),
                            rmd.getPrecision(i),
                            rmd.getScale(i),
                            rmd.getColumnClassName(i));
                }
                System.out.println("verdict: balance is int8? No — numeric(15,2) -> java.math.BigDecimal, scale 2.");
                System.out.println("Hibernate reads EXACTLY these columns names/types to bind entity fields");
                System.out.println("(schema.sql's snake_case -> camelCase mapping is a naming strategy on top of this).");
            }

            banner("3. In one sentence");
            System.out.println("Metadata is the database describing itself — ResultSetMetaData per query,");
            System.out.println("DatabaseMetaData per server — and 'ORM auto-mapping' is a for-loop over it.");
        }
    }

    /** java.sql.Connection constants -> readable names (a tiny pattern-mapping switch). */
    private static String isolationName(int level) {
        return switch (level) {
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "READ UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED -> "READ COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ -> "REPEATABLE READ";
            case Connection.TRANSACTION_SERIALIZABLE -> "SERIALIZABLE";
            default -> "unknown (" + level + ")";
        };
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
