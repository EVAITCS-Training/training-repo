-- ============================================================================
-- 04 · TRANSACTIONS & LOCKING (Week 3 SQL Depth + Week 5 Concurrency)
-- ============================================================================
-- ACID in the flesh. Everything TransferService.java does with @Transactional
-- and SELECT ... FOR UPDATE, you can perform BY HAND here first — do that and
-- the Java annotations stop being magic.
--
-- ACID recap:
--   Atomicity     all-or-nothing           (BEGIN ... COMMIT/ROLLBACK)
--   Consistency   constraints always hold  (FK/CHECK from schema.sql)
--   Isolation     concurrent txns don't interleave visibly  (isolation levels)
--   Durability    committed = survives power loss           (WAL on disk)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- ATOMICITY — the transfer, manually. psql runs statements one-by-one in
-- autocommit mode; BEGIN groups them into ONE unit of work.
-- ---------------------------------------------------------------------------
BEGIN;
    UPDATE account SET balance = balance - 100.00 WHERE id = 1;  -- debit alice checking
    UPDATE account SET balance = balance + 100.00 WHERE id = 2;  -- credit alice savings
    INSERT INTO transactions (account_id, type, amount, description, reference)
    VALUES (1, 'DEBIT',  100.00, 'Manual transfer out', 'manual-demo-0001'),
           (2, 'CREDIT', 100.00, 'Manual transfer in',  'manual-demo-0001');
COMMIT;      -- <- nothing is visible to anyone else until THIS line

-- Undo-able version — run it, inspect, roll back (nothing changed):
BEGIN;
    UPDATE account SET balance = 0 WHERE id = 3;
    SELECT id, balance FROM account;            -- looks catastrophic...
ROLLBACK;                                       -- ...but never happened.
SELECT id, balance FROM account;                -- bob's 300.00 is intact

-- MID-TRANSACTION FAILURE = automatic all-or-nothing. If the second UPDATE
-- below violates chk_balance_non_negative, the FIRST update is also undone:
BEGIN;
    UPDATE account SET balance = balance + 9999 WHERE id = 2;     -- fine on its own
    UPDATE account SET balance = -1 WHERE id = 1;                 -- CHECK violation!
    -- psql: ERROR -> the transaction is now aborted; COMMIT would refuse:
    -- "current transaction is aborted, commands ignored until end of block"
ROLLBACK;
SELECT balance FROM account WHERE id = 2;       -- the +9999 never landed. THAT is atomicity.

-- ---------------------------------------------------------------------------
-- ISOLATION & RACES — the lost-update problem.
-- Open TWO psql sessions (session A, session B) and interleave:
--
--   A: BEGIN;
--   A: SELECT balance FROM account WHERE id = 1;        -> 1500.00
--   B: BEGIN;
--   B: SELECT balance FROM account WHERE id = 1;        -> 1500.00 (same!)
--   A: UPDATE account SET balance = balance - 100 WHERE id = 1;
--   B: UPDATE account SET balance = balance - 100 WHERE id = 1;   <- BLOCKS here
--   A: COMMIT;                                          -- B unblocks, re-reads,
--   B: COMMIT;                                          -- result: 1300.00 (correct!)
--
-- Because "balance = balance - 100" is computed at UPDATE time against the
-- CURRENT row, arithmetic updates stay correct (B waited for A). The lost
-- update happens with READ-MODIFY-WRITE in application code instead:
--   both read 1500 -> both compute 1400 -> both write 1400 -> one -100 vanished.
-- That is the bug @Version (optimistic lock) and FOR UPDATE (pessimistic lock)
-- exist to kill.
-- ---------------------------------------------------------------------------

-- PESSIMISTIC LOCK — what TransferService.findByIdForUpdate runs:
BEGIN;
    SELECT * FROM account WHERE id = 1 FOR UPDATE;   -- row is now LOCKED by me
    -- Session B running ANY of: FOR UPDATE / UPDATE / DELETE on this row
    -- will BLOCK until I COMMIT or ROLLBACK. Reads without FOR UPDATE still work.
    UPDATE account SET balance = balance - 50 WHERE id = 1;
COMMIT;                                              -- lock released

-- DEADLOCK — two sessions locking the same two rows in OPPOSITE order:
--   A: SELECT * FROM account WHERE id = 1 FOR UPDATE;
--   B: SELECT * FROM account WHERE id = 2 FOR UPDATE;
--   A: SELECT * FROM account WHERE id = 2 FOR UPDATE;   <- waits for B
--   B: SELECT * FROM account WHERE id = 1 FOR UPDATE;   <- waits for A. Deadlock!
-- Postgres detects it after ~1s and ABORTS one transaction (error 40P01).
-- TransferService prevents it entirely: always lock min(id) before max(id).
-- Consistent lock ordering = the cheapest deadlock fix in existence.

-- ---------------------------------------------------------------------------
-- ISOLATION LEVELS (set per transaction):
--   READ COMMITTED (Postgres default): each statement sees freshly committed
--     data; no dirty reads. Good enough for ~everything here.
--   REPEATABLE READ: the whole transaction sees ONE snapshot; serialization
--     failures must be retried by the app.
--   SERIALIZABLE: full isolation; Postgres aborts conflicting txns (retry!).
-- Higher level = fewer anomalies, less concurrency. Banking cores often run
-- READ COMMITTED + explicit FOR UPDATE (exactly what stage-2 does).
-- ---------------------------------------------------------------------------
BEGIN ISOLATION LEVEL REPEATABLE READ;
    SELECT SUM(balance) FROM account;     -- snapshot pinned for this txn
    -- ...another session moves money; my SUM stays frozen until COMMIT
COMMIT;
