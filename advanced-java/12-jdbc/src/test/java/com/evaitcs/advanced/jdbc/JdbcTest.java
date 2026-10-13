package com.evaitcs.advanced.jdbc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests as PROOF for the claims made in the demos — against the REAL stage-1
 * PostgreSQL (schema.sql + seed.sql). Run with:
 *   .\mvnw.cmd -pl 12-jdbc test
 *
 * THE GATE (requirement A): @BeforeAll calls Db.isAvailable() and assumeTrue()s
 * it. Database down -> every test SKIPS (aborted assumption, never a failure)
 * and the build stays GREEN. Database up -> every test is deterministic, because
 * the seed is fixed: Alice/Bob/Carol, balances 1500.00/5200.00/300.00, every run.
 *
 * NON-DESTRUCTION (requirement B): tests that write use either a TEMP table
 * (session-local, destroyed on disconnect, dropped explicitly for hygiene) or a
 * transaction that ends in ROLLBACK in a finally block. No test leaves a trace.
 */
class JdbcTest {

    /** Stored so the skip reason is visible and the gate is checked exactly once. */
    static boolean dbUp;

    @BeforeAll
    static void requireDatabase() {
        dbUp = Db.isAvailable();
        assumeTrue(dbUp,
                "Postgres not running — start it: cd ../stage-1-database && docker compose up -d");
    }

    @Test
    @DisplayName("metadata: the connection reports a PostgreSQL server (DatabaseMetaData)")
    void connectionReportsProductMetadata() throws SQLException {
        try (Connection c = Db.connect()) {
            DatabaseMetaData md = c.getMetaData();
            String product = md.getDatabaseProductName();
            assertTrue(product.contains("PostgreSQL"),
                    "expected a PostgreSQL server, got: " + product);
            System.out.println("product = " + product + " " + md.getDatabaseProductVersion()
                    + ", driver = " + md.getDriverName() + " " + md.getDriverVersion());
        }
    }

    @Test
    @DisplayName("seed: SELECT over customer finds exactly the three known people, Alice/Bob/Carol")
    void selectSeedCustomersFindsAliceBobCarol() throws SQLException {
        Set<String> emails = new HashSet<>();
        Set<String> names = new HashSet<>();
        String sql = """
                SELECT id, name, email, created_at
                FROM customer
                ORDER BY id
                """;
        try (Connection c = Db.connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                emails.add(rs.getString("email"));
                names.add(rs.getString("name"));
                // TIMESTAMPTZ -> java.time directly (JDBC 4.2); never null per schema.
                assertTrue(rs.getObject("created_at", java.time.OffsetDateTime.class) != null);
            }
        }
        assertTrue(emails.containsAll(List.of("alice@securebank.dev", "bob@securebank.dev", "carol@securebank.dev")),
                "the three seeded emails must exist, got: " + emails);
        assertTrue(names.containsAll(List.of("Alice Nguyen", "Bob Rivera", "Carol Okafor")),
                "the three seeded names must exist, got: " + names);
        System.out.println("read-only proof: " + emails.size() + " customers found, all three seeds present");
    }

    @Test
    @DisplayName("cursor: reading a column BEFORE next() (or after close) throws SQLException")
    void resultSetCursorRequiresNextBeforeRead() throws SQLException {
        // Fresh statement: a brand-new ResultSet's cursor sits BEFORE the first row.
        try (Connection c = Db.connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name FROM customer ORDER BY id")) {
            assertTrue(rs.isBeforeFirst(), "a fresh cursor is positioned before the first row");

            SQLException beforeFirst = assertThrows(SQLException.class, () -> rs.getString("name"),
                    "reading before the first next() must throw");
            System.out.println("CAUGHT -> " + beforeFirst.getClass().getSimpleName() + ": " + beforeFirst.getMessage());

            assertTrue(rs.next(), "the seed guarantees at least one row");
            assertTrue(rs.getString("name").length() > 0, "at a row, the getter works");
        }
        // After close(): a second, separate proof on its own statement.
        try (Connection c = Db.connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM customer")) {
            rs.next();
            rs.close();
            SQLException afterClose = assertThrows(SQLException.class, () -> rs.getString(1),
                    "reading a closed ResultSet must throw");
            System.out.println("CAUGHT -> " + afterClose.getClass().getSimpleName() + ": " + afterClose.getMessage());
        }
        System.out.println("the cursor is a position, not a snapshot: before-first and closed both mean 'no row here'");
    }

    @Test
    @DisplayName("money: the seed balances come back as BigDecimal with scale 2 — 1500.00 / 5200.00 / 300.00")
    void accountBalancesMatchSeed() throws SQLException {
        assertEquals(new BigDecimal("1500.00"), balanceOf("alice@securebank.dev", "CHECKING"));
        assertEquals(new BigDecimal("5200.00"), balanceOf("alice@securebank.dev", "SAVINGS"));
        assertEquals(new BigDecimal("300.00"), balanceOf("bob@securebank.dev", "CHECKING"));

        BigDecimal checking = balanceOf("alice@securebank.dev", "CHECKING");
        assertEquals(2, checking.scale(), "DECIMAL(15,2) must arrive with scale exactly 2");
        System.out.println("Alice CHECKING=" + checking + " SAVINGS=" + balanceOf("alice@securebank.dev", "SAVINGS")
                + " Bob CHECKING=" + balanceOf("bob@securebank.dev", "CHECKING")
                + " — BigDecimal.equals compares scale too, so this asserts the exact DECIMAL(15,2) shape");
    }

    @Test
    @DisplayName("security: a PreparedStatement binds the DROP TABLE injection payload as one literal string")
    void preparedStatementTreatsInjectionAsLiteralData() throws SQLException {
        String payload = InjectionDemo.PAYLOAD;   // "Robert'); DROP TABLE account;--"
        try (Connection c = Db.connect()) {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TEMP TABLE temp_demo (note VARCHAR(200) NOT NULL)");
            }
            try {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO temp_demo (note) VALUES (?)")) {
                    ps.setString(1, payload);
                    assertEquals(1, ps.executeUpdate(), "the payload inserts as ONE ordinary value");
                }
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT note FROM temp_demo")) {
                    assertTrue(rs.next());
                    String back = rs.getString("note");
                    assertEquals(payload, back, "what comes out is byte-for-byte what went in — data, not code");
                    System.out.println("bound and returned verbatim: " + back);
                }
            } finally {
                try (Statement st = c.createStatement()) {
                    st.execute("DROP TABLE IF EXISTS temp_demo");   // hygiene; disconnect would erase it anyway
                }
            }
            // The payload MENTIONED dropping the real account table — prove it did not:
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT count(*) FROM account")) {
                assertTrue(rs.next());
                long count = rs.getLong(1);
                assertTrue(count >= 3, "the three seed accounts must still exist, found " + count);
                System.out.println("account table alive with " + count + " rows — the semicolon never became a statement boundary");
            }
        }
    }

    @Test
    @DisplayName("atomicity: a debit+credit transfer runs in one transaction, and rollback restores the seed exactly")
    void transferIsAtomicAndRolledBack() throws SQLException {
        BigDecimal amount = new BigDecimal("100.00");
        try (Connection c = Db.connect()) {
            long checkingId = accountId(c, "alice@securebank.dev", "CHECKING");
            long savingsId = accountId(c, "alice@securebank.dev", "SAVINGS");
            BigDecimal checkingBefore = balanceOf("alice@securebank.dev", "CHECKING");
            BigDecimal savingsBefore = balanceOf("alice@securebank.dev", "SAVINGS");

            c.setAutoCommit(false);
            try {
                try (PreparedStatement debit = c.prepareStatement(
                        "UPDATE account SET balance = balance - ? WHERE id = ?");
                     PreparedStatement credit = c.prepareStatement(
                        "UPDATE account SET balance = balance + ? WHERE id = ?")) {
                    debit.setBigDecimal(1, amount);
                    debit.setLong(2, checkingId);
                    assertEquals(1, debit.executeUpdate());
                    credit.setBigDecimal(1, amount);
                    credit.setLong(2, savingsId);
                    assertEquals(1, credit.executeUpdate());
                }
                // INSIDE the open transaction, our own uncommitted writes are visible:
                assertEquals(checkingBefore.subtract(amount), balanceOn(c, checkingId), "debit visible inside the txn");
                assertEquals(savingsBefore.add(amount), balanceOn(c, savingsId), "credit visible inside the txn");
                System.out.println("inside txn: CHECKING=" + balanceOn(c, checkingId) + " SAVINGS=" + balanceOn(c, savingsId));
            } finally {
                c.rollback();   // THE NON-DESTRUCTIVE RULE: rollback unconditionally, assertions or not
                c.setAutoCommit(true);
            }

            // AFTER rollback the seed is back — atomicity proven AND nothing destroyed:
            assertEquals(checkingBefore, balanceOf("alice@securebank.dev", "CHECKING"), "rollback undid the debit");
            assertEquals(savingsBefore, balanceOf("alice@securebank.dev", "SAVINGS"), "rollback undid the credit");
            System.out.println("after rollback: CHECKING=" + checkingBefore + " SAVINGS=" + savingsBefore
                    + " — both halves moved together, then vanished together");
        }
    }

    @Test
    @DisplayName("errors: a duplicate seed email raises SQLException with SQLState 23505 (unique violation)")
    void uniqueViolationCarriesSqlState() throws SQLException {
        try (Connection c = Db.connect()) {
            c.setAutoCommit(false);
            SQLException e;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO customer (name, email) VALUES (?, ?)")) {
                ps.setString(1, "Dup Test");
                ps.setString(2, "alice@securebank.dev");   // uq_customer_email says no
                e = assertThrows(SQLException.class, ps::executeUpdate);
            } finally {
                c.rollback();   // rejected row leaves nothing, rollback guarantees it
            }
            assertEquals("23505", e.getSQLState(), "PostgreSQL's unique_violation SQLState");
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": SQLState=" + e.getSQLState()
                    + " classified as: " + ErrorHandlingDemo.classify(e.getSQLState()));
            System.out.println("stage-2 translates exactly this state into a 409 ProblemDetail — branch on the code, never the message");
        }
    }

    @Test
    @DisplayName("batching: executeBatch returns one update count per staged row, summing to N")
    void batchInsertReportsRowCounts() throws SQLException {
        final int n = 5;
        try (Connection c = Db.connect()) {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TEMP TABLE temp_batch_test (id BIGINT NOT NULL, note VARCHAR(20) NOT NULL)");
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO temp_batch_test (id, note) VALUES (?, ?)")) {
                for (int i = 1; i <= n; i++) {
                    ps.setLong(1, i);
                    ps.setString(2, "row-" + i);
                    ps.addBatch();
                }
                int[] counts = ps.executeBatch();
                assertEquals(n, counts.length, "one entry per staged statement");
                int sum = 0;
                for (int count : counts) {
                    sum += count;
                }
                assertEquals(n, sum, "every staged row reports exactly one inserted row");
                System.out.println("counts = " + java.util.Arrays.toString(counts) + " — " + n + " rows, one round-trip");
            } finally {
                try (Statement st = c.createStatement()) {
                    st.execute("DROP TABLE IF EXISTS temp_batch_test");   // auto-drops on disconnect anyway
                }
            }
        }
    }

    @Test
    @DisplayName("mapping: a functional RowMapper turns Alice's CHECKING row into an Account record")
    void resultMapMapsRowToRecord() throws SQLException {
        String sql = """
                SELECT a.id, a.customer_id, a.type, a.balance, a.version
                FROM account a
                JOIN customer c ON c.id = a.customer_id
                WHERE c.email = ? AND a.type = ?
                """;
        List<Account> rows = ReadingDemo.query(sql, ReadingDemo.ACCOUNT_MAPPER, "alice@securebank.dev", "CHECKING");
        assertEquals(1, rows.size(), "exactly one seeded account matches");

        Account a = rows.get(0);
        assertEquals("CHECKING", a.type());
        assertEquals(new BigDecimal("1500.00"), a.balance());
        assertTrue(a.id() > 0 && a.customerId() > 0, "ids are DB-generated identities, always positive");
        System.out.println("mapped record: " + a);
        System.out.println("JdbcTemplate.query(sql, rowMapper) is this loop with plumbing — same RowMapper idea");
    }

    // --- helpers (each opens its OWN connection: reads are free, nothing is written) ---

    private static BigDecimal balanceOf(String email, String type) throws SQLException {
        String sql = """
                SELECT a.balance
                FROM account a
                JOIN customer c ON c.id = a.customer_id
                WHERE c.email = ? AND a.type = ?
                """;
        try (Connection c = Db.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "seed account missing: " + email + "/" + type);
                return rs.getBigDecimal("balance");
            }
        }
    }

    /** Balance of one account BY ID, read on the GIVEN connection (sees that connection's open txn). */
    private static BigDecimal balanceOn(Connection c, long accountId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT balance FROM account WHERE id = ?")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "account row vanished mid-test: id " + accountId);
                return rs.getBigDecimal("balance");
            }
        }
    }

    private static long accountId(Connection c, String email, String type) throws SQLException {
        String sql = """
                SELECT a.id
                FROM account a
                JOIN customer cu ON cu.id = a.customer_id
                WHERE cu.email = ? AND a.type = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "seed account missing: " + email + "/" + type);
                return rs.getLong("id");
            }
        }
    }
}
