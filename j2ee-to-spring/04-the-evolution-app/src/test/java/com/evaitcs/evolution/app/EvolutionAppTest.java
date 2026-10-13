package com.evaitcs.evolution.app;

import com.evaitcs.evolution.app.era3.AccountView;
import com.evaitcs.evolution.app.era3.TransferResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The capstone's proof suite, over REAL HTTP: ONE server for the whole class
 * (@BeforeAll/@AfterAll — unlike module 01's per-test lifecycle, because this server
 * carries three frameworks' startup costs), ONE shared in-memory database, no mocks.
 * Run with:
 *   .\mvnw.cmd -pl 04-the-evolution-app test
 *
 * ORDER-INDEPENDENCE, by construction (house determinism rule — no @TestMethodOrder
 * anywhere in this repo):
 *   - every test that MOVES money asserts DELTAS (read balances before, compare after),
 *     never absolutes, so any execution order passes;
 *   - the ONE absolute assertion (seeded 1500.00) reads account 1, and no test in this
 *     class ever writes to account 1 — era writes stay on the 2 -> 3 corridor;
 *   - the refused-transfer test picks an amount (999999.00) no reachable balance covers;
 *   - surefire runs methods sequentially in one JVM, so "before" reads inside a test
 *     cannot race with another test's writes.
 */
class EvolutionAppTest {

    private static EvolutionServer server;
    private static String db;

    /** Jackson 3 (tools.jackson) — the same mapper the server side uses; records work natively. */
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @BeforeAll
    static void startWorld() {
        server = EvolutionServer.start();     // fresh H2 + Tomcat(port 0) + all three eras mounted
        db = server.dbUrl();
    }

    @AfterAll
    static void stopWorld() {
        server.close();                       // Spring context, SessionFactory, Tomcat, temp dir, ServiceLocator
    }

    // ---- era 1: servlet + ServiceLocator + hand-cranked JDBC --------------------------

    @Test
    @DisplayName("era 1: a form POST through the raw servlet moves money and returns a reference (module 01's stack, mounted)")
    void era1FormPostMovesMoneyThroughRawJdbc() {
        BigDecimal before2 = DemoDb.balanceOf(db, 2);
        BigDecimal before3 = DemoDb.balanceOf(db, 3);

        EvolutionServer.HttpResult r = server.postForm("/era1/transfer", form("2", "3", "100.00"));

        assertEquals(200, r.status());
        assertTrue(r.bodyContains("transfer OK", "reference ="), r.body());
        String reference = extractReference(r.body());
        assertNotNull(reference);
        assertEquals(before2.subtract(new BigDecimal("100.00")), DemoDb.balanceOf(db, 2));
        assertEquals(before3.add(new BigDecimal("100.00")), DemoDb.balanceOf(db, 3));
        System.out.println("era1 evidence: 2 " + before2 + " -> " + DemoDb.balanceOf(db, 2)
                + ", 3 " + before3 + " -> " + DemoDb.balanceOf(db, 3) + ", reference=" + reference);
    }

    // ---- era 2: servlet + standalone Hibernate ----------------------------------------

    @Test
    @DisplayName("era 2: the same form POST through Hibernate moves money, writes the ledger pair, and bumps @Version with no code")
    void era2FormPostMovesMoneyThroughHibernate() throws SQLException {
        BigDecimal before2 = DemoDb.balanceOf(db, 2);
        BigDecimal before3 = DemoDb.balanceOf(db, 3);
        long version2 = versionOf(2);
        long version3 = versionOf(3);

        EvolutionServer.HttpResult r = server.postForm("/era2/transfer", form("2", "3", "50.00"));

        assertEquals(200, r.status());
        assertTrue(r.bodyContains("transfer OK", "reference ="), r.body());
        String reference = extractReference(r.body());
        assertEquals(before2.subtract(new BigDecimal("50.00")), DemoDb.balanceOf(db, 2));
        assertEquals(before3.add(new BigDecimal("50.00")), DemoDb.balanceOf(db, 3));

        // The ledger discipline survived the ORM: exactly one DEBIT + one CREDIT sharing the reference.
        List<String> rows = ledgerRowsFor(reference);
        assertEquals(2, rows.size(), "exactly two ledger rows per transfer, got " + rows);
        assertTrue(rows.get(0).contains("CREDIT") && rows.get(0).contains("account 3"), rows.toString());
        assertTrue(rows.get(1).contains("DEBIT") && rows.get(1).contains("account 2"), rows.toString());

        // @Version is the era-2 teaching point in one assertion: module 01's DAO wrote
        // "version = version + 1" BY HAND in its UPDATE; here NO Java code mentioned the
        // column, and it moved anyway — Hibernate's optimistic-lock mapping did it at flush.
        assertEquals(version2 + 1, versionOf(2));
        assertEquals(version3 + 1, versionOf(3));
        System.out.println("era2 evidence: deltas -50.00/+50.00, ledger " + rows
                + ", versions " + version2 + "->" + versionOf(2) + " / " + version3 + "->" + versionOf(3)
                + " (bumped by Hibernate, not by code)");
    }

    // ---- era 3: hand-wired Spring MVC, JSON + RFC 7807 ---------------------------------

    @Test
    @DisplayName("era 3: POST JSON returns 200 application/json whose TransferResponse carries the reference and the exact new balances")
    void era3PostJsonMovesMoneyAndReturnsExactBalances() {
        BigDecimal before2 = DemoDb.balanceOf(db, 2);
        BigDecimal before3 = DemoDb.balanceOf(db, 3);

        EvolutionServer.HttpResult r = server.postJson("/api/v3/transfers",
                "{\"from\":2,\"to\":3,\"amount\":25.00}");

        assertEquals(200, r.status());
        assertTrue(r.contentTypeIs("application/json"), "content type was: " + r.contentType());
        // The record round-trip IS the assertion that Jackson 3 speaks records natively.
        TransferResponse body = JSON.readValue(r.body(), TransferResponse.class);
        assertNotNull(body.reference());
        assertEquals(0, body.newFromBalance().compareTo(before2.subtract(new BigDecimal("25.00"))),
                "newFromBalance " + body.newFromBalance() + " != " + before2 + " - 25.00");
        assertEquals(0, body.newToBalance().compareTo(before3.add(new BigDecimal("25.00"))),
                "newToBalance " + body.newToBalance() + " != " + before3 + " + 25.00");
        // The response's claimed balances ARE the database's balances (read inside the transaction).
        assertEquals(0, body.newFromBalance().compareTo(DemoDb.balanceOf(db, 2)));
        assertEquals(0, body.newToBalance().compareTo(DemoDb.balanceOf(db, 3)));
        System.out.println("era3 evidence: " + r.body() + "  [" + r.contentType() + "]");
    }

    @Test
    @DisplayName("era 3: exactly two transactions rows share the returned reference — stage-1's reconciliation rule, enforced over HTTP")
    void era3WritesExactlyTwoLedgerRowsSharingTheReference() throws SQLException {
        EvolutionServer.HttpResult r = server.postJson("/api/v3/transfers",
                "{\"from\":2,\"to\":3,\"amount\":10.00}");
        assertEquals(200, r.status());
        String reference = JSON.readValue(r.body(), TransferResponse.class).reference();

        List<String> rows = ledgerRowsFor(reference);
        assertEquals(2, rows.size(), "expected DEBIT+CREDIT pair, got " + rows);
        assertTrue(rows.get(0).startsWith("CREDIT account 3 10.00"), rows.toString());
        assertTrue(rows.get(1).startsWith("DEBIT account 2 10.00"), rows.toString());
        System.out.println("era3 ledger evidence: reference " + reference + " -> " + rows);
    }

    @Test
    @DisplayName("era 3: insufficient funds is a 409 application/problem+json whose detail names the account and both amounts")
    void era3InsufficientFundsIs409ProblemJson() {
        BigDecimal before2 = DemoDb.balanceOf(db, 2);
        BigDecimal before3 = DemoDb.balanceOf(db, 3);

        EvolutionServer.HttpResult r = server.postJson("/api/v3/transfers",
                "{\"from\":3,\"to\":2,\"amount\":999999.00}");

        assertEquals(409, r.status());
        assertTrue(r.contentTypeIs("application/problem+json"), "content type was: " + r.contentType());
        JsonNode problem = JSON.readTree(r.body());
        assertEquals(409, problem.path("status").asInt());
        assertEquals("Insufficient funds", problem.path("title").asString());
        String detail = problem.path("detail").asString();
        assertTrue(detail.contains("account 3"), detail);
        assertTrue(detail.contains("999999.00"), detail);
        assertTrue(problem.path("type").asString().contains("insufficient-funds"), r.body());
        // Rollback proof: the @Transactional proxy erased everything — balances untouched.
        assertEquals(before3, DemoDb.balanceOf(db, 3));
        assertEquals(before2, DemoDb.balanceOf(db, 2));
        System.out.println("era3 409 evidence: " + r.body());
    }

    @Test
    @DisplayName("era 3: GET on an unknown account is a 404 problem+json — the advice owns the status code, no controller does")
    void era3UnknownAccountIs404ProblemJson() {
        EvolutionServer.HttpResult r = server.get("/api/v3/accounts/999");

        assertEquals(404, r.status());
        assertTrue(r.contentTypeIs("application/problem+json"), "content type was: " + r.contentType());
        JsonNode problem = JSON.readTree(r.body());
        assertEquals(404, problem.path("status").asInt());
        assertEquals("Account not found", problem.path("title").asString());
        assertTrue(problem.path("detail").asString().contains("999"), r.body());
        System.out.println("era3 404 evidence: " + r.body());
    }

    @Test
    @DisplayName("era 3: malformed JSON is a 400 problem+json — the converter's failure becomes the advice's clean contract")
    void era3MalformedJsonIs400ProblemJson() {
        EvolutionServer.HttpResult r = server.postJson("/api/v3/transfers", "{oops: not json");

        assertEquals(400, r.status());
        assertTrue(r.contentTypeIs("application/problem+json"), "content type was: " + r.contentType());
        JsonNode problem = JSON.readTree(r.body());
        assertEquals(400, problem.path("status").asInt());
        assertEquals("Malformed JSON request", problem.path("title").asString());
        System.out.println("era3 400 evidence: " + r.body());
    }

    @Test
    @DisplayName("era 3: GET /api/v3/accounts/1 returns the seeded 1500.00 as a JSON AccountView record")
    void era3GetAccountReturnsTheSeededBalance() {
        // ORDER-INDEPENDENT ON PURPOSE: every writing test in this class moves money on
        // the 2 -> 3 corridor; account 1 is read-only here, so its seeded absolute value
        // holds no matter when this test runs.
        EvolutionServer.HttpResult r = server.get("/api/v3/accounts/1");

        assertEquals(200, r.status());
        assertTrue(r.contentTypeIs("application/json"), "content type was: " + r.contentType());
        AccountView view = JSON.readValue(r.body(), AccountView.class);
        assertEquals(1, view.id());
        assertEquals("CHECKING", view.type());
        assertEquals(0, view.balance().compareTo(DemoDb.ALICE_CHECKING),
                "expected the seeded " + DemoDb.ALICE_CHECKING + ", got " + view.balance());
        System.out.println("era3 GET evidence: " + r.body() + " (seeded 1500.00, account 1 is never written by this suite)");
    }

    // ---- the capstone claim itself ------------------------------------------------------

    @Test
    @DisplayName("all three eras agree: the same 10.00 transfer lands the same deltas through JDBC, Hibernate and Spring MVC")
    void allThreeErasAgreeOnTheSameDeltas() {
        BigDecimal amount = new BigDecimal("10.00");
        BigDecimal start2 = DemoDb.balanceOf(db, 2);
        BigDecimal start3 = DemoDb.balanceOf(db, 3);
        System.out.println("agreement run: account 2 starts at " + start2 + ", account 3 at " + start3);

        // Era 1: raw JDBC.
        server.postForm("/era1/transfer", form("2", "3", "10.00"));
        assertDelta("era1 (JDBC)", start2, start3, amount);

        // Era 2: Hibernate.
        server.postForm("/era2/transfer", form("2", "3", "10.00"));
        assertDelta("era2 (Hibernate)", start2, start3, amount.multiply(new BigDecimal("2")));

        // Era 3: Spring MVC + JdbcTemplate + @Transactional.
        EvolutionServer.HttpResult r = server.postJson("/api/v3/transfers",
                "{\"from\":2,\"to\":3,\"amount\":10.00}");
        assertEquals(200, r.status());
        assertDelta("era3 (Spring MVC)", start2, start3, amount.multiply(new BigDecimal("3")));

        System.out.println("verdict: three stacks, identical deltas — the plumbing changed, the banking did not.");
    }

    private static void assertDelta(String era, BigDecimal start2, BigDecimal start3, BigDecimal totalMoved) {
        assertEquals(0, DemoDb.balanceOf(db, 2).compareTo(start2.subtract(totalMoved)),
                era + ": account 2 should be down " + totalMoved);
        assertEquals(0, DemoDb.balanceOf(db, 3).compareTo(start3.add(totalMoved)),
                era + ": account 3 should be up " + totalMoved);
        System.out.println("  " + era + " moved its share: 2 = " + DemoDb.balanceOf(db, 2)
                + ", 3 = " + DemoDb.balanceOf(db, 3));
    }

    // ---- helpers ------------------------------------------------------------------------

    private static Map<String, String> form(String from, String to, String amount) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("from", from);
        form.put("to", to);
        form.put("amount", amount);
        return form;
    }

    private static String extractReference(String body) {
        return body.lines()
                .filter(l -> l.startsWith("reference = "))
                .map(l -> l.substring("reference = ".length()).strip())
                .findFirst().orElseThrow(() -> new AssertionError("no reference line in body: " + body));
    }

    private static long versionOf(long accountId) throws SQLException {
        try (Connection c = DemoDb.connect(db);
             var ps = c.prepareStatement("SELECT version FROM account WHERE id = ?")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "account " + accountId + " missing");
                return rs.getLong("version");
            }
        }
    }

    /** The ledger rows sharing one reference, as printable strings, CREDIT before DEBIT (ORDER BY type). */
    private static List<String> ledgerRowsFor(String reference) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (Connection c = DemoDb.connect(db);
             var ps = c.prepareStatement(
                     "SELECT account_id, type, amount FROM transactions WHERE reference = ? ORDER BY type")) {
            ps.setString(1, reference);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(rs.getString("type") + " account " + rs.getLong("account_id")
                            + " " + rs.getBigDecimal("amount"));
                }
            }
        }
        return rows;
    }
}
