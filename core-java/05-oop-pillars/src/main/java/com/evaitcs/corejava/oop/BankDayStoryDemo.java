package com.evaitcs.corejava.oop;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A day at the SecureBank branch — all four pillars doing ONE story together.
 * Point at each block and name the pillar that makes it work:
 *
 *   the ledger is typed List<Account>            -> ABSTRACTION  (callers see the shape)
 *   Savings/Current reuse Account's machinery    -> INHERITANCE  (extends + override)
 *   one withdraw() loop, per-object rules        -> POLYMORPHISM (runtime dispatch)
 *   a refused transaction leaves no trace        -> ENCAPSULATION (invariant guards the state)
 */
public class BankDayStoryDemo {

    private static final BigDecimal FEE_THRESHOLD = new BigDecimal("500.00");

    public static void main(String[] args) {

        banner("Opening the branch — building the ledger");
        List<Account> ledger = new ArrayList<Account>();
        SavingsAccount ada = new SavingsAccount(
                "ACC-1001", "Ada Lovelace", new BigDecimal("1200.00"), new BigDecimal("0.04"));
        CurrentAccount grace = new CurrentAccount(
                "ACC-2002", "Grace Hopper", new BigDecimal("30.00"), new BigDecimal("200.00"));
        SavingsAccount alan = new SavingsAccount(
                "ACC-3003", "Alan Turing", new BigDecimal("25.00"), new BigDecimal("0.04"));
        ledger.add(ada);
        ledger.add(grace);
        ledger.add(alan);
        for (Account a : ledger) {
            System.out.println("  opened: " + a.describe());
        }

        banner("Morning — payroll lands: one loop, written ONCE against Account");
        for (Account a : ledger) {
            a.deposit(new BigDecimal("400.00"));    // POLYMORPHISM: every kind accepts it its own way
        }

        banner("Midday — rent day: same call, three different outcomes");
        for (Account a : ledger) {
            try {
                a.withdraw(new BigDecimal("450.00"));
                System.out.println("  OK      " + a.getId() + " -> balance " + a.getBalance()
                        + (a.getBalance().compareTo(BigDecimal.ZERO) < 0 ? "  (overdraft at work)" : ""));
            } catch (IllegalArgumentException refused) {
                // ENCAPSULATION: Alan's invariant said no, and his balance is UNTOUCHED.
                System.out.println("  REFUSED " + a.getId() + ": " + refused.getMessage());
            }
        }

        banner("Afternoon — interest run: instanceof + cast, the classic downcast");
        for (Account a : ledger) {
            if (a instanceof SavingsAccount) {          // ASK...
                SavingsAccount s = (SavingsAccount) a;  // ...CAST...
                s.applyInterest();                      // ...then use the subclass-only method
                System.out.println("  interest paid to " + s.getId() + " -> balance " + s.getBalance());
            }
        }

        banner("Closing — the fee desk: today's policy plugged in as an anonymous class");
        FeePolicy today = new FeePolicy() {
            @Override
            public BigDecimal feeFor(BigDecimal amount) {
                // Big balances pay 1.50, small ones 0.50 — tomorrow's schedule is a new
                // anonymous class; the ledger code below does not change. (ABSTRACTION.)
                return amount.compareTo(FEE_THRESHOLD) > 0
                        ? new BigDecimal("1.50")
                        : new BigDecimal("0.50");
            }
        };
        for (Account a : ledger) {
            BigDecimal fee = today.feeFor(a.getBalance());
            a.withdraw(fee);
            System.out.println("  fee " + fee + " charged to " + a.getId());
        }

        banner("End-of-day report");
        BigDecimal totalUnderCustody = BigDecimal.ZERO;
        for (Account a : ledger) {
            System.out.println("  " + a.describe());
            totalUnderCustody = totalUnderCustody.add(a.getBalance());
        }
        System.out.println("  -------------------------------------------------------");
        System.out.println("  total under custody: " + totalUnderCustody);
        System.out.println();
        System.out.println("  Recap, one line per pillar:");
        System.out.println("    ABSTRACTION    the ledger only ever saw the Account/FeePolicy shapes");
        System.out.println("    INHERITANCE    Savings/Current reused Account's state + withdraw machinery");
        System.out.println("    POLYMORPHISM   one deposit/withdraw loop; each object applied its own rules");
        System.out.println("    ENCAPSULATION  Alan's rent bounced — and left his balance exactly as it was");
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
