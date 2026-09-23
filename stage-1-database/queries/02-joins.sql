-- ============================================================================
-- 02 · JOINS — following the foreign-key links (Week 3 — SQL Depth)
-- ============================================================================
-- A relational database splits truth across tables; JOINs reassemble it.
-- Every join below walks a FK from schema.sql — draw the ERD (docs/ERD.md)
-- next to this file and trace each arrow as you read.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- INNER JOIN — only rows that have a match on BOTH sides.
-- "Each account WITH its owner": account.customer_id -> customer.id
-- ---------------------------------------------------------------------------
SELECT a.id AS account_id, a.type, a.balance, c.name AS owner
FROM account a
INNER JOIN customer c ON c.id = a.customer_id;
-- INNER is the default;  JOIN == INNER JOIN.
-- Alias discipline (a, c): short, consistent, used on EVERY column. Ambiguous
-- column names without aliases are the #1 join beginner error.

-- ---------------------------------------------------------------------------
-- LEFT JOIN — keep ALL left rows, NULLs where the right side has no match.
-- "Every customer, even those WITHOUT accounts" (Carol!):
-- ---------------------------------------------------------------------------
SELECT c.name, a.id AS account_id, a.balance
FROM customer c
LEFT JOIN account a ON a.customer_id = c.id;
-- Alice appears twice (two accounts), Carol once with NULLs.

-- The classic interview query — "customers who have NO account":
-- left join + filter for the NULL side (an "anti-join"):
SELECT c.name, c.email
FROM customer c
LEFT JOIN account a ON a.customer_id = c.id
WHERE a.id IS NULL;                      -- Carol only
-- Equivalent: WHERE NOT EXISTS (SELECT 1 FROM account x WHERE x.customer_id = c.id)

-- ---------------------------------------------------------------------------
-- MULTI-HOP JOIN — transactions -> account -> customer.
-- "Who made each transaction?" Follows two FKs in one query:
-- ---------------------------------------------------------------------------
SELECT t.occurred_at, t.type, t.amount, t.description,
       a.type AS account_type, c.name AS customer
FROM transactions t
JOIN account  a ON a.id = t.account_id
JOIN customer c ON c.id = a.customer_id
ORDER BY t.occurred_at DESC;

-- ---------------------------------------------------------------------------
-- SELF JOIN — a table joined to itself. Pair the two halves of every
-- transfer using the shared `reference` correlation id:
-- ---------------------------------------------------------------------------
SELECT d.reference,
       d.amount        AS debited,
       cr.amount       AS credited,
       d.account_id    AS from_account,
       cr.account_id   AS to_account
FROM transactions d
JOIN transactions cr
     ON cr.reference = d.reference      -- same transfer
    AND cr.type = 'CREDIT'
WHERE d.type = 'DEBIT'
  AND d.reference IS NOT NULL;
-- This is exactly how stage-2's TransferService.correlation design pays off:
-- reconciliation is a self-join on the reference column.

-- ---------------------------------------------------------------------------
-- JOIN + WHERE + ORDER — the statement query from AccountController:
-- ---------------------------------------------------------------------------
SELECT t.occurred_at, t.type, t.amount, t.description
FROM transactions t
JOIN account a ON a.id = t.account_id
WHERE a.customer_id = 1                  -- alice
ORDER BY t.occurred_at DESC;

-- ---------------------------------------------------------------------------
-- VISUALIZING EXECUTION: EXPLAIN shows the join strategy Postgres picked
-- (nested loop / hash join / merge join) — deep-dive in 05-indexes-explain.sql.
-- ---------------------------------------------------------------------------
EXPLAIN
SELECT c.name, a.balance
FROM account a JOIN customer c ON c.id = a.customer_id;
