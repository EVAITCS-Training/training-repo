-- ============================================================================
-- 03 · AGGREGATES & WINDOW FUNCTIONS (Week 3 — SQL Depth)
-- ============================================================================
-- Aggregates collapse MANY rows into ONE summary (SUM, COUNT, AVG...).
-- Window functions compute per-row values FROM a group WITHOUT collapsing —
-- both are everywhere in banking: totals, running balances, rankings.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- GROUP BY — "for EACH customer, total their balances"
-- Pipeline: FROM/JOIN -> WHERE (rows) -> GROUP BY (buckets) -> HAVING (buckets)
--           -> SELECT (columns/aggregates) -> ORDER BY.
-- ---------------------------------------------------------------------------
SELECT c.name,
       COUNT(a.id)    AS accounts,
       SUM(a.balance) AS total_balance
FROM customer c
JOIN account a ON a.customer_id = c.id
GROUP BY c.id, c.name
ORDER BY total_balance DESC;

-- RULE: every non-aggregated SELECT column must appear in GROUP BY.
-- ("select name but group by id" only works because id is the PK — functional
-- dependency; Postgres allows it, most other engines don't. Group explicitly.)

-- HAVING filters GROUPS (WHERE filters ROWS, before grouping):
SELECT c.name, SUM(a.balance) AS total
FROM customer c JOIN account a ON a.customer_id = c.id
GROUP BY c.id, c.name
HAVING SUM(a.balance) > 2000;            -- only wealthy buckets survive

-- ---------------------------------------------------------------------------
-- RECONCILIATION — does the ledger agree with the balances?
-- SUM with a CASE expression: credits positive, debits negative.
-- This single query is the heart of every fintech audit job.
-- ---------------------------------------------------------------------------
SELECT a.id,
       a.balance                                   AS stored_balance,
       SUM(CASE t.type WHEN 'CREDIT' THEN t.amount
                       WHEN 'DEBIT'  THEN -t.amount END) AS ledger_sum,
       a.balance = SUM(CASE t.type WHEN 'CREDIT' THEN t.amount
                                   WHEN 'DEBIT'  THEN -t.amount END) AS in_sync
FROM account a
JOIN transactions t ON t.account_id = a.id
GROUP BY a.id, a.balance;

-- ---------------------------------------------------------------------------
-- WINDOW FUNCTIONS — group-like math, row-level output.
--   SUM(...) OVER (PARTITION BY ... ORDER BY ...)
--     PARTITION BY = the GROUP BY that does NOT collapse
--     ORDER BY     = makes it RUNNING (cumulative) within each partition
-- ---------------------------------------------------------------------------

-- Running balance per account over time — the statement view banks show:
SELECT t.account_id,
       t.occurred_at,
       t.type,
       t.amount,
       SUM(CASE t.type WHEN 'CREDIT' THEN t.amount ELSE -t.amount END)
           OVER (PARTITION BY t.account_id ORDER BY t.occurred_at
                 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_balance
FROM transactions t
ORDER BY t.account_id, t.occurred_at;

-- RANK() — "the biggest transaction per account, medal style":
SELECT *
FROM (
    SELECT t.account_id, t.amount, t.description,
           RANK() OVER (PARTITION BY t.account_id ORDER BY t.amount DESC) AS rnk
    FROM transactions t
) ranked
WHERE rnk = 1;

-- ROW_NUMBER() vs RANK() vs DENSE_RANK():
--   ROW_NUMBER: 1,2,3,4 (never ties)      RANK: 1,2,2,4 (skips)
--   DENSE_RANK: 1,2,2,3 (no skip)
-- LAG/LEAD peek at neighboring rows (previous balance, day-over-day deltas):
SELECT t.account_id, t.occurred_at, t.amount,
       LAG(t.amount) OVER (PARTITION BY t.account_id ORDER BY t.occurred_at) AS prev_amount
FROM transactions t;

-- ---------------------------------------------------------------------------
-- WHY THIS MATTERS FOR THE APP: the stage-2 dashboard's "total across
-- accounts" is the GROUP BY above; the statement's paging is ORDER BY +
-- LIMIT/OFFSET (Pageable); monthly analytics jobs are window functions.
-- When Hibernate can't express it, repositories fall back to native SQL
-- built from exactly these queries.
-- ---------------------------------------------------------------------------
