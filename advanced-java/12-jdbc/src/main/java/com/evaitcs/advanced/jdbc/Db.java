package com.evaitcs.advanced.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * The module's ONE connection contract — every demo and every test goes through
 * this class, so the connection facts live in exactly one place (README section
 * "Prerequisites: start the database").
 *
 * Two promises this module makes to the rest of the repo (README, top):
 *
 *   1. THE REACTOR ALWAYS BUILDS. When PostgreSQL is down, isAvailable() returns
 *      false FAST (a 2-second login timeout, never a hang), demos print start
 *      instructions and return normally, and JdbcTest skips via JUnit Assumptions.
 *      A missing database must never turn the build red.
 *
 *   2. NON-DESTRUCTIVE. The stage-1 database holds shared seed data (Alice/Bob/
 *      Carol and their balances) that other courses rely on. Nothing in this
 *      module may permanently change it: every write demo runs inside a
 *      transaction that ends in ROLLBACK, or against a TEMP table that vanishes
 *      when the connection closes. Reads are free.
 *
 * Resolution order for each connection fact (first hit wins):
 *   environment variable  ->  system property  ->  hardcoded default
 *   JDBC_URL              ->  jdbc.url         ->  jdbc:postgresql://localhost:5432/securebank
 *   JDBC_USER             ->  jdbc.user        ->  securebank
 *   JDBC_PASSWORD         ->  jdbc.password    ->  securebank
 */
public final class Db {

    public static final String DEFAULT_URL      = "jdbc:postgresql://localhost:5432/securebank";
    public static final String DEFAULT_USER     = "securebank";
    public static final String DEFAULT_PASSWORD = "securebank";   // demo-only credential (stage-1)

    static {
        // Fail FAST when the database is down. Without this, DriverManager waits for
        // the OS-level TCP timeout (tens of seconds) and the build appears to hang.
        // "Login timeout" = how long getConnection() may take before giving up.
        DriverManager.setLoginTimeout(2);
    }

    private Db() { }   // utility class: no instances

    public static String url()      { return configured("JDBC_URL", "jdbc.url", DEFAULT_URL); }
    public static String user()     { return configured("JDBC_USER", "jdbc.user", DEFAULT_USER); }
    public static String password() { return configured("JDBC_PASSWORD", "jdbc.password", DEFAULT_PASSWORD); }

    private static String configured(String envKey, String propKey, String fallback) {
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromProp = System.getProperty(propKey);
        if (fromProp != null && !fromProp.isBlank()) {
            return fromProp;
        }
        return fallback;
    }

    /**
     * A live Connection to the configured server, or a SQLException within ~2 seconds.
     * Callers own the Connection: use try-with-resources so it always closes — and
     * remember a closing connection also DESTROYS its TEMP tables (that is what makes
     * the write demos non-destructive).
     */
    public static Connection connect() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", user());
        props.setProperty("password", password());
        // Driver-level belt-and-braces on top of DriverManager.setLoginTimeout:
        // connectTimeout = TCP socket establishment, loginTimeout = whole handshake.
        props.setProperty("connectTimeout", "2");
        props.setProperty("loginTimeout", "3");
        return DriverManager.getConnection(url(), props);
    }

    /**
     * The gate every demo and test checks FIRST. Attempts a real connection plus
     * SELECT 1 (the cheapest possible round-trip); ANY SQLException — refused port,
     * unknown role, wrong database, timeout — means "not available", never a crash.
     */
    public static boolean isAvailable() {
        try (Connection c = connect();
             Statement s = c.createStatement()) {
            s.execute("SELECT 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /** One-line description safe to print — never echoes the password. */
    public static String describe() {
        return url() + " as user '" + user() + "'";
    }

    /** Printed by every demo main() when the database is down, then the demo RETURNS. */
    public static void printStartInstructions() {
        System.out.println();
        System.out.println("PostgreSQL is NOT reachable at " + describe() + ".");
        System.out.println("This module never fails without it — demos return normally, tests SKIP.");
        System.out.println();
        System.out.println("Start the stage-1 database (from the advanced-java folder):");
        System.out.println("    cd ..\\stage-1-database");
        System.out.println("    docker compose up -d");
        System.out.println("    docker compose exec db pg_isready -U securebank    # wait for 'accepting connections'");
        System.out.println();
        System.out.println("Or point this module at ANY PostgreSQL server (README override table):");
        System.out.println("    env  JDBC_URL / JDBC_USER / JDBC_PASSWORD");
        System.out.println("    or   -Djdbc.url=... -Djdbc.user=... -Djdbc.password=...");
    }
}
