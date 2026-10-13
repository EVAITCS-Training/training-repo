package com.evaitcs.evolution.j2ee;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF over REAL HTTP: embedded Tomcat on an ephemeral port, a fresh in-memory
 * database per test, no mocks anywhere. Run with:
 *   .\mvnw.cmd -pl 01-j2ee-the-world-before-spring test
 *
 * Determinism notes (house rules):
 *   - every test gets its OWN server + database (@BeforeEach) — no ordering effects;
 *   - ports are OS-assigned (port 0) — nothing collides, nothing needs freeing;
 *   - the server is stopped in @AfterEach — the JVM exits clean;
 *   - no timing is asserted; HTTP over loopback to an in-process server is reliable.
 */
class J2eeServletTest {

    private J2eeServer server;
    private String db;

    @BeforeEach
    void startWorld() {
        server = J2eeServer.start();     // fresh H2 + Tomcat on an ephemeral port
        db = server.dbUrl();
    }

    @AfterEach
    void stopWorld() {
        server.close();                  // stops Tomcat, clears the ServiceLocator
    }

    @Test
    @DisplayName("the container answers: GET /health returns 200 OK")
    void healthCheck() {
        J2eeServer.HttpResult r = server.get("/health");
        assertEquals(200, r.status());
        assertEquals("OK", r.body().strip());
        System.out.println("GET /health -> " + r.status() + " '" + r.body().strip() + "' on port " + server.port());
    }

    @Test
    @DisplayName("GET balance returns the seeded value through servlet -> locator -> DAO -> SQL")
    void getBalanceReadsSeedData() {
        J2eeServer.HttpResult r = server.get("/api/transfer?accountId=1");
        assertEquals(200, r.status());
        assertTrue(r.bodyContains("1500.00"), "Alice's seeded checking balance");
        System.out.println("GET /api/transfer?accountId=1 -> " + r.body().strip());
    }

    @Test
    @DisplayName("POST transfer moves money and writes the two-row ledger trail with one reference")
    void postTransferMovesMoneyAndWritesLedger() throws SQLException {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("from", "1");
        form.put("to", "3");
        form.put("amount", "100.00");

        J2eeServer.HttpResult r = server.postForm("/api/transfer", form);
        assertEquals(200, r.status());
        assertTrue(r.bodyContains("transfer OK", "reference ="), r.body());

        assertEquals(new BigDecimal("1400.00"), DemoDb.balanceOf(db, 1));
        assertEquals(new BigDecimal("400.00"), DemoDb.balanceOf(db, 3));

        String reference = r.body().lines()
                .filter(l -> l.startsWith("reference = "))
                .map(l -> l.substring("reference = ".length()).strip())
                .findFirst().orElseThrow();
        assertNotNull(reference);

        // The stage-1 reconciliation rule: one DEBIT + one CREDIT sharing the reference.
        try (Connection c = DemoDb.connect(db);
             var ps = c.prepareStatement(
                     "SELECT account_id, type, amount FROM transactions WHERE reference = ? ORDER BY type")) {
            ps.setString(1, reference);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                System.out.println("ledger: account " + rs.getLong(1) + " " + rs.getString(2) + " " + rs.getBigDecimal(3));
                assertEquals("CREDIT", rs.getString("type"));
                assertTrue(rs.next());
                System.out.println("ledger: account " + rs.getLong(1) + " " + rs.getString(2) + " " + rs.getBigDecimal(3));
                assertEquals("DEBIT", rs.getString("type"));
                assertTrue(!rs.next(), "exactly two ledger rows per transfer");
            }
        }
        System.out.println("balances after: acct1=" + DemoDb.balanceOf(db, 1) + ", acct3=" + DemoDb.balanceOf(db, 3)
                + ", reference=" + reference);
    }

    @Test
    @DisplayName("insufficient funds: HTTP 409, and the database is untouched (rollback proved)")
    void insufficientFundsRollsBack() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("from", "3");                 // Bob has 300.00
        form.put("to", "1");
        form.put("amount", "9999.00");

        J2eeServer.HttpResult r = server.postForm("/api/transfer", form);
        assertEquals(409, r.status(), "the servlet turns the domain exception into 409");
        assertTrue(r.bodyContains("REFUSED"), r.body());

        assertEquals(DemoDb.BOB_CHECKING, DemoDb.balanceOf(db, 3), "debit never landed");
        assertEquals(DemoDb.ALICE_CHECKING, DemoDb.balanceOf(db, 1), "credit never landed");
        System.out.println("409 body: " + r.body().strip() + "; balances unchanged: "
                + DemoDb.balanceOf(db, 3) + " / " + DemoDb.balanceOf(db, 1));
    }

    @Test
    @DisplayName("bad input is a 400: the servlet parses parameters itself (no validation framework yet)")
    void badInputReturns400() {
        Map<String, String> noAmount = new LinkedHashMap<>();
        noAmount.put("from", "1");
        noAmount.put("to", "3");
        assertEquals(400, server.postForm("/api/transfer", noAmount).status(), "missing amount");

        Map<String, String> negative = new LinkedHashMap<>();
        negative.put("from", "1");
        negative.put("to", "3");
        negative.put("amount", "-5.00");
        assertEquals(400, server.postForm("/api/transfer", negative).status(), "negative amount");

        J2eeServer.HttpResult notFound = server.get("/api/transfer?accountId=999");
        assertEquals(404, notFound.status(), "unknown account");
        System.out.println("400 on missing/negative amount, 404 on unknown account: " + notFound.body().strip());
    }

    @Test
    @DisplayName("parameters are DATA, not SQL: an injection attempt is refused as a bad number")
    void injectionAttemptIsJustData() {
        // The parameter never reaches SQL text (parseLong fails first; and the DAO uses
        // PreparedStatement bind slots anyway — advanced-java/12's lesson, enforced twice).
        // (The payload is URL-encoded because a raw space is illegal in a URI — real
        //  attacker traffic looks exactly like this on the wire.)
        String payload = java.net.URLEncoder.encode("1 OR 1=1", java.nio.charset.StandardCharsets.UTF_8);
        J2eeServer.HttpResult r = server.get("/api/transfer?accountId=" + payload);
        assertEquals(400, r.status());

        Map<String, String> form = new LinkedHashMap<>();
        form.put("from", "1; DROP TABLE account;--");
        form.put("to", "3");
        form.put("amount", "10.00");
        assertEquals(400, server.postForm("/api/transfer", form).status());

        // Proof nothing was dropped: the table still answers.
        assertEquals(200, server.get("/api/transfer?accountId=1").status());
        System.out.println("injection strings came back as 400s; account table still queryable");
    }

    @Test
    @DisplayName("the DAO itself: a refused transfer leaves every balance exactly as seeded")
    void daoRollbackIsTotal() {
        JdbcTransferDao dao = new JdbcTransferDao(db);

        assertThrows(JdbcTransferDao.InsufficientFundsException.class,
                () -> dao.transfer(3, 1, new BigDecimal("300.01")));   // one cent too much

        assertEquals(DemoDb.BOB_CHECKING, DemoDb.balanceOf(db, 3));
        assertEquals(DemoDb.ALICE_CHECKING, DemoDb.balanceOf(db, 1));
        System.out.println("300.01 > 300.00 refused; balances still "
                + DemoDb.balanceOf(db, 3) + " / " + DemoDb.balanceOf(db, 1));
    }

    @Test
    @DisplayName("the ServiceLocator anti-pattern, demonstrated: unknown key = runtime explosion")
    void serviceLocatorFailsAtRuntime() {
        ServiceLocator.register("transferDao", new JdbcTransferDao(db));
        assertNotNull(ServiceLocator.lookup("transferDao"));

        // Defect #1 of the pattern: a typo'd key compiles fine and dies at RUNTIME.
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> ServiceLocator.lookup("transferDAO"));   // capital-O typo
        System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        System.out.println("Spring's context.getBean(...) with constructor injection fails at STARTUP instead (module 03).");
    }
}
