-- ============================================================================
-- SECUREBANK SCHEMA (Week 3 + Project Kick-In)
-- ============================================================================
-- This file is the SINGLE SOURCE OF TRUTH for the database shape.
-- In stage-2 the JPA entities are validated against it (spring.jpa.hibernate
-- .ddl-auto=validate on the postgres profile) — if an entity and its table
-- drift apart, the app refuses to boot. Schema-as-code discipline.
--
-- THE ERD IN WORDS (see docs/ERD.md for the diagram):
--
--   customer 1 ──── * account 1 ──── * transactions
--      │                 (money lives here; every movement writes a ledger row)
--      ├── 1 ── * loan            (applications + approval lifecycle)
--      └── 1 ── 0..1 app_user     (login identity; staff users have none)
--
-- CONVENTIONS USED (and why):
--   * BIGINT GENERATED ALWAYS AS IDENTITY  -> surrogate keys, DB-generated
--     (safe under concurrency; the DB, not the app, owns uniqueness).
--   * snake_case names                     -> SQL standard style; Hibernate's
--     naming strategy maps camelCase Java fields onto these automatically.
--   * TIMESTAMPTZ for instants             -> stores UTC unambiguously
--     (TIMESTAMP without tz silently mixes local times — a classic bug).
--   * DECIMAL(15,2) for money              -> exact decimal arithmetic;
--     FLOAT would accumulate rounding errors (never use float for money!).
--   * FK + CHECK constraints               -> the database enforces the rules
--     even if some future service forgets them (defense in depth).
-- ============================================================================

-- ---------------------------------------------------------------------------
-- customer: the person who owns accounts and applies for loans
-- ---------------------------------------------------------------------------
CREATE TABLE customer (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- UNIQUE = at most one row per email; enforced by a hidden unique index.
    CONSTRAINT uq_customer_email UNIQUE (email)
);
COMMENT ON TABLE customer IS 'Bank clients. Java mirror: domain/Customer.java';

-- ---------------------------------------------------------------------------
-- app_user: LOGIN identity. Separate from customer on purpose:
--   * customers log in AND own accounts (customer_id set)
--   * staff (teller/admin) log in but own nothing   (customer_id NULL)
-- Table is named app_user because "user" is a RESERVED WORD in PostgreSQL.
-- ---------------------------------------------------------------------------
CREATE TABLE app_user (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    -- BCrypt hash ONLY, never plaintext. $2a$10$... = algorithm$cost$salt+hash.
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,             -- CUSTOMER | TELLER | ADMIN
    customer_id   BIGINT,                            -- NULL for staff
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT fk_app_user_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT chk_app_user_role CHECK (role IN ('CUSTOMER', 'TELLER', 'ADMIN'))
);

-- ---------------------------------------------------------------------------
-- account: where money lives. balance is the CURRENT truth;
-- the transactions table is the HISTORY of how it got there.
-- ---------------------------------------------------------------------------
CREATE TABLE account (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id BIGINT        NOT NULL,
    type        VARCHAR(20)   NOT NULL,              -- CHECKING | SAVINGS
    balance     DECIMAL(15,2) NOT NULL DEFAULT 0,
    -- Optimistic-locking counter used by Hibernate @Version (stage 2):
    -- every UPDATE carries "WHERE version = <read version>" and bumps it.
    version     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT chk_account_type CHECK (type IN ('CHECKING', 'SAVINGS')),
    -- Business rule enforced by the DATABASE itself: no overdrafts. The Java
    -- entity checks first (nicer error message); this is the last line of
    -- defense that no buggy code path can bypass.
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
);
-- Index the FK column: joins/filters on customer_id are the hottest path
-- (every dashboard query). Postgres does NOT auto-index foreign keys!
CREATE INDEX idx_account_customer ON account (customer_id);

-- ---------------------------------------------------------------------------
-- transactions: append-only LEDGER. Rows are never updated or deleted —
-- corrections are new rows. A transfer writes TWO rows (DEBIT + CREDIT)
-- sharing one `reference` UUID so both halves can be found and reconciled.
-- (Plural table name: "transaction" would collide with SQL keywords in tools.)
-- ---------------------------------------------------------------------------
CREATE TABLE transactions (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id  BIGINT        NOT NULL,
    type        VARCHAR(10)   NOT NULL,              -- DEBIT | CREDIT
    amount      DECIMAL(15,2) NOT NULL,              -- always positive; type = direction
    description VARCHAR(200),
    reference   VARCHAR(36),                         -- correlates the two halves of a transfer
    occurred_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_tx_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT chk_tx_type CHECK (type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_tx_amount_positive CHECK (amount > 0)
);
-- Composite index matching the statement query:
--   WHERE account_id = ? ORDER BY occurred_at DESC  (Week 3: leftmost-prefix rule)
CREATE INDEX idx_tx_account_time ON transactions (account_id, occurred_at);

-- ---------------------------------------------------------------------------
-- loan: application + lifecycle  APPLIED -> APPROVED|REJECTED -> PAID_OFF
-- ---------------------------------------------------------------------------
CREATE TABLE loan (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id     BIGINT        NOT NULL,
    principal       DECIMAL(15,2) NOT NULL,
    annual_rate     DECIMAL(5,2)  NOT NULL,          -- e.g. 7.50 = 7.5% APR
    term_months     INT           NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'APPLIED',
    monthly_payment DECIMAL(15,2),                   -- computed at approval (annuity formula)
    applied_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_loan_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT chk_loan_status CHECK (status IN ('APPLIED', 'APPROVED', 'REJECTED', 'PAID_OFF')),
    CONSTRAINT chk_loan_principal CHECK (principal > 0),
    CONSTRAINT chk_loan_term CHECK (term_months BETWEEN 6 AND 360)
);
CREATE INDEX idx_loan_customer ON loan (customer_id);
CREATE INDEX idx_loan_status   ON loan (status);     -- teller queue: WHERE status='APPLIED'
