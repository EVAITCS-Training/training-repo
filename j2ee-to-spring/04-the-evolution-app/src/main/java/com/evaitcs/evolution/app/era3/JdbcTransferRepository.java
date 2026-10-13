package com.evaitcs.evolution.app.era3;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;

/**
 * ERA 3 — module 03's JdbcTransferRepository, unchanged in spirit and now a
 * COMPONENT: @Repository puts it in the @ComponentScan's net, the container builds it,
 * injects the DataSource through the constructor, and the exception translator applies.
 * (stage-2 counterpart: a Spring Data JPA interface — the SQL below would be method names.)
 *
 * The era-1-to-era-3 deletion list, one more time at the bottom of this file's history:
 * getConnection/try/finally/close, PreparedStatement binding, ResultSet walking,
 * throws SQLException — all gone; what remains is the SQL text itself and the
 * BigDecimal(scale 2) discipline. A framework deletes plumbing, never schema knowledge.
 *
 * Transactions: this class never mentions them. JdbcTemplate obtains its Connection via
 * DataSourceUtils, which hands back the thread-bound one when @Transactional (on
 * TransferService) started a transaction — module 03's quietest giant.
 */
@Repository
public class JdbcTransferRepository {

    /** What GET /api/v3/accounts/{id} needs: the type column and the balance, one row. */
    public record AccountRow(String type, BigDecimal balance) {
    }

    private final JdbcTemplate jdbc;

    /** Constructor injection: the DataSource arrives through the SIGNATURE — visible, typed, testable. */
    public JdbcTransferRepository(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /**
     * One row, one column, one lambda. Unknown account -> translated here from
     * EmptyResultDataAccessException (infrastructure word) into AccountNotFoundException
     * (domain word) so BankApiAdvice can map it to 404 without knowing JDBC exists.
     */
    public BigDecimal balanceOf(long accountId) {
        RowMapper<BigDecimal> balanceMapper = (rs, rowNum) -> rs.getBigDecimal("balance");
        try {
            return jdbc.queryForObject(
                    "SELECT balance FROM account WHERE id = ?", balanceMapper, accountId);
        } catch (EmptyResultDataAccessException e) {
            throw new AccountNotFoundException(accountId);
        }
    }

    /** The GET endpoint's read: id, type and balance as a record projection (not an entity — era 3 is JDBC, not ORM). */
    public AccountRow findAccount(long accountId) {
        RowMapper<AccountRow> mapper = (rs, rowNum) ->
                new AccountRow(rs.getString("type"), rs.getBigDecimal("balance"));
        try {
            return jdbc.queryForObject(
                    "SELECT type, balance FROM account WHERE id = ?", mapper, accountId);
        } catch (EmptyResultDataAccessException e) {
            throw new AccountNotFoundException(accountId);
        }
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
        // PreparedStatement ceremony of era 1's updateBalance(), gone.
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
}
