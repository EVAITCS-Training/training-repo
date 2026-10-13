package com.evaitcs.advanced.gof;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 09-design-patterns-gof test
 * Each test proves ONE pattern claim and prints its evidence, so the test run
 * doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC: single-threaded, exact BigDecimals at
 * scale 2, exact event orders (ArrayList iteration = registration order), and the
 * enum-singleton guarantees, which are facts about the JVM, not about timing.
 */
class GofPatternTest {

    @Test
    @DisplayName("strategy: swapping the FeePolicy changes the fee exactly, calculator untouched")
    void strategySwapChangesTheFeeExactly() {
        FeeStrategyDemo.FeeCalculator calculator = new FeeStrategyDemo.FeeCalculator();
        BigDecimal amount = new BigDecimal("250.00");

        BigDecimal flat = calculator.feeFor(new FeeStrategyDemo.FlatFee(new BigDecimal("2.50")), amount);
        BigDecimal percent = calculator.feeFor(new FeeStrategyDemo.PercentFee(new BigDecimal("0.01")), amount);
        FeeStrategyDemo.FreeOverThreshold free =
                new FeeStrategyDemo.FreeOverThreshold(new BigDecimal("200.00"), new BigDecimal("1.00"));
        BigDecimal waived = calculator.feeFor(free, amount);
        BigDecimal below = calculator.feeFor(free, new BigDecimal("150.00"));

        assertEquals(new BigDecimal("2.50"), flat, "flat fee is the constant itself, scale 2");
        assertEquals(new BigDecimal("2.50"), percent, "250.00 * 0.01 = 2.5000, rounded HALF_UP to scale 2");
        assertEquals(new BigDecimal("0.00"), waived, "250.00 clears the 200.00 threshold, so free");
        assertEquals(new BigDecimal("1.00"), below, "150.00 is below the threshold, so the fallback applies");

        System.out.println("one calculator, three strategies on 250.00 -> "
                + flat.toPlainString() + " / " + percent.toPlainString() + " / " + waived.toPlainString()
                + "; below threshold on 150.00 -> " + below.toPlainString());
    }

    @Test
    @DisplayName("factory: from(String) returns the right sealed subtype and rejects unknown types loudly")
    void factoryReturnsTheRightSealedSubtypeAndRejectsUnknown() {
        BigDecimal amount = new BigDecimal("86.40");

        PaymentFactoryDemo.Payment card = PaymentFactoryDemo.PaymentFactory.from("card", amount);
        PaymentFactoryDemo.Payment wire = PaymentFactoryDemo.PaymentFactory.from("wire", amount);
        PaymentFactoryDemo.Payment debit = PaymentFactoryDemo.PaymentFactory.from("direct-debit", amount);

        assertInstanceOf(PaymentFactoryDemo.CardPayment.class, card);
        assertInstanceOf(PaymentFactoryDemo.WirePayment.class, wire);
        assertInstanceOf(PaymentFactoryDemo.DirectDebitPayment.class, debit);
        assertEquals("card ending 4242 for 86.40", PaymentFactoryDemo.PaymentFactory.describe(card),
                "the sealed switch describes every subtype without a default branch");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PaymentFactoryDemo.PaymentFactory.from("crypto", amount));
        assertTrue(e.getMessage().contains("crypto"), "the rejection names the offending type");

        System.out.println("from(card|wire|direct-debit) -> "
                + card.getClass().getSimpleName() + ", " + wire.getClass().getSimpleName() + ", "
                + debit.getClass().getSimpleName());
        System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    @Test
    @DisplayName("builder: build() names every missing required field, and a built application is exact")
    void builderEnforcesRequiredFieldsAndBuildsCorrectly() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> LoanBuilderDemo.LoanApplication.builder().purpose("PERSONAL").build());
        assertTrue(e.getMessage().contains("applicantId"), "names the first missing field");
        assertTrue(e.getMessage().contains("amount"), "names the second missing field");
        assertTrue(e.getMessage().contains("termMonths"), "names the third missing field — all at once");

        LoanBuilderDemo.LoanApplication loan = LoanBuilderDemo.LoanApplication.builder()
                .applicantId("CUST-77")
                .amount(new BigDecimal("25000.00"))
                .termMonths(24)
                .build();

        assertEquals("CUST-77", loan.applicantId());
        assertEquals(new BigDecimal("25000.00"), loan.amount());
        assertEquals(24, loan.termMonths());
        assertEquals("PERSONAL", loan.purpose(), "optional slot kept its default");
        assertEquals(new BigDecimal("6.50"), loan.annualRate(), "optional slot kept its default");

        // Defense in depth: even bypassing the Builder, the record validates itself.
        assertThrows(NullPointerException.class,
                () -> new LoanBuilderDemo.LoanApplication(null, BigDecimal.ONE, 12, "X", BigDecimal.ONE));

        System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        System.out.println("built = " + loan);
    }

    @Test
    @DisplayName("template method: both reports record the SAME skeleton order around different slots")
    void templateMethodRunsTheSkeletonInExactOrder() {
        ReportTemplateDemo.TaxReport tax =
                new ReportTemplateDemo.TaxReport(new BigDecimal("12.50"), new BigDecimal("2.50"));
        List<String> taxSteps = tax.generate();

        assertEquals(List.of("generate:start", "header", "tax:title",
                "rows", "tax:row:interest", "tax:row:withheld",
                "footer", "tax:total", "generate:end"), taxSteps);
        assertEquals(List.of("SecureBank Tax Report 2025",
                "Interest income: 12.50", "Tax withheld: 2.50", "Net owed: 10.00"), tax.lines());

        ReportTemplateDemo.StatementReport statement = new ReportTemplateDemo.StatementReport("ACC-1", List.of(
                new ReportTemplateDemo.StatementRow("TXN-1", "Payroll", new BigDecimal("2450.00")),
                new ReportTemplateDemo.StatementRow("TXN-2", "Rent", new BigDecimal("-1200.00"))));
        List<String> statementSteps = statement.generate();

        assertEquals(List.of("generate:start", "header", "statement:title",
                "rows", "statement:row:TXN-1", "statement:row:TXN-2",
                "footer", "statement:total", "generate:end"), statementSteps);
        assertEquals("Closing balance: 1250.00", statement.lines().get(3), "2450.00 - 1200.00, exact");

        System.out.println("tax steps       = " + taxSteps);
        System.out.println("statement steps = " + statementSteps);
        System.out.println("both frames read generate:start/header/rows/footer/generate:end — the final method owns the order");
    }

    @Test
    @DisplayName("decorator: the delegated result and balances are unchanged, and exactly one audit line is added")
    void decoratorDelegatesUnchangedAndAppendsAudit() {
        AuditDecoratorDemo.BaseTransferService bare = new AuditDecoratorDemo.BaseTransferService();
        String bareReference = bare.transfer("ACC-1", "ACC-2", new BigDecimal("250.00"));

        List<String> auditLog = new ArrayList<>();
        AuditDecoratorDemo.BaseTransferService worker = new AuditDecoratorDemo.BaseTransferService();
        AuditDecoratorDemo.TransferService audited = new AuditDecoratorDemo.AuditDecorator(worker, auditLog);
        String auditedReference = audited.transfer("ACC-1", "ACC-2", new BigDecimal("250.00"));

        assertEquals(bareReference, auditedReference, "same work, same reference — the decorator altered nothing");
        assertEquals(new BigDecimal("250.00"), worker.balanceOf("ACC-1"), "500.00 - 250.00");
        assertEquals(new BigDecimal("350.00"), worker.balanceOf("ACC-2"), "100.00 + 250.00");
        assertEquals(List.of("AUDIT TXN-1 ACC-1 -> ACC-2 250.00"), auditLog, "exactly one appended line");

        System.out.println("bare = " + bareReference + ", audited = " + auditedReference + " (identical)");
        System.out.println("audit log = " + auditLog);
    }

    @Test
    @DisplayName("observer: listeners receive exact events in order, and a deregistered listener receives none")
    void observerNotifiesInOrderAndStopsAfterDeregister() {
        BalanceObserverDemo.Account account = new BalanceObserverDemo.Account("ACC-1", new BigDecimal("100.00"));
        List<BalanceObserverDemo.BalanceEvent> printer = new ArrayList<>();
        List<BalanceObserverDemo.BalanceEvent> watcher = new ArrayList<>();

        // Hold the listener references — removing a FRESH method reference would
        // remove nothing (demo banner 3 shows that trap live).
        BalanceObserverDemo.BalanceListener printerListener = printer::add;
        BalanceObserverDemo.BalanceListener watcherListener = watcher::add;
        account.addListener(printerListener);
        account.addListener(watcherListener);

        account.deposit(new BigDecimal("50.00"));
        account.deposit(new BigDecimal("25.00"));

        List<BalanceObserverDemo.BalanceEvent> expected = List.of(
                new BalanceObserverDemo.BalanceEvent("ACC-1", new BigDecimal("150.00")),
                new BalanceObserverDemo.BalanceEvent("ACC-1", new BigDecimal("175.00")));
        assertEquals(expected, printer, "registration-order, deposit-order, exact balances");
        assertEquals(expected, watcher);

        account.removeListener(watcherListener);
        account.deposit(new BigDecimal("5.00"));

        assertEquals(3, printer.size(), "still subscribed: saw the third deposit");
        assertEquals(2, watcher.size(), "deregistered: the third deposit never arrived");
        assertEquals(new BalanceObserverDemo.BalanceEvent("ACC-1", new BigDecimal("180.00")),
                printer.get(2));

        System.out.println("printer saw " + printer);
        System.out.println("watcher saw " + watcher + " and then went silent after removeListener");
    }

    @Test
    @DisplayName("adapter: every legacy pipe-row becomes an exactly equal typed entry")
    void adapterMapsLegacyRowsExactly() {
        LegacyLedgerAdapterDemo.LegacyLedger ledger = new LegacyLedgerAdapterDemo.LegacyLedger(List.of(
                "2026-01-05|Payroll|2450.00",
                "2026-01-07|Rent|-1200.00",
                "2026-01-09|Interest|12.50"));

        List<LegacyLedgerAdapterDemo.StatementEntry> entries =
                new LegacyLedgerAdapterDemo.LedgerAdapter(ledger).entries();

        assertEquals(List.of(
                new LegacyLedgerAdapterDemo.StatementEntry(LocalDate.of(2026, 1, 5), "Payroll", new BigDecimal("2450.00")),
                new LegacyLedgerAdapterDemo.StatementEntry(LocalDate.of(2026, 1, 7), "Rent", new BigDecimal("-1200.00")),
                new LegacyLedgerAdapterDemo.StatementEntry(LocalDate.of(2026, 1, 9), "Interest", new BigDecimal("12.50"))),
                entries, "record equality: parsed dates, descriptions and scale-2 amounts");
        assertEquals(new BigDecimal("2462.50"), LegacyLedgerAdapterDemo.totalCredits(
                new LegacyLedgerAdapterDemo.LedgerAdapter(ledger)), "2450.00 + 12.50, credits only");

        System.out.println("legacy rows   = " + ledger.rows());
        System.out.println("typed entries = " + entries);
    }

    @Test
    @DisplayName("singleton: the enum constant is one instance, and neither reflection nor serialization can forge another")
    void enumSingletonIsOneInstanceAndCannotBeForged() {
        assertSame(EnumSingletonDemo.FeeSchedule.INSTANCE, EnumSingletonDemo.FeeSchedule.valueOf("INSTANCE"),
                "both lookup paths end at the one JVM-initialized constant");

        // Reflection loses: Constructor.newInstance special-cases enums (demo banner 2).
        IllegalArgumentException reflected = assertThrows(IllegalArgumentException.class, () -> {
            Constructor<EnumSingletonDemo.FeeSchedule> c = EnumSingletonDemo.FeeSchedule.class
                    .getDeclaredConstructor(String.class, int.class);
            c.setAccessible(true);
            c.newInstance("FAKE", 3);
        });
        assertTrue(reflected.getMessage().contains("Cannot reflectively create enum objects"));

        System.out.println("CAUGHT -> " + reflected.getClass().getSimpleName() + ": " + reflected.getMessage());
    }

    @Test
    @DisplayName("singleton: a serialization round trip returns the SAME enum constant, not a copy")
    void enumSingletonSurvivesSerializationAsSameInstance() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(EnumSingletonDemo.FeeSchedule.INSTANCE);
        }
        EnumSingletonDemo.FeeSchedule back;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            back = (EnumSingletonDemo.FeeSchedule) in.readObject();
        }

        assertSame(EnumSingletonDemo.FeeSchedule.INSTANCE, back,
                "enums serialize by NAME only; readObject resolves via valueOf — no second instance");
        assertEquals(new BigDecimal("2.50"), back.rateFor("WIRE"));

        System.out.println("serialized, deserialized, and still == INSTANCE: " + (back == EnumSingletonDemo.FeeSchedule.INSTANCE));
    }
}
