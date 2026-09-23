-- ============================================================================
-- 01 · BASIC CRUD (Week 3 — SQL Kickoff)
-- ============================================================================
-- Run me:  docker compose exec db psql -U securebank -d securebank
-- then:    \i /queries/01-basic-crud.sql   (or paste statements one by one)
--
-- CRUD = Create, Read, Update, Delete — the four verbs every data app lives
-- on. In stage-2 these same operations happen through Spring Data JPA;
-- learning them raw first is what lets you READ the SQL Hibernate prints
-- (spring.jpa.show-sql=true) instead of trusting it blindly.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- READ — SELECT anatomy:  SELECT <columns> FROM <table> WHERE <rows> ORDER BY <sort> LIMIT <n>
-- Execution order is NOT reading order: FROM -> WHERE -> ORDER BY -> LIMIT -> SELECT.
-- Understanding that order explains why you can't filter on a SELECT alias
-- (WHERE runs BEFORE the alias exists).
-- ---------------------------------------------------------------------------
SELECT id, name, email FROM customer;

-- WHERE filters ROWS; operators: = <> > < >= <=, AND/OR/NOT, BETWEEN, IN, LIKE
SELECT id, type, balance FROM account WHERE balance > 400;
SELECT * FROM customer WHERE email LIKE '%securebank.dev';        -- % = any chars
SELECT * FROM account WHERE type IN ('CHECKING', 'SAVINGS') AND id <> 99;

-- ORDER BY + LIMIT = "top N" (pagination's raw form — stage 2's Pageable
-- generates exactly this SQL):
SELECT id, balance FROM account ORDER BY balance DESC LIMIT 2;

-- SELECT * is fine while learning; in production name your columns
-- (schema changes stop breaking clients, and less data moves).

-- ---------------------------------------------------------------------------
-- CREATE — INSERT. Omit id: the IDENTITY column generates it.
-- RETURNING gives you the generated values back in the same round-trip
-- (this is exactly what Hibernate uses to learn the id of a saved entity).
-- ---------------------------------------------------------------------------
INSERT INTO customer (name, email)
VALUES ('Demo Person', 'demo@securebank.dev')
RETURNING id, created_at;

INSERT INTO account (customer_id, type, balance)
SELECT id, 'SAVINGS', 42.00 FROM customer WHERE email = 'demo@securebank.dev'
RETURNING id;                                   -- INSERT ... SELECT: rows from a query

-- Constraint demo — watch the database REFUSE bad data (defense in depth):
-- INSERT INTO account (customer_id, type, balance) VALUES (1, 'CHECKING', -5);
--   -> ERROR: chk_balance_non_negative  (CHECK constraint)
-- INSERT INTO customer (name, email) VALUES ('Dupe', 'alice@securebank.dev');
--   -> ERROR: uq_customer_email         (UNIQUE constraint)

-- ---------------------------------------------------------------------------
-- UPDATE — always write the matching SELECT FIRST, verify the row set, then
-- flip the verb. Updating the wrong rows is the classic production accident.
-- ---------------------------------------------------------------------------
SELECT * FROM customer WHERE email = 'demo@securebank.dev';       -- 1) who will be hit?
UPDATE customer SET name = 'Demo Person Jr.'
WHERE email = 'demo@securebank.dev';                              -- 2) hit only those

-- An UPDATE with NO WHERE updates EVERY ROW. There is no Ctrl+Z.
-- (psql users: run inside BEGIN; ... ROLLBACK; while practicing.)

-- ---------------------------------------------------------------------------
-- DELETE — same discipline. Note the FK consequence: deleting a customer
-- that owns accounts FAILS (fk_account_customer) unless the accounts go first.
-- Referential integrity means orphan rows are impossible, by construction.
-- ---------------------------------------------------------------------------
DELETE FROM account       WHERE customer_id = (SELECT id FROM customer WHERE email = 'demo@securebank.dev');
DELETE FROM customer      WHERE email = 'demo@securebank.dev';
-- DELETE FROM customer WHERE email = 'alice@securebank.dev';
--   -> ERROR: still referenced by account/app_user/loan rows (as intended!)

-- ---------------------------------------------------------------------------
-- psql meta-commands (the tool, not SQL):
--   \dt            list tables            \d account     describe one table
--   \x             expanded output        \i file.sql    run a script
--   \timing        show execution time    \?             help
-- ---------------------------------------------------------------------------
