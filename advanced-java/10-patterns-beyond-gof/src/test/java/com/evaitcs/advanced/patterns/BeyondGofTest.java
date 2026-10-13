package com.evaitcs.advanced.patterns;

import com.evaitcs.advanced.patterns.IdiomsDemo.AuditSink;
import com.evaitcs.advanced.patterns.IdiomsDemo.Customer;
import com.evaitcs.advanced.patterns.IdiomsDemo.BankAccount;
import com.evaitcs.advanced.patterns.IdiomsDemo.Address;
import com.evaitcs.advanced.patterns.IdiomsDemo.CustomerProfile;
import com.evaitcs.advanced.patterns.IdiomsDemo.ListAuditSink;
import com.evaitcs.advanced.patterns.IdiomsDemo.NullAuditSink;
import com.evaitcs.advanced.patterns.IdiomsDemo.ReorganizedCustomer;
import com.evaitcs.advanced.patterns.MiniContainerDemo.AuditLog;
import com.evaitcs.advanced.patterns.MiniContainerDemo.AccountRepository;
import com.evaitcs.advanced.patterns.MiniContainerDemo.MiniContainer;
import com.evaitcs.advanced.patterns.MiniContainerDemo.TransferController;
import com.evaitcs.advanced.patterns.MiniContainerDemo.TransferService;
import com.evaitcs.advanced.patterns.MiniContainerDemo.UnmanagedThing;
import com.evaitcs.advanced.patterns.MoneyValueObjectDemo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests as PROOF for the claims made in the demos. Run with:
 *   .\mvnw.cmd -pl 10-patterns-beyond-gof test
 * Each test proves ONE claim and prints its evidence, so the test run doubles as a demo.
 *
 * Everything asserted here is DETERMINISTIC: the container build is a fixed
 * depth-first walk, every scene is single-threaded, and the anemic-account scene
 * SCRIPTS the check-then-act interleaving instead of racing for it (module 04's
 * race, put on cue — same bug shape, zero timing dependence).
 */
class BeyondGofTest {

    private static final List<Class<?>> SCAN = List.of(
            AccountRepository.class, AuditLog.class, TransferService.class, TransferController.class);

    @Test
    @DisplayName("DI: the mini container wires the whole graph, shares one instance per type, and a second container builds fresh")
    void miniContainerWiresTheGraphWithSingletonsPerContainer() {
        MiniContainer container = new MiniContainer(SCAN);

        TransferController controller = container.get(TransferController.class);
        assertNotNull(controller, "the top bean exists");
        assertNotNull(controller.service(), "its service dependency was injected, not null");
        assertNotNull(controller.service().repository(), "the service's repository was injected transitively");

        // Correct instances: the injected objects ARE the container's singletons.
        assertSame(container.get(TransferService.class), controller.service());
        assertSame(container.get(AccountRepository.class), controller.service().repository());
        assertSame(container.get(AuditLog.class), container.get(AuditLog.class), "singleton per type: == on every get");

        // The wired graph works, and both layers share ONE audit log instance.
        String reference = controller.handleTransfer("ACC-1", "ACC-2", new BigDecimal("250.00"));
        assertEquals("TXN-ACC-1-ACC-2", reference);
        assertEquals(2, container.get(AuditLog.class).lines().size(), "controller line + service line, same log object");
        assertEquals(new BigDecimal("250.00"), container.get(AccountRepository.class).balanceOf("ACC-1"));
        assertEquals(new BigDecimal("350.00"), container.get(AccountRepository.class).balanceOf("ACC-2"));

        // A SECOND container is a SECOND world: fresh instances, untouched seed data.
        MiniContainer second = new MiniContainer(SCAN);
        assertNotSame(container.get(TransferController.class), second.get(TransferController.class));
        assertNotSame(container.get(AccountRepository.class), second.get(AccountRepository.class));
        assertEquals(new BigDecimal("500.00"), second.get(AccountRepository.class).balanceOf("ACC-1"),
                "the second container's store still has its seed balance — no shared state between containers");

        System.out.println("build order = " + container.buildLog());
        System.out.println("one graph per container: same-instance (==) inside, fresh instances (not ==) across");
    }

    @Test
    @DisplayName("DI: without the @Wire annotation the container refuses to build the class — the annotation is the entry ticket")
    void miniContainerRefusesClassesWithoutTheWireAnnotation() {
        MiniContainer container = new MiniContainer(List.of(AuditLog.class));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> container.get(UnmanagedThing.class));
        assertTrue(e.getMessage().contains("UnmanagedThing"), "the refusal names the offending class");

        System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        System.out.println("module 03's lesson: the annotation alone does nothing — this container is the reader");
    }

    @Test
    @DisplayName("layered: the flow returns a DTO with exact fields, and the entity class never crosses the service boundary")
    void layeredFlowReturnsDtoAndTheEntityNeverCrossesTheBoundary() {
        LayeredBankDemo.AccountRepository repository = new LayeredBankDemo.AccountRepository();
        LayeredBankDemo.TransferService service = new LayeredBankDemo.TransferService(repository);
        LayeredBankDemo.TransferController controller = new LayeredBankDemo.TransferController(service);

        LayeredBankDemo.TransferDto dto = controller.handle(
                new LayeredBankDemo.TransferRequest("ACC-1", "ACC-2", new BigDecimal("250.00")));

        // The boundary proof: what crossed back is the DTO class — the entity stayed inside.
        assertEquals(LayeredBankDemo.TransferDto.class, dto.getClass());
        assertEquals("TXN-1", dto.reference());
        assertEquals("ACC-1", dto.fromId());
        assertEquals("ACC-2", dto.toId());
        assertEquals(new BigDecimal("250.00"), dto.amount());
        assertEquals(new BigDecimal("250.00"), dto.fromBalanceAfter(), "500.00 - 250.00");
        assertEquals(new BigDecimal("350.00"), dto.toBalanceAfter(), "100.00 + 250.00");

        LayeredBankDemo.AccountDto view = controller.handleView("ACC-1");
        assertEquals(LayeredBankDemo.AccountDto.class, view.getClass());
        assertEquals("Dana", view.owner());
        assertEquals(new BigDecimal("250.00"), view.balance());

        // The overdraft rule lives in the service — the web layer cannot forget it.
        assertThrows(LayeredBankDemo.InsufficientFundsException.class,
                () -> controller.handle(new LayeredBankDemo.TransferRequest("ACC-1", "ACC-2", new BigDecimal("99999.00"))));

        System.out.println("crossed the boundary: " + dto.getClass().getSimpleName() + " " + dto);
        System.out.println("never crossed: AccountEntity (package-private, and no method above the service returns it)");
    }

    @Test
    @DisplayName("value object: the compact constructor rejects null, negative and wrong-scale money at the door")
    void moneyRejectsNullNegativeAndWrongScaleAtTheDoor() {
        assertThrows(NullPointerException.class, () -> new Money(null), "requireNonNull guards the null");

        IllegalArgumentException scaleLow = assertThrows(IllegalArgumentException.class, () -> Money.of("2.5"));
        assertTrue(scaleLow.getMessage().contains("scale"), "the rejection names the rule it enforces");
        assertThrows(IllegalArgumentException.class, () -> Money.of("2.500"), "scale 3 is just as wrong as scale 1");

        IllegalArgumentException negative = assertThrows(IllegalArgumentException.class, () -> Money.of("-5.00"));
        assertTrue(negative.getMessage().contains("negative"));

        // No negative-zero surprise: BigDecimal normalizes "-0.00", so there is one zero.
        assertEquals(Money.of("0.00"), Money.of("-0.00"), "\"-0.00\" parses to plain zero — no haunted twin");

        System.out.println("CAUGHT -> NullPointerException: amount must not be null");
        System.out.println("CAUGHT -> " + scaleLow.getClass().getSimpleName() + ": " + scaleLow.getMessage());
        System.out.println("CAUGHT -> " + negative.getClass().getSimpleName() + ": " + negative.getMessage());
        System.out.println("Money.of(\"-0.00\") = " + Money.of("-0.00") + " — equals Money.of(\"0.00\"): "
                + Money.of("-0.00").equals(Money.of("0.00")));
    }

    @Test
    @DisplayName("value object: equal amounts ARE equal, and every operation returns a NEW instance")
    void moneyHasValueEqualityAndOperationsReturnNewInstances() {
        assertEquals(Money.of("2.50"), Money.of("2.50"), "value equality, not identity");
        assertEquals(Money.of("2.50").hashCode(), Money.of("2.50").hashCode());
        assertNotEquals(Money.of("2.50"), Money.of("2.51"));

        // WHY the scale pin matters: BigDecimal.equals is scale-sensitive...
        assertNotEquals(new BigDecimal("1.5"), new BigDecimal("1.50"),
                "raw BigDecimals with different scales are NOT equal — Money forbids that ambiguity");

        Money start = Money.of("10.00");
        Money sum = start.add(Money.of("5.00"));
        assertNotSame(start, sum, "add returned a NEW object");
        assertEquals(Money.of("10.00"), start, "the original is untouched — immutability");
        assertEquals(Money.of("15.00"), sum);

        assertThrows(IllegalArgumentException.class, () -> start.subtract(sum),
                "10.00 - 15.00 would be negative — the bouncer rejects the RESULT too");

        System.out.println("start = " + start + ", start.add(5.00) = " + sum + ", start is still " + start);
        System.out.println("BigDecimal(\"1.5\").equals(BigDecimal(\"1.50\")) = false — which is why Money pins scale 2");
    }

    @Test
    @DisplayName("rich domain: transferTo moves money exactly, and an overdraft throws with balances untouched")
    void richAccountTransfersExactlyAndRefusesOverdraft() {
        AntiPatternsDemo.RichAccount from = new AntiPatternsDemo.RichAccount("ACC-A", new BigDecimal("60.00"));
        AntiPatternsDemo.RichAccount to = new AntiPatternsDemo.RichAccount("ACC-B", BigDecimal.ZERO.setScale(2));

        from.transferTo(to, new BigDecimal("50.00"));
        assertEquals(new BigDecimal("10.00"), from.balance());
        assertEquals(new BigDecimal("50.00"), to.balance());

        AntiPatternsDemo.InsufficientFundsException e = assertThrows(
                AntiPatternsDemo.InsufficientFundsException.class,
                () -> from.transferTo(to, new BigDecimal("1000.00")));
        assertTrue(e.getMessage().contains("ACC-A"), "the exception names the account that refused");
        assertEquals(new BigDecimal("10.00"), from.balance(), "a refused transfer changed NOTHING");
        assertEquals(new BigDecimal("50.00"), to.balance(), "a refused transfer changed NOTHING");

        System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        System.out.println("check and act are ONE method — there is no gap between them");
    }

    @Test
    @DisplayName("anemic domain: the same overdraft slips through a scripted check-then-act gap and corrupts the balance")
    void anemicAccountLetsTheSameOverdraftSlipThroughAndCorruptsTheBalance() {
        AntiPatternsDemo.AnemicAccount from = new AntiPatternsDemo.AnemicAccount("ACC-A", new BigDecimal("60.00"));
        AntiPatternsDemo.AnemicAccount to = new AntiPatternsDemo.AnemicAccount("ACC-B", BigDecimal.ZERO.setScale(2));
        AntiPatternsDemo.AnemicTransferService service = new AntiPatternsDemo.AnemicTransferService();

        assertTrue(service.checkFunds(from, new BigDecimal("50.00")), "60.00 >= 50.00 — the check passes");

        // The gap, scripted: a legitimate nightly-fee job pokes the bare setter between
        // the check and the act. This is module 04's check-then-act race put on cue —
        // same bug shape, zero timing dependence.
        from.setBalance(new BigDecimal("40.00"));
        service.moveMoney(from, to, new BigDecimal("50.00"));

        // EVIDENCE OF THE ANTI-PATTERN: the assertions below PROVE the corruption.
        // A negative balance means money was created from nothing, and the account
        // object had no way to object — the rules live in the service, not in the data.
        assertEquals(new BigDecimal("-10.00"), from.getBalance(), "40.00 - 50.00: the overdraft slipped through");
        assertEquals(new BigDecimal("50.00"), to.getBalance(), "the receiving side was credited as if all were well");

        System.out.println("from = " + from.getBalance().toPlainString() + " (CORRUPTED), to = " + to.getBalance().toPlainString());
        System.out.println("same happy path as the rich account — different failure behavior. That is the whole lesson.");
    }

    @Test
    @DisplayName("null object: the silent sink is a no-op with exact defaults, and call sites never null-check")
    void nullObjectIsASilentNoOpWithExactDefaults() {
        AuditSink off = new NullAuditSink();
        off.record("transfer TXN-1");     // identical call sites for both sinks —
        off.record("transfer TXN-2");     // not one null check anywhere
        off.record("transfer TXN-3");

        assertEquals(0, off.size(), "recorded three times, kept nothing");
        assertEquals("(none)", off.lastEntry(), "the exact documented default");

        AuditSink on = new ListAuditSink();
        on.record("transfer TXN-1");
        on.record("transfer TXN-2");
        on.record("transfer TXN-3");
        assertEquals(3, on.size(), "the same calls through the same interface do real work");
        assertEquals("transfer TXN-3", on.lastEntry());

        System.out.println("NullAuditSink: 3 records -> size 0, lastEntry \"(none)\"");
        System.out.println("ListAuditSink: 3 records -> size 3, lastEntry \"" + on.lastEntry() + "\"");
    }

    @Test
    @DisplayName("Demeter: city() survives an internal restructure that the train-wreck chain would not")
    void demeterCitySurvivesAnInternalRestructureTheTrainWreckWouldNot() {
        // BEFORE the restructure: the train wreck compiles and works...
        Customer before = new Customer(new BankAccount(new Address("Amsterdam")));
        assertEquals("Amsterdam", before.getAccount().getAddress().getCity(), "four hops, works today");
        assertEquals("Amsterdam", before.city(), "one hop, same answer");

        // AFTER: addresses moved into profiles. The one-hop call site is UNTOUCHED...
        ReorganizedCustomer after = new ReorganizedCustomer(new CustomerProfile("Amsterdam", "2019"));
        assertEquals("Amsterdam", after.city(), "city() survived the restructure");

        // ...while the train-wreck chain has NOTHING to hold onto anymore: reflection
        // proves the hop the wreck depended on no longer exists on the new customer.
        assertThrows(NoSuchMethodException.class, () -> ReorganizedCustomer.class.getMethod("getAccount"),
                "getAccount no longer exists — every wreck call site would stop compiling");

        System.out.println("before: getAccount().getAddress().getCity() = Amsterdam; after: city() = Amsterdam");
        System.out.println("the restructured customer has no getAccount — the wreck breaks, the one hop does not");
    }
}
