package com.evaitcs.evolution.spring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF: every claim the demos print is asserted here against a real context and
 * a real (in-memory) database — no mocks anywhere. Run with:
 *   .\mvnw.cmd -pl 03-spring-framework-core test
 *
 * Determinism notes (house rules):
 *   - every test method gets its OWN fresh DemoDb + OWN SpringApp context (@BeforeEach),
 *     closed in @AfterEach — no ordering effects, nothing shared but the static evidence
 *     lists, which are cleared before AND after every method;
 *   - no server, no ports in this module: the container is a library;
 *   - no timing is asserted — only identities, counts, balances and ledger rows.
 */
class SpringCoreTest {

    private String db;
    private AnnotationConfigApplicationContext ctx;

    @BeforeEach
    void startWorld() {
        AuditAspect.clearTrail();
        TransferEventListener.clearReceived();
        LifecycleProbe.resetFlags();
        db = DemoDb.freshDatabase();
        ctx = SpringApp.start(db);
    }

    @AfterEach
    void stopWorld() {
        ctx.close();                     // fires @PreDestroy, releases the singleton Map
        AuditAspect.clearTrail();
        TransferEventListener.clearReceived();
    }

    // ---- 1. the container: scopes and lifecycle ---------------------------------------------

    @Test
    @DisplayName("singleton scope: getBean twice returns the SAME cached instance (==)")
    void singletonBeanIsOneInstancePerContext() {
        TransferService first = ctx.getBean(TransferService.class);
        TransferService second = ctx.getBean(TransferService.class);
        assertSame(first, second, "the singleton cache must return one shared instance");
        assertSame(ctx.getBean("transferService"), first, "by-name and by-type lookups hit the same cache entry");
        System.out.println("singleton: both lookups -> " + first.getClass().getSimpleName()
                + " @" + Integer.toHexString(System.identityHashCode(first)));
    }

    @Test
    @DisplayName("prototype scope: every getBean mints a FRESH instance (@Scope(\"prototype\"))")
    void prototypeBeanIsFreshEveryLookup() {
        Receipt first = ctx.getBean(Receipt.class);
        Receipt second = ctx.getBean(Receipt.class);
        assertNotSame(first, second, "prototype = factory, not cache");
        assertEquals(first.id().substring(0, 2), "R-");
        assertTrue(!first.id().equals(second.id()), "each instance carries its own fresh id");
        System.out.println("prototype: " + first.id() + " vs " + second.id() + " — distinct instances");
    }

    @Test
    @DisplayName("lifecycle: @PostConstruct fired during refresh, @PreDestroy fires on close()")
    void lifecycleCallbacksFire() {
        assertTrue(LifecycleProbe.postConstructRan, "@PostConstruct must run at startup, after injection");
        ctx.close();                     // close is idempotent — @AfterEach's second close is a no-op
        assertTrue(LifecycleProbe.preDestroyRan, "@PreDestroy must run when the context closes");
        System.out.println("lifecycle: @PostConstruct ran = true, @PreDestroy ran = true (jakarta.annotation callbacks)");
    }

    // ---- 2. the wiring eras expose one graph --------------------------------------------------

    @Test
    @DisplayName("XML (2004) and annotation (today) contexts expose the SAME bean types")
    void xmlAndAnnotationContextsExposeTheSameGraph() {
        try (ClassPathXmlApplicationContext xml = SpringApp.startXml(db)) {
            assertEquals(DriverManagerDataSource.class, xml.getBean("dataSource").getClass());
            assertEquals(xml.getBean("dataSource").getClass(), ctx.getBean("dataSource").getClass(),
                    "same class from both wiring languages");
            assertEquals(JdbcTransferRepository.class, xml.getBean("transferRepository").getClass());
            assertEquals(xml.getBean("transferRepository").getClass(),
                    ctx.getBean("transferRepository").getClass(),
                    "the XML <constructor-arg> and the @Bean parameter build the same injected object");
            System.out.println("wiring eras: beans.xml and AppConfig both yield "
                    + xml.getBean("dataSource").getClass().getSimpleName() + " + "
                    + xml.getBean("transferRepository").getClass().getSimpleName());
        }
    }

    @Test
    @DisplayName("an unsatisfied dependency aborts context STARTUP (BeanCreationException), unlike the ServiceLocator's runtime failure")
    void unsatisfiedDependencyAbortsStartup() {
        // Module 01's ServiceLocator compiled, deployed and failed at 2 a.m. on first lookup.
        // The container refuses to start at all: the failure happens inside refresh().
        BeanCreationException e = assertThrows(BeanCreationException.class,
                () -> new AnnotationConfigApplicationContext(BrokenTestConfig.class));
        Throwable root = rootCause(e);
        System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ", root: "
                + root.getClass().getSimpleName() + ": " + firstLine(root.getMessage()));
        assertTrue(root.getMessage().contains("PdfRenderer"),
                "the root cause must NAME the missing dependency type");
    }

    // ---- 3. JdbcTemplate: reads and exception translation -------------------------------------

    @Test
    @DisplayName("the JdbcTemplate repository reads every seeded balance EXACTLY (scale 2)")
    void jdbcTemplateReadsSeededBalancesExactly() {
        JdbcTransferRepository repository = ctx.getBean(JdbcTransferRepository.class);
        assertEquals(DemoDb.ALICE_CHECKING, repository.balanceOf(1));
        assertEquals(DemoDb.ALICE_SAVINGS, repository.balanceOf(2));
        assertEquals(DemoDb.BOB_CHECKING, repository.balanceOf(3));
        System.out.println("seeded reads: 1=" + repository.balanceOf(1) + ", 2=" + repository.balanceOf(2)
                + ", 3=" + repository.balanceOf(3) + " — byte-identical to module 01's DAO reads");
    }

    @Test
    @DisplayName("a duplicate email surfaces as DuplicateKeyException — unchecked, typed, SQLState 23505 underneath")
    void duplicateEmailBecomesTypedDuplicateKeyException() {
        JdbcTransferRepository repository = ctx.getBean(JdbcTransferRepository.class);
        DuplicateKeyException e = assertThrows(DuplicateKeyException.class,
                () -> repository.addCustomer("Impostor Alice", "alice@securebank.dev"));
        assertTrue(e.getCause() instanceof SQLException, "the vendor exception is kept as the cause");
        SQLException sql = (SQLException) e.getCause();
        assertEquals("23505", sql.getSQLState(), "H2 reports the unique violation as SQLState 23505");
        System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + " (SQLState " + sql.getSQLState() + "):");
        System.out.println("  the translator read the vendor code and threw MEANING — callers never touch SQLState.");
    }

    // ---- 4. @Transactional: commit, rollback, and the trap -------------------------------------

    @Test
    @DisplayName("@Transactional commit: balances move and exactly two ledger rows share the reference")
    void transactionalTransferCommitsWithLedgerPair() throws SQLException {
        TransferService service = ctx.getBean(TransferService.class);

        String reference = service.transfer(1, 3, new BigDecimal("100.00"));

        assertEquals(new BigDecimal("1400.00"), DemoDb.balanceOf(db, 1), "1500.00 - 100.00 committed");
        assertEquals(new BigDecimal("400.00"), DemoDb.balanceOf(db, 3), "300.00 + 100.00 committed");

        // Stage-1's reconciliation rule, asserted straight from SQL: one DEBIT + one CREDIT,
        // same reference, same amount — and NOTHING else wearing that reference.
        try (Connection c = DemoDb.connect(db);
             var ps = c.prepareStatement(
                     "SELECT account_id, type, amount FROM transactions WHERE reference = ? ORDER BY type")) {
            ps.setString(1, reference);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                System.out.println("ledger: account " + rs.getLong(1) + " " + rs.getString(2) + " " + rs.getBigDecimal(3));
                assertEquals("CREDIT", rs.getString("type"));
                assertEquals(3L, rs.getLong("account_id"));
                assertTrue(rs.next());
                System.out.println("ledger: account " + rs.getLong(1) + " " + rs.getString(2) + " " + rs.getBigDecimal(3));
                assertEquals("DEBIT", rs.getString("type"));
                assertEquals(1L, rs.getLong("account_id"));
                assertTrue(!rs.next(), "exactly two ledger rows per transfer");
            }
        }
        System.out.println("committed: 1400.00 / 400.00 with ledger pair " + reference);
    }

    @Test
    @DisplayName("@Transactional rollback: one cent over the balance is refused and NOTHING lands")
    void insufficientFundsRollsBackEverything() {
        TransferService service = ctx.getBean(TransferService.class);
        long ledgerRowsBefore = countLedgerRows();

        assertThrows(TransferService.InsufficientFundsException.class,
                () -> service.transfer(3, 1, new BigDecimal("300.01")));   // Bob has 300.00

        assertEquals(DemoDb.BOB_CHECKING, DemoDb.balanceOf(db, 3), "the debit never survived the rollback");
        assertEquals(DemoDb.ALICE_CHECKING, DemoDb.balanceOf(db, 1), "the credit never existed");
        assertEquals(ledgerRowsBefore, countLedgerRows(), "zero new ledger rows — the rollback was total");
        System.out.println("rolled back: balances still " + DemoDb.balanceOf(db, 3) + " / " + DemoDb.balanceOf(db, 1)
                + ", ledger rows " + ledgerRowsBefore + " -> " + countLedgerRows());
    }

    @Test
    @DisplayName("the famous trap: a CHECKED exception does NOT roll back by default — the write COMMITS")
    void checkedExceptionDoesNotRollBackByDefault() {
        TransferService service = ctx.getBean(TransferService.class);

        Exception e = assertThrows(Exception.class,
                () -> service.debitThenCheckedFailure(3, new BigDecimal("25.00")));
        assertTrue(e.getMessage().contains("checked"), "our marker message proves which path ran");

        assertEquals(new BigDecimal("275.00"), DemoDb.balanceOf(db, 3),
                "the debit COMMITTED: default rule rolls back RuntimeException/Error only");
        System.out.println("CAUGHT -> Exception (checked): balance 300.00 -> " + DemoDb.balanceOf(db, 3)
                + " — committed despite the throw. Fix: rollbackFor = Exception.class, or throw unchecked.");
    }

    // ---- 5. AOP: the proxy, the advice, the self-invocation trap --------------------------------

    @Test
    @DisplayName("the bean is a CGLIB proxy; advice fires exactly once externally and ZERO times on self-invocation")
    void adviceFiresExternallyButNotOnSelfInvocation() {
        TransferService service = ctx.getBean(TransferService.class);

        // The proxy, proven: same mechanism carries @Transactional and our @Around advice.
        assertTrue(AopUtils.isAopProxy(service), "the container must hand out the proxy, not the raw target");
        assertTrue(service.getClass().getName().contains("$$SpringCGLIB$$"),
                "CGLIB subclass marker expected, was: " + service.getClass().getName());
        System.out.println("proxy: " + service.getClass().getName());

        AuditAspect.clearTrail();
        service.transfer(1, 3, new BigDecimal("10.00"));                  // THROUGH the proxy
        assertEquals(1, AuditAspect.count("before", "transfer"));
        assertEquals(1, AuditAspect.count("after", "transfer"));
        System.out.println("external call: advice fired before=1 after=1 around transfer()");

        AuditAspect.clearTrail();
        service.selfInvokedTransfer(1, 3, new BigDecimal("10.00"));       // outer: through the proxy...
        assertEquals(1, AuditAspect.count("before", "selfInvokedTransfer"),
                "the OUTER call still passes through the proxy");
        assertEquals(0, AuditAspect.count("before", "transfer"),
                "the INNER this.transfer(...) never touches the proxy — no advice, no transaction");
        assertEquals(0, AuditAspect.count("after", "transfer"));
        System.out.println("self-invocation: selfInvokedTransfer advised (before=1), inner transfer() NOT (before=0)");
        System.out.println("  -> same trap for @Transactional/@Async in stage-2: 'this' is the raw target.");
    }

    // ---- 6. events -----------------------------------------------------------------------------

    @Test
    @DisplayName("the @EventListener hears a committed transfer — and nothing after a refused one")
    void listenerHearsSuccessAndNotFailure() {
        TransferService service = ctx.getBean(TransferService.class);

        TransferEventListener.clearReceived();
        String reference = service.transfer(1, 3, new BigDecimal("50.00"));
        assertEquals(1, TransferEventListener.RECEIVED.size(), "exactly one event per successful transfer");
        TransferExecutedEvent event = TransferEventListener.RECEIVED.get(0);
        assertEquals(reference, event.reference());
        assertEquals(new BigDecimal("50.00"), event.amount());
        assertEquals(1L, event.fromAccountId());
        assertEquals(3L, event.toAccountId());
        System.out.println("event received: " + event.amount().toPlainString() + " from " + event.fromAccountId()
                + " to " + event.toAccountId() + ", reference " + reference);

        TransferEventListener.clearReceived();
        assertThrows(TransferService.InsufficientFundsException.class,
                () -> service.transfer(3, 1, new BigDecimal("99999.00")));
        assertEquals(0, TransferEventListener.RECEIVED.size(),
                "the throw precedes the publish — no event for work that never happened");
        System.out.println("refused transfer: 0 events — the success path is the only publisher");
    }

    // ---- exhibits and helpers -------------------------------------------------------------------

    /** The test-local broken graph: MonthlyStatement needs a PdfRenderer nobody provides. */
    public interface PdfRenderer {
    }

    public record MonthlyStatement(PdfRenderer renderer) {
    }

    @Configuration
    static class BrokenTestConfig {
        @Bean
        MonthlyStatement monthlyStatement(PdfRenderer renderer) {   // unsatisfiable parameter
            return new MonthlyStatement(renderer);
        }
    }

    private long countLedgerRows() {
        try (Connection c = DemoDb.connect(db);
             var ps = c.prepareStatement("SELECT COUNT(*) FROM transactions");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Throwable rootCause(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private static String firstLine(String message) {
        return message == null ? "" : message.lines().findFirst().orElse("");
    }
}
