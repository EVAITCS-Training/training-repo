# Stage 1 — Database (Week 3: SQL Kickoff, SQL Depth, Project Kick-In)

The foundation everything else stands on. Here SecureBank exists only as
**SQL**: a schema, seed data, and five teaching scripts that walk from CRUD
to window functions to locking. Stage 2 maps these exact tables onto JPA
entities — keep `schema.sql` open next to `stage-2-monolith/src/main/java/
com/evaitcs/securebank/domain/` and the ORM stops being magic.

## Run it

```powershell
docker compose up -d                 # starts PostgreSQL 17 with schema+seed
docker compose exec db psql -U securebank -d securebank    # interactive psql
```

Reset to a pristine database (init scripts only run on an empty volume):

```powershell
docker compose down -v ; docker compose up -d
```

Run a teaching script inside the container:

```powershell
Get-Content queries\02-joins.sql | docker compose exec -T db psql -U securebank -d securebank
```

Or browse with any GUI (DBeaver, pgAdmin, DataGrip): `localhost:5432`,
db/user/password all `securebank`.

## Files

| File | What it teaches |
|---|---|
| `schema.sql` | DDL: tables, PKs, FKs, UNIQUE, CHECK constraints, indexes, comments. The single source of truth — stage 2 validates entities against it. |
| `seed.sql` | Deterministic demo world (Alice/Bob/Carol, accounts, ledger, loan) with REAL BCrypt password hashes so stage-2 login works against this DB. |
| `queries/01-basic-crud.sql` | SELECT/WHERE/ORDER/LIMIT, INSERT...RETURNING, UPDATE/DELETE discipline, constraints refusing bad data. |
| `queries/02-joins.sql` | INNER vs LEFT, anti-join, multi-hop joins, self-join transfer reconciliation, EXPLAIN intro. |
| `queries/03-aggregates-window.sql` | GROUP BY/HAVING, ledger↔balance reconciliation, running balances, RANK/LAG — the analytics toolkit. |
| `queries/04-transactions-locking.sql` | BEGIN/COMMIT/ROLLBACK by hand, lost-update race, `SELECT FOR UPDATE`, deadlock choreography, isolation levels. |
| `queries/05-indexes-explain.sql` | EXPLAIN (ANALYZE, BUFFERS), leftmost-prefix rule, functional indexes, unused-index hunting. |

## The demo world

```
customer:  1 Alice ── account 1 CHECKING 1500.00
                   └─ account 2 SAVINGS  5200.00     loan 1: 10,000 @7.5%/36mo APPLIED
           2 Bob   ── account 3 CHECKING  300.00
           3 Carol ── (no accounts — LEFT JOIN bait)
app_user:  alice/bob (CUSTOMER), teller, admin — password: password123
transactions: opening deposits, payroll, ATM, one correlated transfer pair
```

Every balance reconciles against its ledger rows — prove it with the
reconciliation query in `queries/03`.

## How this stage connects forward

- **advanced-java / 12-jdbc** (course module): reads these exact tables through RAW JDBC —
  `PreparedStatement` (the SQL-injection fix), `ResultSet` cursors, and transactions. It is
  the layer stage-2's Spring Data JPA hides. It needs this stage's Postgres up (`docker
  compose up -d`), degrades gracefully (tests skip, demos print a hint) when it is down, and
  is non-destructive (writes run in rolled-back transactions or temp tables).
- **Stage 2** (`stage-2-monolith`): each table becomes an `@Entity`
  (`customer` → `Customer.java`, ...). Boot's `postgres` profile points at
  this compose database with `ddl-auto: validate` — entity and table must agree
  or the app refuses to start.
- **Stage 4**: the same `postgres:17-alpine` service appears in the
  full-stack compose, now feeding the containerized backend.
- **Stage 5**: the database SPLITS — each microservice gets its own schema
  (`accountdb`, `paymentdb`, ...), which is why "database per service" is a
  decision you feel in the SQL layer first.

## Exercises for class

1. Add a `savings_goal` column to `account` (ALTER TABLE + default). What does
   stage 2's `validate` say the next time it boots? (Answer: refuses — then fix the entity.)
2. Give Carol an account and a transfer to Alice using ONLY SQL, keeping the
   ledger consistent (two rows, one reference).
3. Write the teller-queue query (`WHERE status = 'APPLIED' ORDER BY applied_at`)
   and explain which index serves it.
4. Reproduce the deadlock from `04` with two psql windows, then fix it with
   consistent lock ordering.
