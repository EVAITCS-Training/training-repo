package com.evaitcs.corejava.oop;

import java.math.BigDecimal;

/**
 * ENCAPSULATION — pillar number one on every exam list, and the most misquoted one.
 *
 * It is NOT "hide the fields because tidiness". It IS: every path into the state
 * goes through a method that can say NO. Account's invariant — a savings balance
 * never drops below zero — is enforced in exactly one place (withdraw), and no
 * caller can get around it, because there is no setBalance().
 *
 * Encapsulation = protecting INVARIANTS, not merely hiding data. The demo ends
 * with the counter-example: a public field, and the damage any caller can do.
 */
public class EncapsulationDemo {

    public static void main(String[] args) {

        banner("1. The front door: private state, public rules");
        SavingsAccount acc = new SavingsAccount(
                "ACC-1001", "Ada Lovelace", new BigDecimal("500.00"), new BigDecimal("0.04"));
        System.out.println("  " + acc.describe());
        acc.deposit(new BigDecimal("250.00"));      // allowed: any positive amount
        acc.withdraw(new BigDecimal("700.00"));     // allowed: 750.00 - 700.00 is still >= 0
        System.out.println("  after deposit(250.00) then withdraw(700.00): balance = " + acc.getBalance());

        banner("2. The invariant says NO — and the state survives untouched");
        try {
            acc.withdraw(new BigDecimal("99999.00"));   // would take the balance below zero...
            System.out.println("  (this line should never print)");
        } catch (IllegalArgumentException rejected) {
            System.out.println("  caught IllegalArgumentException -> " + rejected.getMessage());
        }
        System.out.println("  balance after the refused withdraw: " + acc.getBalance()
                + "   <- UNCHANGED. The check-then-act lives inside ONE method, so the");
        System.out.println("     caller's story is atomic: either the money moved, or nothing happened.");
        try {
            acc.deposit(new BigDecimal("-50.00"));      // a negative deposit is a withdrawal in disguise
        } catch (IllegalArgumentException rejected) {
            System.out.println("  REFUSED: " + rejected.getMessage());
        }
        System.out.println();
        System.out.println("  Notice what does NOT exist on Account: setBalance(). A setter that");
        System.out.println("  accepts anything is a hole in the invariant with a polite name on it.");

        banner("3. The counter-example: what breaks when the field is public");
        LeakyAccount leaky = new LeakyAccount("ACC-6666");
        leaky.balance = new BigDecimal("-999999");      // compiles. Nothing can stop this line.
        System.out.println("  leaky.balance is now " + leaky.balance
                + "   <- and NO method ran to object.");
        System.out.println();
        System.out.println("  With a public field:");
        System.out.println("    - ANY caller can set balance = -999999 — no check ever runs");
        System.out.println("    - interest is computed on nonsense; an audit log never hears about it");
        System.out.println("    - every future rule (limits, rounding, logging) means hunting down EVERY caller");
        System.out.println("  The class stops owning the meaning of its own state. THAT is what");
        System.out.println("  encapsulation protects: not secrecy — INVARIANTS.");
    }

    /**
     * The exhibit to point at and shake your head at: a bank account whose data anyone
     * can scribble on. OopPillarsTest pokes it to prove the poke lands. Never write one.
     */
    static class LeakyAccount {
        public String id;
        public BigDecimal balance = BigDecimal.ZERO;   // public field: the caller now owns your invariant

        LeakyAccount(String id) {
            this.id = id;
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
