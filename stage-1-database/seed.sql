-- ============================================================================
-- SECUREBANK SEED DATA — the demo world every lecture example runs against.
-- ============================================================================
-- Deterministic rows (identities assign ids in insertion order):
--   customers: 1=Alice, 2=Bob, 3=Carol
--   users:     1=alice, 2=bob, 3=teller, 4=admin   (password: password123)
--   accounts:  1=Alice CHECKING 1500.00, 2=Alice SAVINGS 5200.00, 3=Bob CHECKING 300.00
--   loans:     1=Alice 10,000 @7.5% 36mo APPLIED (teller approves this in class)
--   transactions: opening deposits + a small history for statement demos
--
-- These mirror config/DataSeeder.java in stage-2 (same ids, same final
-- balances; the SQL ledger is slightly richer for query practice) so the H2
-- dev mode and the Postgres mode show the same demo world.
--
-- FK LOOKUP PATTERN: rows reference each other by SELECTing the natural key
-- (email/username) instead of hardcoding ids — the seed still works if you
-- reorder inserts. (Alternative: data-modifying CTEs — shown at the bottom.)
-- ============================================================================

-- --- Customers ---------------------------------------------------------------
INSERT INTO customer (name, email) VALUES
    ('Alice Nguyen', 'alice@securebank.dev'),
    ('Bob Rivera',   'bob@securebank.dev'),
    -- Carol has NO accounts — she exists for LEFT JOIN demos (queries/02):
    -- "which customers have never opened an account?" needs a customer with none.
    ('Carol Okafor', 'carol@securebank.dev');

-- --- Login identities ---------------------------------------------------------
-- password_hash is a REAL BCrypt hash of 'password123' (cost 10) generated with
-- Spring Security's encoder — the stage-2 app running on the postgres profile
-- can log in with these rows as-is. Each hash embeds its own random salt, so
-- hashing 'password123' again yields a DIFFERENT string that still verifies.
INSERT INTO app_user (username, password_hash, role, customer_id) VALUES
    ('alice',  '$2a$10$a5Joa76lCChBNq.jzinPs.CV2l1lSwgnEbjThHlgK7PeciYaNcDtG', 'CUSTOMER',
     (SELECT id FROM customer WHERE email = 'alice@securebank.dev')),
    ('bob',    '$2a$10$a5Joa76lCChBNq.jzinPs.CV2l1lSwgnEbjThHlgK7PeciYaNcDtG', 'CUSTOMER',
     (SELECT id FROM customer WHERE email = 'bob@securebank.dev')),
    ('teller', '$2a$10$a5Joa76lCChBNq.jzinPs.CV2l1lSwgnEbjThHlgK7PeciYaNcDtG', 'TELLER', NULL),
    ('admin',  '$2a$10$a5Joa76lCChBNq.jzinPs.CV2l1lSwgnEbjThHlgK7PeciYaNcDtG', 'ADMIN',  NULL);

-- --- Accounts -----------------------------------------------------------------
INSERT INTO account (customer_id, type, balance) VALUES
    ((SELECT id FROM customer WHERE email = 'alice@securebank.dev'), 'CHECKING', 1500.00),
    ((SELECT id FROM customer WHERE email = 'alice@securebank.dev'), 'SAVINGS',  5200.00),
    ((SELECT id FROM customer WHERE email = 'bob@securebank.dev'),   'CHECKING',  300.00);

-- --- Opening ledger rows (every balance has matching history!) ----------------
-- RECONCILIATION RULE: SUM(credits) - SUM(debits) per account MUST equal
-- account.balance. Verify it with queries/03-aggregates-window.sql.
--   Alice CHECKING (id 1): 1200 + 500 - 200        = 1500.00  ✓
--   Alice SAVINGS  (id 2): 5000 + 200              = 5200.00  ✓
--   Bob   CHECKING (id 3): 500 - 200               =  300.00  ✓
INSERT INTO transactions (account_id, type, amount, description, reference, occurred_at) VALUES
    -- Alice checking: opening + payroll - transfer-out
    ((SELECT id FROM account WHERE type = 'CHECKING'
       AND customer_id = (SELECT id FROM customer WHERE email = 'alice@securebank.dev')),
     'CREDIT', 1200.00, 'Opening deposit', NULL, now() - INTERVAL '30 days'),
    ((SELECT id FROM account WHERE type = 'CHECKING'
       AND customer_id = (SELECT id FROM customer WHERE email = 'alice@securebank.dev')),
     'CREDIT',  500.00, 'Payroll', NULL, now() - INTERVAL '15 days'),
    -- Alice savings: opening + transfer-in
    ((SELECT id FROM account WHERE type = 'SAVINGS'
       AND customer_id = (SELECT id FROM customer WHERE email = 'alice@securebank.dev')),
     'CREDIT', 5000.00, 'Opening deposit', NULL, now() - INTERVAL '30 days'),
    -- Bob checking: opening - ATM
    ((SELECT id FROM account WHERE customer_id = (SELECT id FROM customer WHERE email = 'bob@securebank.dev')),
     'CREDIT',  500.00, 'Opening deposit', NULL, now() - INTERVAL '30 days'),
    ((SELECT id FROM account WHERE customer_id = (SELECT id FROM customer WHERE email = 'bob@securebank.dev')),
     'DEBIT',   200.00, 'ATM withdrawal', NULL, now() - INTERVAL '5 days'),
    -- A completed transfer = TWO rows sharing ONE reference (correlation id):
    ((SELECT id FROM account WHERE type = 'CHECKING'
       AND customer_id = (SELECT id FROM customer WHERE email = 'alice@securebank.dev')),
     'DEBIT',   200.00, 'Transfer out to savings', 'seed-transfer-0001', now() - INTERVAL '10 days'),
    ((SELECT id FROM account WHERE type = 'SAVINGS'
       AND customer_id = (SELECT id FROM customer WHERE email = 'alice@securebank.dev')),
     'CREDIT',  200.00, 'Transfer in from checking', 'seed-transfer-0001', now() - INTERVAL '10 days');

-- --- One loan awaiting the teller's decision -----------------------------------
INSERT INTO loan (customer_id, principal, annual_rate, term_months, status) VALUES
    ((SELECT id FROM customer WHERE email = 'alice@securebank.dev'), 10000.00, 7.50, 36, 'APPLIED');

-- ============================================================================
-- BONUS PATTERN — data-modifying CTEs: INSERT ... RETURNING feeds the next
-- statement without subselects. Shown here for teaching; the style above is
-- friendlier to beginners.
--
--   WITH c AS (
--       INSERT INTO customer (name, email) VALUES ('Demo Person', 'demo@securebank.dev')
--       RETURNING id
--   )
--   INSERT INTO account (customer_id, type, balance)
--   SELECT id, 'SAVINGS', 0 FROM c;
-- ============================================================================
