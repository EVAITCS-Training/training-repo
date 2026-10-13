package com.evaitcs.evolution.j2ee;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * The J2EE-era DAO: a bank transfer in RAW JDBC, every step by hand.
 * (README section "The plumbing you owned"; this class is the exhibit.)
 *
 * Compare this file to what module 03 does with @Transactional + JdbcTemplate, and to
 * what stage-2 does with a Spring Data repository. The BUSINESS rule is four lines
 * (enough funds? debit, credit, write the ledger). Everything else below is plumbing
 * the framework era took over: get a connection, disable auto-commit, lock the rows,
 * map parameters by hand, commit, roll back on ANY failure, close EVERYTHING in the
 * right order, and translate SQLException into something callers can use.
 *
 * The ledger discipline is stage-1's: a transfer writes TWO rows into `transactions`
 * (DEBIT + CREDIT) sharing one `reference` so both halves reconcile.
 */
public class JdbcTransferDao {

    private final String dbUrl;

    public JdbcTransferDao(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    /** Thrown when the business rule refuses the transfer. Unchecked — module 11 of advanced-java explains why. */
    public static class InsufficientFundsException extends RuntimeException {
        private final long accountId;
        private final BigDecimal attempted;
        private final BigDecimal available;

        public InsufficientFundsException(long accountId, BigDecimal attempted, BigDecimal available) {
            super("account " + accountId + " has " + available + ", cannot send " + attempted);
            this.accountId = accountId;
            this.attempted = attempted;
            this.available = available;
        }

        public long accountId() {
            return accountId;
        }

        public BigDecimal attempted() {
            return attempted;
        }

        public BigDecimal available() {
            return available;
        }
    }

    /**
     * Move `amount` from one account to another, atomically, with a two-row ledger trail.
     *
     * Walkthrough — what happens when you call transfer(1, 3, 100.00):
     *   1. open a connection and turn OFF auto-commit (every statement alone would
     *      otherwise be its own transaction — a crash mid-transfer would tear money).
     *   2. SELECT ... FOR UPDATE locks both account rows IN ID ORDER (same order every
     *      time = no deadlock with a concurrent transfer the other way — stage-1
     *      queries/04 acts this out in psql).
     *   3. check the source balance; refuse if insufficient (the exception triggers the
     *      rollback in the catch below — nothing written so far survives).
     *   4. two UPDATEs move the balance; two INSERTs write the DEBIT/CREDIT ledger pair
     *      sharing one fresh reference id.
     *   5. COMMIT makes all four writes visible at once — or ROLLBACK erases them.
     *   6. finally: restore auto-commit and close the connection (in 2001, forgetting
     *      this line leaked a connection from the pool and eventually downed the server).
     */
    public String transfer(long fromId, long toId, BigDecimal amount) throws SQLException {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, was: " + amount);
        }
        if (fromId == toId) {
            throw new IllegalArgumentException("cannot transfer to the same account");
        }
        String reference = UUID.randomUUID().toString();

        Connection c = null;                                   // plumbing step: acquire
        try {
            c = DriverManager_getConnection();                 // (1)
            c.setAutoCommit(false);                              // (1)

            // Lock BOTH rows in ID ORDER — every concurrent transfer takes the locks in
            // the same sequence, so two transfers in opposite directions queue instead
            // of deadlocking (stage-1 queries/04 acts out what happens without this rule).
            long firstLock = Math.min(fromId, toId);
            long secondLock = Math.max(fromId, toId);
            BigDecimal firstBalance = lockAndReadBalance(c, firstLock);      // (2)
            BigDecimal secondBalance = lockAndReadBalance(c, secondLock);    // (2)
            BigDecimal fromBalance = (firstLock == fromId) ? firstBalance : secondBalance;
            BigDecimal toBalance = (firstLock == toId) ? firstBalance : secondBalance;

            if (fromBalance.compareTo(amount) < 0) {             // (3) the ONLY business line so far
                throw new InsufficientFundsException(fromId, amount, fromBalance);
            }

            updateBalance(c, fromId, fromBalance.subtract(amount));   // (4)
            updateBalance(c, toId, toBalance.add(amount));            // (4)
            insertLedgerRow(c, fromId, "DEBIT", amount, "Transfer out", reference);   // (4)
            insertLedgerRow(c, toId, "CREDIT", amount, "Transfer in", reference);     // (4)

            c.commit();                                          // (5)
            return reference;
        } catch (SQLException | RuntimeException e) {
            if (c != null) {
                try {
                    c.rollback();                                // (5) any failure erases everything
                } catch (SQLException rollbackFailed) {
                    e.addSuppressed(rollbackFailed);             // (advanced-java/11: suppression)
                }
            }
            throw e;
        } finally {
            if (c != null) {
                try {
                    c.setAutoCommit(true);
                    c.close();                                   // (6) the line everyone forgot once
                } catch (SQLException ignored) {
                    // closing is best-effort; the rollback above is what mattered
                }
            }
        }
    }

    /** Read one account's balance, locking its row for the rest of the transaction. */
    public BigDecimal balanceOf(long accountId) throws SQLException {
        try (Connection c = DriverManager_getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT balance FROM account WHERE id = ?")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("no account " + accountId);
                }
                return rs.getBigDecimal("balance");               // DECIMAL(15,2) -> BigDecimal, never double
            }
        }
    }

    // ---- the plumbing, one method per SQL statement -------------------------------

    private Connection DriverManager_getConnection() throws SQLException {
        // Method name deliberately ugly: in 2001 this line lived in EVERY DAO, and a
        // DataSource from JNDI was the "proper" version (ServiceLocator demo). Spring
        // turns it into one injected bean; Boot turns it into spring.datasource.url.
        return java.sql.DriverManager.getConnection(dbUrl);
    }

    private BigDecimal lockAndReadBalance(Connection c, long accountId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT balance FROM account WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("no account " + accountId);
                }
                return rs.getBigDecimal("balance");
            }
        }
    }

    private void updateBalance(Connection c, long accountId, BigDecimal newBalance) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE account SET balance = ?, version = version + 1 WHERE id = ?")) {
            ps.setBigDecimal(1, newBalance);
            ps.setLong(2, accountId);
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new SQLException("expected to update 1 account row, updated " + rows);
            }
        }
    }

    private void insertLedgerRow(Connection c, long accountId, String type, BigDecimal amount,
                                 String description, String reference) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO transactions (account_id, type, amount, description, reference) VALUES (?, ?, ?, ?, ?)")) {
            ps.setLong(1, accountId);
            ps.setString(2, type);
            ps.setBigDecimal(3, amount);
            ps.setString(4, description);
            ps.setString(5, reference);
            ps.executeUpdate();
        }
    }
}
