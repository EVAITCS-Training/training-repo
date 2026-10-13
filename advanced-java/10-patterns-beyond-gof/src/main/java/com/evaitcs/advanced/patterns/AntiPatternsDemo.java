package com.evaitcs.advanced.patterns;

import java.math.BigDecimal;

/**
 * Demo D1 — ANTI-PATTERNS: the shapes that eat banking codebases alive.
 * (README section "Anti-patterns: named so you can refuse them".)
 *
 * The centerpiece is IMPLEMENTED, not narrated: the SAME transfer runs against an
 * ANEMIC account (data with no behavior — an overdraft slips through a scripted
 * check-then-act gap) and a RICH account (behavior with the data — the overdraft is
 * refused and the balance never corrupts). Same happy path, different failure
 * behavior; BeyondGofTest asserts both outcomes exactly.
 *
 * The scripted interleaving in banner 2 is the DETERMINISTIC form of module 04's
 * check-then-act race: there, two threads hit the gap by luck; here, a second caller
 * hits it on cue. Same bug shape, no timing involved.
 *
 * stage-2-monolith already made the rich choice on purpose: Account.deposit/withdraw
 * live ON the entity there, guarding the same invariant.
 */
public class AntiPatternsDemo {

    // ---- the ANEMIC world: data here, rules over there ----

    /** Bare fields with getters and setters. The invariant lives in NOBODY's house. */
    static final class AnemicAccount {
        private final String id;
        private BigDecimal balance;

        AnemicAccount(String id, BigDecimal balance) {
            this.id = id;
            this.balance = balance;
        }

        String getId() {
            return id;
        }

        BigDecimal getBalance() {
            return balance;
        }

        void setBalance(BigDecimal balance) {
            this.balance = balance;      // ANY caller, ANY value — the account cannot object
        }
    }

    /** The rules live in a service, AWAY from the data they must protect. */
    static final class AnemicTransferService {

        boolean checkFunds(AnemicAccount from, BigDecimal amount) {
            return from.getBalance().compareTo(amount) >= 0;
        }

        void moveMoney(AnemicAccount from, AnemicAccount to, BigDecimal amount) {
            from.setBalance(from.getBalance().subtract(amount));   // no check: trusts the caller
            to.setBalance(to.getBalance().add(amount));
        }
    }

    // ---- the RICH world: the data keeps its own promises ----

    /** The invariant lives WITH the balance it protects — no setter exists. */
    static final class RichAccount {
        private final String id;
        private BigDecimal balance;

        RichAccount(String id, BigDecimal balance) {
            this.id = id;
            this.balance = balance;
        }

        String id() {
            return id;
        }

        BigDecimal balance() {
            return balance;
        }

        /** Check and act in ONE method: there is no gap for a second caller to slip through. */
        void transferTo(RichAccount other, BigDecimal amount) {
            if (balance.compareTo(amount) < 0) {
                throw new InsufficientFundsException(id, balance, amount);
            }
            balance = balance.subtract(amount);
            other.balance = other.balance.add(amount);
        }
    }

    /** The same exception name stage-2-monolith uses — the type itself carries meaning. */
    static final class InsufficientFundsException extends RuntimeException {

        InsufficientFundsException(String accountId, BigDecimal available, BigDecimal attempted) {
            super("account " + accountId + " has " + available.toPlainString()
                    + ", tried to send " + attempted.toPlainString());
        }
    }

    public static void main(String[] args) {
        banner("1. Happy path: the two worlds look IDENTICAL");

        AnemicAccount anemicFrom = new AnemicAccount("ACC-A", new BigDecimal("60.00"));
        AnemicAccount anemicTo = new AnemicAccount("ACC-B", BigDecimal.ZERO.setScale(2));
        AnemicTransferService anemicService = new AnemicTransferService();
        if (anemicService.checkFunds(anemicFrom, new BigDecimal("50.00"))) {
            anemicService.moveMoney(anemicFrom, anemicTo, new BigDecimal("50.00"));
        }
        RichAccount richFrom = new RichAccount("ACC-A", new BigDecimal("60.00"));
        RichAccount richTo = new RichAccount("ACC-B", BigDecimal.ZERO.setScale(2));
        richFrom.transferTo(richTo, new BigDecimal("50.00"));
        System.out.println("anemic: ACC-A = " + anemicFrom.getBalance().toPlainString()
                + ", ACC-B = " + anemicTo.getBalance().toPlainString());
        System.out.println("rich  : ACC-A = " + richFrom.balance().toPlainString()
                + ", ACC-B = " + richTo.balance().toPlainString());
        System.out.println("verdict: same result, 10.00 / 50.00. Design smells only show on the unhappy path.");

        banner("2. Anemic world: the overdraft SLIPS THROUGH a check-then-act gap");

        AnemicAccount from = new AnemicAccount("ACC-A", new BigDecimal("60.00"));
        AnemicAccount to = new AnemicAccount("ACC-B", BigDecimal.ZERO.setScale(2));
        AnemicTransferService service = new AnemicTransferService();
        System.out.println("step 1: service.checkFunds(from, 50.00) = "
                + service.checkFunds(from, new BigDecimal("50.00")) + "   (60.00 >= 50.00, so far so good)");
        from.setBalance(new BigDecimal("40.00"));     // a nightly-fee job pokes the bare setter
        System.out.println("step 2: a nightly-fee job (legitimate code!) sets balance to 40.00 —");
        System.out.println("        the account has NO say; setBalance accepts anything");
        service.moveMoney(from, to, new BigDecimal("50.00"));
        System.out.println("step 3: service.moveMoney re-reads 40.00 and blindly subtracts 50.00");
        System.out.println("step 4: ACC-A = " + from.getBalance().toPlainString()
                + "  <- NEGATIVE, ACC-B = " + to.getBalance().toPlainString());
        System.out.println("verdict: money was created from nothing. The check and the act were TWO");
        System.out.println("         calls on bare data, and anything could happen between them.");
        System.out.println("         module 04 showed the same gap as a thread race (73412 lost updates);");
        System.out.println("         here it is scripted — the bug is the SHAPE, not the timing.");

        banner("3. Rich world: the same overdraft is REFUSED, and nothing corrupts");

        RichAccount richA = new RichAccount("ACC-A", new BigDecimal("40.00"));
        RichAccount richB = new RichAccount("ACC-B", BigDecimal.ZERO.setScale(2));
        try {
            richA.transferTo(richB, new BigDecimal("50.00"));
        } catch (InsufficientFundsException e) {
            System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("why: check and act are ONE method on the data — no gap, no setBalance,");
            System.out.println("     no caller able to skip the rule. The exception type itself tells the");
            System.out.println("     story (stage-2 maps it to HTTP 422 via ProblemDetail automatically).");
        }
        System.out.println("balances after the refused transfer: ACC-A = " + richA.balance().toPlainString()
                + ", ACC-B = " + richB.balance().toPlainString() + " — untouched, exact.");

        banner("4. God class: the smell list (narrative — no bank deserves a live one)");

        System.out.println("picture: BankManager, 4,000 lines — parses HTTP, writes SQL, computes fees,");
        System.out.println("         sends e-mail, and generates the PDF statement. All in one class.");
        System.out.println("smell 1: twenty-plus fields, half of them other objects it should be delegating to");
        System.out.println("smell 2: fifty-plus methods and no single sentence describes what the class IS");
        System.out.println("smell 3: many REASONS TO CHANGE — HTTP format, SQL, fee rules, mail vendor...");
        System.out.println("smell 4: the name is Manager, God, Util, Helper or Common — names that mean 'pile'");
        System.out.println("smell 5: its unit test needs half the bank running to assert anything");
        System.out.println("smell 6: every branch touches it, so every merge conflicts in it");

        banner("5. Singleton abuse and the service locator (module 09's trap, grown up)");

        System.out.println("ServiceLocator.get(TransferService.class) LOOKS like convenience. It is debt:");
        System.out.println("  - hidden dependencies: the constructor no longer tells you what a class needs;");
        System.out.println("  - global state: tests contaminate each other through the one shared locator;");
        System.out.println("  - one graph per JVM: no way to run two configurations side by side.");
        System.out.println("DI (MiniContainerDemo) inverts all three: dependencies arrive as constructor");
        System.out.println("parameters — visible in the type, fakeable in tests, one graph per container.");

        banner("6. The one-liners");

        System.out.println("shared mutable state : the root of module 04's lost updates and module 05's");
        System.out.println("                       lock gymnastics — prefer immutable and confined (demo B1).");
        System.out.println("golden hammer        : 'we have Kafka, so everything is an event' — a 12-row");
        System.out.println("                       lookup table needs a Map, not a streaming platform.");
        System.out.println("premature optimization: measure first (module 07's MXBeans and profilers);");
        System.out.println("                       optimization without measurement is guessing in a lab coat.");

        banner("7. In one sentence");
        System.out.println("Anti-patterns are not stupid mistakes — they are reasonable shortcuts whose");
        System.out.println("bill arrives later; naming them is how a team refuses the bill on purpose.");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
