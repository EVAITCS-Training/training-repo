package com.evaitcs.advanced.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 11-exceptions test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC:
 *   - a caught exception, its stack-trace method order, its cause, and its suppressed array
 *     are fixed facts about a chosen throw, identical on every run;
 *   - reverse close order and primary-vs-suppressed are guaranteed by the try-with-resources
 *     spec (module 08);
 *   - exhaustiveness of the sealed switch is a compile-time fact we can also read at runtime
 *     via getPermittedSubclasses();
 *   - the CONTROL-FLOW test prints nanoTime for both approaches but asserts ONLY that they
 *     produce the same answers — never which is faster (repo determinism rule: timings are
 *     printed, not asserted).
 */
class ExceptionsTest {

    @Test
    @DisplayName("stack unwinding: the caught exception lists checkBalance, authorize, transfer — deepest frame first")
    void stackUnwindingPopsFramesInOrder() {
        Account from = new Account("ACC-1001", new BigDecimal("50.00"));
        Account to = new Account("ACC-2002", new BigDecimal("0.00"));

        BankingError.InsufficientFunds e = assertThrows(BankingError.InsufficientFunds.class,
                () -> new TransferService().transfer(from, to, new BigDecimal("80.00")));   // 80 > 50: throws deep inside

        StackTraceElement[] trace = e.getStackTrace();
        // The throw happened in checkBalance (deepest); unwinding pops up through authorize, then transfer.
        assertEquals("checkBalance", trace[0].getMethodName(), "frame 0 is the throw site, deepest");
        assertEquals("authorize", trace[1].getMethodName(), "frame 1 called checkBalance");
        assertEquals("transfer", trace[2].getMethodName(), "frame 2 called authorize");

        System.out.println("unwind path (deepest first): " + trace[0].getMethodName()
                + " -> " + trace[1].getMethodName() + " -> " + trace[2].getMethodName()
                + " -> (catch above here); full trace length = " + trace.length);
    }

    @Test
    @DisplayName("cause chain: wrapping WITH the cause preserves the origin — getCause() walks back to the same object")
    void causeChainIsPreserved() {
        // Realistic path: processPayment catches a low-level IOException and rethrows WITH it as the cause.
        RuntimeException caught = assertThrows(RuntimeException.class, CauseAndRethrowDemo::processPayment);
        assertEquals("payment could not be completed", caught.getMessage());
        assertNotNull(caught.getCause(), "the origin is attached, not swallowed");
        assertSame(IOException.class, caught.getCause().getClass(), "the cause is the low-level IOException");
        assertEquals("card gateway timed out", caught.getCause().getMessage());

        // Identity: an explicit chain keeps the ROOT as the very same object getCause() returns.
        IOException root = new IOException("gateway down");
        RuntimeException wrapped = new RuntimeException("high-level context", root);
        assertSame(root, wrapped.getCause(), "getCause() returns the identical root object");

        System.out.println("chain: " + caught.getClass().getSimpleName() + " -> "
                + caught.getCause().getClass().getSimpleName() + ": " + caught.getCause().getMessage());
    }

    @Test
    @DisplayName("swallowing the cause: rethrowing WITHOUT it makes getCause() null — the origin is lost")
    void swallowingTheCauseLosesTheOrigin() {
        RuntimeException caught = assertThrows(RuntimeException.class, CauseAndRethrowDemo::processPaymentBadly);
        assertEquals("payment could not be completed", caught.getMessage());
        assertNull(caught.getCause(), "no cause attached — the IOException origin was thrown away");

        System.out.println("CAUGHT -> " + caught.getClass().getSimpleName() + ": " + caught.getMessage()
                + "; getCause() = " + caught.getCause());
        System.out.println("the low-level 'card gateway timed out' is GONE — this is the swallow trap");
    }

    @Test
    @DisplayName("suppression: when the body and close() both throw, the body is primary and close() is the one suppressed")
    void tryWithResourcesSuppressesTheCloseException() {
        List<String> log = new ArrayList<>();

        IllegalStateException primary = assertThrows(IllegalStateException.class, () -> {
            try (SuppressionDemo.Ledger ledger = new SuppressionDemo.Ledger("R", log, true)) {   // close() will throw
                throw new IllegalStateException("primary body failure");                        // body throws first
            }
        });

        assertEquals("primary body failure", primary.getMessage(), "the body's exception is the primary");
        Throwable[] suppressed = primary.getSuppressed();
        assertEquals(1, suppressed.length, "exactly one suppressed exception: the failing close()");
        assertSame(IOException.class, suppressed[0].getClass(), "the suppressed one is the close()'s IOException");
        assertTrue(suppressed[0].getMessage().contains("R"), "the suppressed message names the resource");

        System.out.println("primary = " + primary.getClass().getSimpleName() + ": " + primary.getMessage());
        System.out.println("suppressed[0] = " + suppressed[0].getClass().getSimpleName() + ": " + suppressed[0].getMessage());
    }

    @Test
    @DisplayName("try-with-resources closes resources in REVERSE order of declaration")
    void resourcesCloseInReverseOrderOfDeclaration() throws Exception {
        List<String> order = new ArrayList<>();
        try (SuppressionDemo.Ledger a = new SuppressionDemo.Ledger("A", order, false);
             SuppressionDemo.Ledger b = new SuppressionDemo.Ledger("B", order, false)) {
            System.out.println("opened A then B (declaration order)");
        }
        assertEquals(List.of("B", "A"), order, "declared A then B; closed B then A — reverse");
        System.out.println("recorded close order = " + order + " (reverse of declaration A, B)");
    }

    @Test
    @DisplayName("multi-catch: one catch (IOException | ConfigException e) handles two different throw sites")
    void multiCatchHandlesBoth() {
        List<String> caughtTypes = new ArrayList<>();
        for (boolean failIo : new boolean[] { true, false }) {
            try {
                CauseAndRethrowDemo.loadAndParse(failIo);       // throws IOException, else ConfigException
            } catch (IOException | CauseAndRethrowDemo.ConfigException e) {
                caughtTypes.add(e.getClass().getSimpleName());  // ONE handler, whichever type arrived
            }
        }
        assertEquals(List.of("IOException", "ConfigException"), caughtTypes, "the single handler caught both");
        System.out.println("one multi-catch handler caught: " + caughtTypes);
    }

    @Test
    @DisplayName("sealed hierarchy: a pattern-matching switch returns a message for every permitted BankingError, no default")
    void sealedHierarchyIsExhaustive() {
        BankingError insufficient = new BankingError.InsufficientFunds(
                new BankingError.TransferContext("ACC-1", new BigDecimal("80.00"), new BigDecimal("50.00")));
        BankingError frozen = new BankingError.AccountFrozen("ACC-2", "reported lost");
        BankingError limit = new BankingError.DailyLimitExceeded("ACC-3", new BigDecimal("5000.00"), new BigDecimal("7500.00"));

        // customerMessage() switches over the sealed family with NO default branch — javac proved it exhaustive.
        assertTrue(BankingErrorsDemo.customerMessage(insufficient).contains("short by 30.00"));
        assertTrue(BankingErrorsDemo.customerMessage(frozen).contains("frozen"));
        assertTrue(BankingErrorsDemo.customerMessage(limit).contains("daily limit"));

        assertEquals(3, BankingError.class.getPermittedSubclasses().length, "the family is closed: exactly three permitted subtypes");
        System.out.println("each permitted case produced a customer message; add a 4th permit and customerMessage() would NOT COMPILE");
    }

    @Test
    @DisplayName("typed context: InsufficientFunds exposes attempted/available BigDecimals, so the shortfall needs no string parsing")
    void typedContextBeatsStringParsing() {
        BankingError.InsufficientFunds e = new BankingError.InsufficientFunds(
                new BankingError.TransferContext("ACC-1", new BigDecimal("80.00"), new BigDecimal("50.00")));

        // Read the TYPED fields directly — no regex, no substring, no parsing of getMessage().
        assertEquals(0, new BigDecimal("80.00").compareTo(e.context().attempted()));
        assertEquals(0, new BigDecimal("50.00").compareTo(e.context().available()));

        BigDecimal shortfall = e.context().shortfall();
        assertEquals(0, new BigDecimal("30.00").compareTo(shortfall), "shortfall computed from fields, not parsed from the message");

        System.out.println("attempted=" + e.context().attempted() + " available=" + e.context().available()
                + " shortfall=" + shortfall + " — all read as BigDecimals, nothing parsed");
    }

    @Test
    @DisplayName("helpful NPE (JEP 358): dereferencing null yields a detailed message that names the null reference")
    void helpfulNpeMessageNamesTheNull() {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> {
            Account missing = null;
            missing.id();   // helpful NPE: Cannot invoke "Account.id()" because "missing" is null
        });

        String msg = npe.getMessage();
        assertNotNull(msg, "a helpful NPE carries a message (pre-JEP-358 it was null)");
        assertTrue(msg.contains("null"), "the message names the null value");
        assertTrue(msg.contains("invoke") || msg.contains("because"), "the message names the failed operation");

        System.out.println("helpful NPE message = " + msg);
    }

    @Test
    @DisplayName("control flow: parseInt-by-exception and parseInt-by-predicate give the SAME answers (timings printed, never asserted)")
    void exceptionForControlFlowIsCostlierThanPredicate() {
        String[] samples = { "42", "abc", "7", "12x", "1000", "", "-5", "99" };
        int iterations = 100_000;
        int fallback = -1;

        long totalByException = 0;
        long startEx = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (String s : samples) {
                totalByException += ExceptionCostDemo.parseIntByException(s, fallback);
            }
        }
        long exMillis = (System.nanoTime() - startEx) / 1_000_000;

        long totalByPredicate = 0;
        long startPred = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (String s : samples) {
                totalByPredicate += ExceptionCostDemo.parseIntByPredicate(s, fallback);
            }
        }
        long predMillis = (System.nanoTime() - startPred) / 1_000_000;

        // The ONLY assertion: both approaches are correct. We NEVER assert which was faster.
        assertEquals(totalByException, totalByPredicate, "both produce the same total over every input");
        System.out.println("exception path = " + exMillis + " ms, predicate path = " + predMillis
                + " ms, equal totals = " + (totalByException == totalByPredicate)
                + " (timings printed for evidence, NEVER asserted)");
    }
}
