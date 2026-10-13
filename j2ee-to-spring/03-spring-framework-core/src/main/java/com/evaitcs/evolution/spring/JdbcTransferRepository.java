package com.evaitcs.evolution.spring;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.math.BigDecimal;

/**
 * Module 01's JdbcTransferDao, rewritten on JdbcTemplate — same SQL, same behavior,
 * ~130 lines down to ~40. (README section "JdbcTemplate deletes the DAO plumbing".)
 *
 * What was DELETED, banner by banner (JdbcTemplateRewriteDemo prints this table live):
 *   - getConnection / try / finally / close ........ JdbcTemplate + DataSourceUtils own it
 *   - PreparedStatement + ps.setLong/setBigDecimal.. update(sql, args...) varargs own it
 *   - rs.getBigDecimal + if (!rs.next()) ............ one RowMapper lambda owns it
 *   - catch (SQLException) / rethrow ................ exception TRANSLATION owns it (below)
 *   - setAutoCommit(false) / commit / rollback ...... @Transactional's proxy owns it —
 *                                                     not even this class's job anymore
 *
 * What was NOT deleted: the SQL text and the BigDecimal discipline (scale 2 — house rule,
 * money never touches double). A framework deletes plumbing, never your schema knowledge.
 *
 * EXCEPTION TRANSLATION — the quietest giant here: every method is UNCHECKED. JdbcTemplate
 * catches each SQLException at the boundary and rethrows it as a DataAccessException
 * subclass chosen by MEANING (duplicate key -> DuplicateKeyException; empty result ->
 * EmptyResultDataAccessException). Callers catch what happened, not what the vendor's
 * driver reported — advanced-java/11's boundary translation, productized.
 *
 * Transactions: this class never mentions them. JdbcTemplate obtains its Connection via
 * DataSourceUtils, which hands back the thread-bound one when @Transactional started a
 * transaction — and a fresh auto-commit one when it didn't. Same code runs both ways.
 */
public class JdbcTransferRepository {

    private final JdbcTemplate jdbc;

    /** Constructor injection: the DataSource arrives through the SIGNATURE — visible, typed, testable. */
    public JdbcTransferRepository(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /**
     * One row, one column, one lambda. Module 01 needed a PreparedStatement, a ResultSet,
     * an if (!rs.next()) guard and a try-with-resources for this exact read.
     * Unknown account -> EmptyResultDataAccessException (translated, unchecked).
     */
    public BigDecimal balanceOf(long accountId) {
        // RowMapper<T> = "ResultSet, row number -> T"; the lambda IS the whole mapping layer.
        RowMapper<BigDecimal> balanceMapper = (rs, rowNum) -> rs.getBigDecimal("balance");
        return jdbc.queryForObject(
                "SELECT balance FROM account WHERE id = ?", balanceMapper, accountId);
    }

    /** Money out: relative update, version bump kept (stage-1's optimistic-lock column). */
    public void debit(long accountId, BigDecimal amount) {
        shiftBalance(accountId, amount.negate());
    }

    /** Money in. */
    public void credit(long accountId, BigDecimal amount) {
        shiftBalance(accountId, amount);
    }

    private void shiftBalance(long accountId, BigDecimal delta) {
        // update(sql, args...): parameter binding by POSITION, values by type — the
        // PreparedStatement ceremony of module 01's updateBalance(), gone.
        int rows = jdbc.update(
                "UPDATE account SET balance = balance + ?, version = version + 1 WHERE id = ?",
                delta, accountId);
        if (rows != 1) {
            throw new IllegalStateException("expected to update 1 account row, updated " + rows);
        }
    }

    /** One half of stage-1's ledger rule: DEBIT/CREDIT rows sharing one reference reconcile. */
    public void insertLedgerRow(long accountId, String type, BigDecimal amount,
                                String description, String reference) {
        jdbc.update("""
                INSERT INTO transactions (account_id, type, amount, description, reference)
                VALUES (?, ?, ?, ?, ?)""",
                accountId, type, amount, description, reference);
    }

    /**
     * Exists for ONE demo: inserting a seeded email ('alice@securebank.dev') violates
     * uq_customer_email, and the SQLException(SQLState 23505) comes back out as
     * DuplicateKeyException — the translation story, caught red-handed.
     */
    public void addCustomer(String name, String email) {
        jdbc.update("INSERT INTO customer (name, email) VALUES (?, ?)", name, email);
    }
}
