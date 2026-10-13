package com.evaitcs.advanced.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * Demo 1 — THE MOVING PARTS, and the driver nobody loaded. (README sections
 * "The moving parts" and "Loading the driver".)
 *
 * The claim: a JDBC program is a fixed chain — DriverManager finds a Driver, the
 * Driver builds a Connection, the Connection mints Statements, a Statement
 * executes SQL and hands back a ResultSet — and since JDBC 4.0 (Java 6) the
 * driver registers ITSELF via ServiceLoader. No Class.forName anywhere.
 *
 * NON-DESTRUCTIVE: this demo only READS metadata and runs SELECT 1.
 */
public class ConnectionDemo {

    public static void main(String[] args) throws SQLException {
        if (!Db.isAvailable()) {
            Db.printStartInstructions();
            return;
        }

        banner("1. Anatomy of the JDBC URL — jdbc:postgresql://host:port/database");
        System.out.println("step 1: Db.url()                = " + Db.url());
        System.out.println("step 2: 'jdbc'                  = the protocol — always literally 'jdbc'");
        System.out.println("step 3: 'postgresql'            = the subprotocol — WHICH driver family answers");
        System.out.println("step 4: '//localhost:5432/...'  = the subname — WHICH server and database");
        System.out.println("verdict: DriverManager matches the subprotocol against registered drivers.");

        banner("2. The driver is ALREADY registered — nobody called Class.forName");
        // Before JDBC 4.0 you had to write Class.forName("org.postgresql.Driver") to force
        // the driver's static initializer to run and register itself. Since Java 6 the JDK
        // runs a ServiceLoader over META-INF/services/java.sql.Driver inside every driver JAR
        // the first time DriverManager is touched — registration is automatic.
        String drivers = DriverManager.drivers()
                .map(d -> d.getClass().getName() + " (v" + d.getMajorVersion() + "." + d.getMinorVersion() + ")")
                .collect(Collectors.joining(", "));
        System.out.println("step 1: DriverManager.drivers() = " + drivers);
        System.out.println("step 2: that list was filled by ServiceLoader reading");
        System.out.println("        META-INF/services/java.sql.Driver inside the postgresql JAR");
        System.out.println("verdict: Class.forName is history. The driver on the classpath is enough.");

        banner("3. One Connection, live facts about the server");
        try (Connection c = Db.connect()) {
            // DatabaseMetaData = the server's answer sheet about ITSELF (MetadataDemo goes deeper).
            DatabaseMetaData md = c.getMetaData();
            System.out.println("step 1: product                 = " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            System.out.println("step 2: driver                  = " + md.getDriverName() + " " + md.getDriverVersion());
            System.out.println("step 3: c.getAutoCommit()       = " + c.getAutoCommit()
                    + "   <- the DEFAULT: every statement commits alone (TransactionDemo)");
            System.out.println("step 4: c.getCatalog()          = " + c.getCatalog());
            System.out.println("verdict: this Connection object is a live TCP session with the server.");
        }

        banner("4. Why stage-2 pools these (README 'Connection pooling') — one handshake, timed");
        long t0 = System.nanoTime();
        try (Connection c = Db.connect()) {
            c.createStatement().execute("SELECT 1");
        }
        long millis = (System.nanoTime() - t0) / 1_000_000;
        // Printed, never asserted: this number depends on the machine and Docker setup.
        System.out.println("one connect + SELECT 1 + close took ~" + millis + " ms (illustrative — machine-dependent, never asserted)");
        System.out.println("That cost is a TCP handshake PLUS a PostgreSQL authentication exchange.");
        System.out.println("A connection POOL (HikariCP in stage-2) pays it once at startup, then RENTS");
        System.out.println("warm connections out through the javax.sql.DataSource interface.");
        System.out.println("verdict: DriverManager.getConnection per request works, but it is slow — pool it.");

        banner("5. In one sentence");
        System.out.println("JDBC is a chain of small interfaces — DriverManager finds the Driver, the Driver");
        System.out.println("builds the Connection, the Connection mints Statements — and Spring Data JPA is");
        System.out.println("a builder on top of EXACTLY this chain. Nothing is magic.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
