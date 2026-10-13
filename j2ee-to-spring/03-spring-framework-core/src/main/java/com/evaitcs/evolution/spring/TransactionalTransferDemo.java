package com.evaitcs.evolution.spring;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Demo 5 — DECLARATIVE TRANSACTIONS: commit, rollback, and the checked-exception trap.
 * (README section "@Transactional: the proxy owns the transaction".)
 *
 * The business rule is TransferService.transfer() — go read it: check funds, two updates,
 * two ledger rows, one event. NOT ONE LINE about connections, auto-commit, commit,
 * rollback or close. Module 01's DAO wrapped those ~50 plumbing lines around the same
 * four business lines; here the plumbing is a PROXY, built at refresh() and installed
 * around the bean (demo 6 shows the proxy itself).
 *
 * Four banners, four behaviors — all deterministic, all asserted again in SpringCoreTest:
 *   1. happy path           -> COMMIT: balances move, ledger pair shares one reference.
 *   2. refused transfer     -> ROLLBACK: RuntimeException = the default rollback trigger.
 *   3. failure after a write -> ROLLBACK you can watch: the debit lands, then is erased.
 *   4. CHECKED exception    -> COMMIT (!): the default rule's famous trap, proven live.
 */
public class TransactionalTransferDemo {

    public static void main(String[] args) {
        String db = DemoDb.freshDatabase();
        try (AnnotationConfigApplicationContext ctx = SpringApp.start(db)) {
            TransferService service = ctx.getBean(TransferService.class);

            banner("1. COMMIT: transfer 100.00 from account 1 (Alice CHECKING) to account 3 (Bob CHECKING)");
            System.out.println("step 1: before — account 1 = " + DemoDb.balanceOf(db, 1) + ", account 3 = " + DemoDb.balanceOf(db, 3));
            String reference = service.transfer(1, 3, new BigDecimal("100.00"));
            System.out.println("step 2: transfer returned reference = " + reference);
            System.out.println("step 3: after  — account 1 = " + DemoDb.balanceOf(db, 1) + " (was 1500.00), account 3 = "
                    + DemoDb.balanceOf(db, 3) + " (was 300.00)");
            System.out.println("step 4: the ledger pair sharing the reference (stage-1's reconciliation rule):");
            printLedger(db, reference);
            System.out.println("verdict: COMMITTED — all four writes became visible together, and the method body");
            System.out.println("         never said the word 'transaction'. The proxy did: begin -> invoke -> commit.");

            banner("2. ROLLBACK: a refused transfer (RuntimeException = the default rollback trigger)");
            long rowsBefore = countLedgerRows(db);
            try {
                service.transfer(3, 1, new BigDecimal("9999.00"));
            } catch (TransferService.InsufficientFundsException e) {
                System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.out.println("         WHY: the business rule refused; the exception crossed the proxy");
                System.out.println("         boundary, and the default rule (RuntimeException -> rollback) fired.");
            }
            System.out.println("step 1: account 3 still " + DemoDb.balanceOf(db, 3) + ", account 1 still " + DemoDb.balanceOf(db, 1));
            System.out.println("step 2: ledger rows before = " + rowsBefore + ", after = " + countLedgerRows(db) + " — nothing new landed");
            System.out.println("verdict: ROLLED BACK — same guarantee module 01 hand-wrote with catch { c.rollback(); }.");

            banner("3. ROLLBACK you can watch: a write that LANDS, then is erased");
            System.out.println("step 1: debitThenRuntimeFailure(3, 50.00) — the debit EXECUTES first, then throws.");
            try {
                service.debitThenRuntimeFailure(3, new BigDecimal("50.00"));
            } catch (IllegalStateException e) {
                System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.out.println("         WHY: thrown AFTER a real write — the strongest rollback test there is.");
            }
            System.out.println("step 2: account 3 = " + DemoDb.balanceOf(db, 3) + " — the executed debit was ERASED; back to the 400.00 it had before the call");
            System.out.println("verdict: the transaction made a completed SQL UPDATE unhappen. That is atomicity,");
            System.out.println("         declared with one annotation instead of ~25 lines of module 01's catch/finally.");

            banner("4. THE TRAP: a CHECKED exception does NOT roll back by default — it COMMITS");
            System.out.println("step 1: debitThenCheckedFailure(3, 25.00) — same shape as banner 3, but 'throws Exception'.");
            try {
                service.debitThenCheckedFailure(3, new BigDecimal("25.00"));
            } catch (Exception e) {
                System.out.println("CAUGHT -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            System.out.println("step 2: account 3 = " + DemoDb.balanceOf(db, 3) + " — the debit COMMITTED (400.00 -> 375.00).");
            System.out.println("step 3: the exact default rule: RuntimeException and Error roll back;");
            System.out.println("        CHECKED exceptions pass through and the transaction COMMITS.");
            System.out.println("        WHY it was designed that way: EJB-era convention assumed checked = 'handled");
            System.out.println("        business outcome, keep the work' — an assumption that ages badly.");
            System.out.println("step 4: the fixes: @Transactional(rollbackFor = Exception.class), or the house rule —");
            System.out.println("        domain failures are UNCHECKED (InsufficientFundsException already is).");
            System.out.println("In one sentence: @Transactional gives module 01's atomicity for free — as long as you");
            System.out.println("know the rollback rule states EXACTLY 'unchecked only', and say so in every code review.");
        }
    }

    /** The reconciliation view, straight from SQL — the same query module 01's demo printed. */
    private static void printLedger(String db, String reference) {
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

    /** Total ledger rows — the 'nothing new landed' evidence for banner 2. */
    private static long countLedgerRows(String db) {
        try (var c = DemoDb.connect(db);
             var ps = c.prepareStatement("SELECT COUNT(*) FROM transactions");
             var rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
