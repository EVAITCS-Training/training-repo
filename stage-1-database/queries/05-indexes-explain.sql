-- ============================================================================
-- 05 · INDEXES & EXPLAIN — reading the database's mind (Week 3 + Week 8 perf)
-- ============================================================================
-- An index is a sorted side-structure (B-tree by default) that turns
-- "scan every row" into "binary-search to the row". Cost: every INSERT/UPDATE
-- maintains it. Index what you FILTER and JOIN and SORT by — nothing else.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- EXPLAIN (ANALYZE, BUFFERS) — the query plan + REAL timings.
--   EXPLAIN          = plan only (doesn't run the query)
--   ANALYZE          = actually executes and measures
-- Read the tree BOTTOM-UP: innermost nodes run first.
-- ---------------------------------------------------------------------------
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM transactions WHERE account_id = 1 ORDER BY occurred_at DESC;
-- With idx_tx_account_time present you should see:
--   Index Scan Backward using idx_tx_account_time
-- Without it (DROP INDEX idx_tx_account_time;) you'd see:
--   Seq Scan on transactions  +  Sort   <- full table read, then sort in memory
-- Recreate it afterwards:
--   CREATE INDEX idx_tx_account_time ON transactions (account_id, occurred_at);

-- ---------------------------------------------------------------------------
-- LEFTMOST-PREFIX RULE — composite index (a, b) serves queries on:
--   WHERE a = ?            YES        WHERE a = ? AND b > ?   YES
--   WHERE b = ?            NO (b is not leftmost — index unusable)
-- That's why the statement index is (account_id, occurred_at) and not the
-- reverse: the filter is always "one account", the sort is time.
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- SEE WHAT EXISTS — \d in psql lists indexes per table; or:
-- ---------------------------------------------------------------------------
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename IN ('account', 'transactions', 'loan', 'customer', 'app_user')
ORDER BY tablename, indexname;

-- ---------------------------------------------------------------------------
-- WHEN INDEXES DON'T HELP
--  * tiny tables (seq scan of 5 rows beats any index) — the planner knows
--  * low-selectivity columns (role with 3 values, boolean flags)
--  * expressions that hide the column:  WHERE lower(email) = 'x'  needs a
--    FUNCTIONAL index:  CREATE INDEX ON customer (lower(email));
--  * leading wildcards:  LIKE '%term'   (B-trees sort from the left)
-- Always MEASURE with EXPLAIN ANALYZE; intuition about plans is often wrong.
-- ---------------------------------------------------------------------------

-- Functional index demo:
CREATE INDEX IF NOT EXISTS idx_customer_lower_email ON customer (lower(email));
EXPLAIN (ANALYZE) SELECT * FROM customer WHERE lower(email) = 'alice@securebank.dev';

-- ---------------------------------------------------------------------------
-- PRODUCTION NOTES (Week 8/9 hardening):
--  * CREATE INDEX CONCURRENTLY — builds without blocking writes (slower build,
--    zero downtime; cannot run inside a transaction block).
--  * Unused indexes are pure write-amplification. Find them:
--      SELECT relname, indexrelname, idx_scan FROM pg_stat_user_indexes
--      ORDER BY idx_scan ASC;      -- idx_scan = 0 since last reset = suspect
--  * Postgres must ANALYZE tables so the planner knows statistics; autovacuum
--    does it continuously — another reason never to disable autovacuum.
-- ---------------------------------------------------------------------------
SELECT relname AS table, indexrelname AS index, idx_scan AS times_used
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC;
