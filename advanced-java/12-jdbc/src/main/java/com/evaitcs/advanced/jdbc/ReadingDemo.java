package com.evaitcs.advanced.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo 2 — READING: the ResultSet cursor, typed getters, and the RowMapper
 * pattern. (README sections "Reading rows" and "The cursor".)
 *
 * The claims, printed as evidence:
 *   1. the ResultSet cursor starts BEFORE the first row — next() advances it and
 *      returns false when the rows run out; reading at the wrong position throws
 *   2. every column has a TYPED getter, and DECIMAL(15,2) money must come out as
 *      BigDecimal (never float), TIMESTAMPTZ as OffsetDateTime (java.time, not the
 *      legacy java.sql.Timestamp)
 *   3. a RowMapper lambda turns rows into records — the exact loop JdbcTemplate
 *      and Hibernate automate
 *
 * NON-DESTRUCTIVE: every statement here is a SELECT. Reads never touch the seed.
 */
public class ReadingDemo {

    /**
     * The module's RowMapper for `account` rows — reused by JdbcTest.
     * One lambda, one row: this is ALL an ORM's row-mapping magic is, plus
     * ResultSetMetaData (MetadataDemo) to discover the columns automatically.
     */
    static final RowMapper<Account> ACCOUNT_MAPPER = rs -> new Account(
            rs.getLong("id"),
            rs.getLong("customer_id"),
            rs.getString("type"),
            rs.getBigDecimal("balance"),   // DECIMAL(15,2) -> BigDecimal, scale 2, exact
            rs.getLong("version"));

    private static final String SELECT_CUSTOMERS = """
            SELECT id, name, email, created_at
            FROM customer
            ORDER BY id
            """;

    private static final String SELECT_ACCOUNTS = """
            SELECT id, customer_id, type, balance, version
            FROM account
            ORDER BY id
            """;

    public static void main(String[] args) throws SQLException {
        if (!Db.isAvailable()) {
            Db.printStartInstructions();
            return;
        }

        banner("1. Typed getters over the seed customers (java.time for TIMESTAMPTZ)");
        try (Connection c = Db.connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(SELECT_CUSTOMERS)) {
            // The classic while-loop: next() ADVANCES the cursor and reports whether a
            // row is there. The cursor started BEFORE row 1 — see banner 2.
            while (rs.next()) {
                long id = rs.getLong("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                // JDBC 4.2 maps TIMESTAMPTZ straight to java.time — no legacy types needed.
                OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);
                System.out.println("row " + id + ": " + name + " <" + email + "> created " + createdAt);
            }
            System.out.println("verdict: getString/getLong/getObject(col, Class) — the getter matches the column type.");
        }

        banner("2. The legacy bridge, shown ONCE: java.sql.Timestamp — and why java.time wins");
        try (Connection c = Db.connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(SELECT_CUSTOMERS)) {
            rs.next();
            Timestamp legacy = rs.getTimestamp("created_at");          // the pre-JDBC-4.2 way
            OffsetDateTime modern = rs.getObject("created_at", OffsetDateTime.class);
            System.out.println("step 1: getTimestamp(...)            = " + legacy + "  (mutable, java.util.Date subclass)");
            System.out.println("step 2: legacy.toInstant()           = " + legacy.toInstant() + "  (the escape hatch)");
            System.out.println("step 3: getObject(..., OffsetDateTime) = " + modern + "  (immutable, carries the UTC offset)");
            System.out.println("verdict: Timestamp exists for old code; new code reads TIMESTAMPTZ as java.time");
            System.out.println("         (java8-features module 08-java-time explains WHY immutable time types win).");
        }

        banner("3. The cursor state machine: before-first / at-row / after-last / closed");
        try (Connection c = Db.connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name FROM customer ORDER BY id")) {
            System.out.println("step 1: fresh ResultSet — isBeforeFirst() = " + rs.isBeforeFirst()
                    + ", isAfterLast() = " + rs.isAfterLast());
            try {
                rs.getString("name");   // reading BEFORE the first next() — the classic mistake
            } catch (SQLException e) {
                System.out.println("step 2: read before next() -> CAUGHT " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.out.println("        WHY: the cursor points at no row yet — there is nothing to read.");
            }
            while (rs.next()) {
                System.out.println("step 3: next() returned true — cursor at row " + rs.getRow() + ": " + rs.getString("name"));
            }
            System.out.println("step 4: next() returned false — isAfterLast() = " + rs.isAfterLast());
            System.out.println("verdict: next() is BOTH 'advance' and 'is there a row' — the loop condition IS the cursor.");
        }
        // Reading after close() is a separate SQLException — shown on a fresh statement:
        try (Connection c = Db.connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM customer")) {
            rs.next();
            rs.close();                 // try-with-resources would do this anyway; doing it early to prove the point
            try {
                rs.getString(1);
            } catch (SQLException e) {
                System.out.println("step 5: read after close() -> CAUGHT " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.out.println("verdict: a closed ResultSet has no server-side cursor — every getter throws.");
            }
        }

        banner("4. Column INDEX vs column LABEL — and indexes start at 1");
        try (Connection c = Db.connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name FROM customer ORDER BY id")) {
            rs.next();
            System.out.println("step 1: rs.getString(2)          = " + rs.getString(2) + "   (2nd selected column, ONE-based)");
            System.out.println("step 2: rs.getString(\"name\")     = " + rs.getString("name"));
            System.out.println("step 3: identical               = " + rs.getString(2).equals(rs.getString("name")));
            System.out.println("verdict: labels survive a column reorder; indexes do not. Prefer labels.");
        }

        banner("5. RowMapper: account rows -> Account records (what JdbcTemplate automates)");
        List<Account> accounts = query(SELECT_ACCOUNTS, ACCOUNT_MAPPER);
        for (Account a : accounts) {
            System.out.println("mapped: " + a);
        }
        // Money sanity: getBigDecimal preserved the DECIMAL(15,2) scale exactly.
        BigDecimal firstBalance = accounts.get(0).balance();
        System.out.println("step 1: first balance             = " + firstBalance + " (scale " + firstBalance.scale() + ")");
        System.out.println("verdict: scale 2, exact — DECIMAL(15,2) -> rs.getBigDecimal -> BigDecimal, never float.");
        System.out.println("The 12-line query() method below IS JdbcTemplate.query(sql, rowMapper) minus the generics.");

        banner("6. In one sentence");
        System.out.println("Reading is a cursor you advance with next(), typed getters that match the column");
        System.out.println("types, and a mapper function per row — the ORM automates the loop, not the idea.");
    }

    /**
     * The entire row-mapping loop, written out once so the automation is visible.
     * Spring's JdbcTemplate.query(sql, RowMapper) is this method with retry,
     * exception translation and DataSource plumbing added around it.
     */
    static <T> List<T> query(String sql, RowMapper<T> mapper) throws SQLException {
        List<T> out = new ArrayList<>();
        try (Connection c = Db.connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(mapper.map(rs));
            }
        }
        return out;
    }

    /** Parameterized variant used by the tests — same loop, PreparedStatement instead of Statement. */
    static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> out = new ArrayList<>();
        try (Connection c = Db.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);   // bind parameters are ONE-based too
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapper.map(rs));
                }
            }
        }
        return out;
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
