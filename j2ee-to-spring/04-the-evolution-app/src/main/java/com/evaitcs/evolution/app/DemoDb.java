package com.evaitcs.evolution.app;

import org.h2.tools.RunScript;

import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * The demo database: stage-1's SecureBank world, rebuilt in H2 memory.
 * (README section "The one database, three eras".)
 *
 * Copied from module 01 unchanged except this javadoc — the point of the copy IS the
 * point of the capstone: all three eras in this module run against the SAME data, on
 * the SAME server, so the only thing that changes on screen is the technology. One
 * EvolutionServer.start() builds ONE fresh world; era 1 (raw JDBC), era 2 (Hibernate)
 * and era 3 (Spring MVC + JdbcTemplate) all move money in it side by side.
 *
 * The schema below is stage-1-database/schema.sql translated to H2 dialect:
 *   TIMESTAMPTZ -> TIMESTAMP WITH TIME ZONE, COMMENT ON dropped, everything else
 *   (identities, FKs, CHECK constraints, indexes) kept — constraints are teaching
 *   material here, not decoration.
 */
public final class DemoDb {

    /** DDL: the same five tables as stage-1, same columns, same constraints. */
    public static final String SCHEMA = """
            CREATE TABLE customer (
                id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                name       VARCHAR(100) NOT NULL,
                email      VARCHAR(255) NOT NULL,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
                CONSTRAINT uq_customer_email UNIQUE (email)
            );
            CREATE TABLE app_user (
                id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                username      VARCHAR(50)  NOT NULL,
                password_hash VARCHAR(100) NOT NULL,
                role          VARCHAR(20)  NOT NULL,
                customer_id   BIGINT,
                CONSTRAINT uq_app_user_username UNIQUE (username),
                CONSTRAINT fk_app_user_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
                CONSTRAINT chk_app_user_role CHECK (role IN ('CUSTOMER', 'TELLER', 'ADMIN'))
            );
            CREATE TABLE account (
                id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                customer_id BIGINT        NOT NULL,
                type        VARCHAR(20)   NOT NULL,
                balance     DECIMAL(15,2) NOT NULL DEFAULT 0,
                version     BIGINT        NOT NULL DEFAULT 0,
                CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
                CONSTRAINT chk_account_type CHECK (type IN ('CHECKING', 'SAVINGS')),
                CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
            );
            CREATE INDEX idx_account_customer ON account (customer_id);
            CREATE TABLE transactions (
                id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                account_id  BIGINT        NOT NULL,
                type        VARCHAR(10)   NOT NULL,
                amount      DECIMAL(15,2) NOT NULL,
                description VARCHAR(200),
                reference   VARCHAR(36),
                occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
                CONSTRAINT fk_tx_account FOREIGN KEY (account_id) REFERENCES account (id),
                CONSTRAINT chk_tx_type CHECK (type IN ('DEBIT', 'CREDIT')),
                CONSTRAINT chk_tx_amount_positive CHECK (amount > 0)
            );
            CREATE INDEX idx_tx_account_time ON transactions (account_id, occurred_at);
            CREATE TABLE loan (
                id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                customer_id     BIGINT        NOT NULL,
                principal       DECIMAL(15,2) NOT NULL,
                annual_rate     DECIMAL(5,2)  NOT NULL,
                term_months     INT           NOT NULL,
                status          VARCHAR(20)   NOT NULL DEFAULT 'APPLIED',
                monthly_payment DECIMAL(15,2),
                applied_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
                CONSTRAINT fk_loan_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
                CONSTRAINT chk_loan_status CHECK (status IN ('APPLIED', 'APPROVED', 'REJECTED', 'PAID_OFF')),
                CONSTRAINT chk_loan_principal CHECK (principal > 0),
                CONSTRAINT chk_loan_term CHECK (term_months BETWEEN 6 AND 360)
            );
            """;

    /**
     * The seed world, identical to stage-1-database/seed.sql:
     * customers 1=Alice 2=Bob 3=Carol(no accounts); accounts 1=Alice CHECKING 1500.00,
     * 2=Alice SAVINGS 5200.00, 3=Bob CHECKING 300.00; the opening ledger rows; one loan
     * APPLIED. Password hash is the demo BCrypt of 'password123' (house rule: demo
     * credentials only).
     */
    public static final String SEED = """
            INSERT INTO customer (name, email) VALUES
                ('Alice Nguyen', 'alice@securebank.dev'),
                ('Bob Rivera',   'bob@securebank.dev'),
                ('Carol Okafor', 'carol@securebank.dev');
            INSERT INTO app_user (username, password_hash, role, customer_id) VALUES
                ('alice',  '$2a$10$a5Joa76lCChBNq.jzinPs.CV2l1lSwgnEbjThHlgK7PeciYaNcDtG', 'CUSTOMER',
                 (SELECT id FROM customer WHERE email = 'alice@securebank.dev')),
                ('bob',    '$2a$10$a5Joa76lCChBNq.jzinPs.CV2l1lSwgnEbjThHlgK7PeciYaNcDtG', 'CUSTOMER',
                 (SELECT id FROM customer WHERE email = 'bob@securebank.dev')),
                ('teller', '$2a$10$a5Joa76lCChBNq.jzinPs.CV2l1lSwgnEbjThHlgK7PeciYaNcDtG', 'TELLER', NULL),
                ('admin',  '$2a$10$a5Joa76lCChBNq.jzinPs.CV2l1lSwgnEbjThHlgK7PeciYaNcDtG', 'ADMIN',  NULL);
            INSERT INTO account (customer_id, type, balance) VALUES
                ((SELECT id FROM customer WHERE email = 'alice@securebank.dev'), 'CHECKING', 1500.00),
                ((SELECT id FROM customer WHERE email = 'alice@securebank.dev'), 'SAVINGS',  5200.00),
                ((SELECT id FROM customer WHERE email = 'bob@securebank.dev'),   'CHECKING',  300.00);
            INSERT INTO transactions (account_id, type, amount, description, reference, occurred_at) VALUES
                (1, 'CREDIT', 1200.00, 'Opening deposit', NULL, now()),
                (1, 'CREDIT',  500.00, 'Payroll', NULL, now()),
                (2, 'CREDIT', 5000.00, 'Opening deposit', NULL, now()),
                (3, 'CREDIT',  500.00, 'Opening deposit', NULL, now()),
                (3, 'DEBIT',   200.00, 'ATM withdrawal', NULL, now()),
                (1, 'DEBIT',   200.00, 'Transfer out to savings', 'seed-transfer-0001', now()),
                (2, 'CREDIT',  200.00, 'Transfer in from checking', 'seed-transfer-0001', now());
            INSERT INTO loan (customer_id, principal, annual_rate, term_months, status) VALUES
                ((SELECT id FROM customer WHERE email = 'alice@securebank.dev'), 10000.00, 7.50, 36, 'APPLIED');
            """;

    private DemoDb() {
    }

    /**
     * Creates a BRAND-NEW in-memory database (unique name, schema + seed applied) and
     * returns its JDBC URL. Every demo/test gets its own world: no shared state, no
     * cleanup, deterministic ids (identities assign in insertion order, exactly like
     * stage-1's seed comments promise).
     *
     * DB_CLOSE_DELAY=-1 keeps the in-memory database alive while the JVM runs, even
     * between connections — a servlet opening a connection per request needs that.
     */
    public static String freshDatabase() {
        String url = "jdbc:h2:mem:securebank-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        try (Connection c = DriverManager.getConnection(url)) {
            RunScript.execute(c, new StringReader(SCHEMA));
            RunScript.execute(c, new StringReader(SEED));
        } catch (SQLException e) {
            throw new IllegalStateException("could not initialize the demo database", e);
        }
        return url;
    }

    /** Opens a connection to a database this class created. Callers close it (or don't — that IS the J2EE lesson). */
    public static Connection connect(String url) throws SQLException {
        return DriverManager.getConnection(url);
    }

    /** Test/demo helper: the current balance of one account, straight from SQL. */
    public static BigDecimal balanceOf(String url, long accountId) {
        try (Connection c = connect(url);
             var ps = c.prepareStatement("SELECT balance FROM account WHERE id = ?")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("no account " + accountId);
                }
                return rs.getBigDecimal("balance");
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The seed balances, as constants tests can lean on (scale 2, like the column). */
    public static final BigDecimal ALICE_CHECKING = new BigDecimal("1500.00");
    public static final BigDecimal ALICE_SAVINGS = new BigDecimal("5200.00");
    public static final BigDecimal BOB_CHECKING = new BigDecimal("300.00");
}
