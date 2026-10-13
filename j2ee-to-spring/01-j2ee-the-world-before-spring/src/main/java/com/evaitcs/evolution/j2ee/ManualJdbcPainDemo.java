package com.evaitcs.evolution.j2ee;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Demo 2 — THE PLUMBING, LIVE: JdbcTransferDao against a fresh in-memory database.
 * (README section "The plumbing you owned"; pairs with the DAO's walkthrough comment.)
 *
 * Two scenes, both deterministic:
 *   scene 1 — a GOOD transfer: balances move, the two-row ledger trail appears, and we
 *              count what the DAO did by hand vs what the business rule needed.
 *   scene 2 — a REFUSED transfer (insufficient funds): the exception fires mid-
 *              transaction and the rollback erases every partial write — the property
 *              @Transactional will later give you with zero code (module 03).
 */
public class ManualJdbcPainDemo {

    public static void main(String[] args) throws SQLException {
        String db = DemoDb.freshDatabase();
        JdbcTransferDao dao = new JdbcTransferDao(db);

        banner("1. The seed world (same as stage-1's PostgreSQL)");
        System.out.println("account 1 (Alice CHECKING) = " + DemoDb.balanceOf(db, 1));
        System.out.println("account 2 (Alice SAVINGS)  = " + DemoDb.balanceOf(db, 2));
        System.out.println("account 3 (Bob CHECKING)   = " + DemoDb.balanceOf(db, 3));

        banner("2. A GOOD transfer: 100.00 from Alice's checking to Bob's");
        String reference = dao.transfer(1, 3, new BigDecimal("100.00"));
        System.out.println("step 1: reference returned by the DAO = " + reference);
        System.out.println("step 2: account 1 now " + DemoDb.balanceOf(db, 1) + "  (was 1500.00)");
        System.out.println("step 3: account 3 now " + DemoDb.balanceOf(db, 3) + "  (was 300.00)");
        System.out.println("step 4: ledger rows sharing the reference:");
        printLedger(db, reference);
        System.out.println("verdict: money moved atomically — and EVERY step of the atomicity");
        System.out.println("         (autoCommit off, FOR UPDATE locks, commit, finally-close) was hand-written.");

        banner("3. A REFUSED transfer: 9999.00 from Bob — rollback erases the partial work");
        try {
            dao.transfer(3, 1, new BigDecimal("9999.00"));
        } catch (JdbcTransferDao.InsufficientFundsException e) {
            System.out.println("step 1: CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.out.println("step 2: the DAO had already locked both rows and read both balances");
            System.out.println("step 3: the exception hit the catch -> c.rollback() -> every partial write erased");
        }
        System.out.println("account 3 still " + DemoDb.balanceOf(db, 3) + " (unchanged by the refused transfer)");
        System.out.println("account 1 still " + DemoDb.balanceOf(db, 1) + " (the credit never landed)");

        banner("4. The line-count lesson");
        System.out.println("JdbcTransferDao: ~130 lines. Business rule inside it: 4 lines.");
        System.out.println("Module 03 rewrites the same rule as a @Transactional method over JdbcTemplate:");
        System.out.println("the plumbing becomes an annotation, and the DAO shrinks to the rule.");
    }

    /** Reads the two ledger halves of one transfer straight from SQL — the reconciliation view. */
    static void printLedger(String db, String reference) {
        try (var c = DemoDb.connect(db);
             var ps = c.prepareStatement(
                     "SELECT account_id, type, amount FROM transactions WHERE reference = ? ORDER BY account_id")) {
            ps.setString(1, reference);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println("        account " + rs.getLong("account_id")
                            + "  " + rs.getString("type")
                            + "  " + rs.getBigDecimal("amount").toPlainString());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
