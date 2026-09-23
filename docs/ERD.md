# SecureBank ERD — the data model every stage agrees on

The single source of truth is `stage-1-database/schema.sql`. This document is
its picture; stage-2's JPA entities and stage-5's per-service databases are
its projections.

## Diagram

```
┌────────────────────┐        ┌────────────────────────────┐
│      customer      │        │          app_user          │   (auth context)
├────────────────────┤        ├────────────────────────────┤
│ id          PK     │◄──┐    │ id            PK           │
│ name               │   │    │ username      UNIQUE       │
│ email       UNIQUE │   └────┤ customer_id   FK NULL ─────┼──► staff rows have NULL
│ created_at         │        │ password_hash (BCrypt)     │
└─────────┬──────────┘        │ role (CUSTOMER|TELLER|ADMIN)│
          │ 1                 └────────────────────────────┘
          │
          │ *
┌─────────▼──────────┐ 1    * ┌────────────────────────────┐
│      account       ├───────┤        transactions        │
├────────────────────┤       ├────────────────────────────┤
│ id          PK     │       │ id            PK           │
│ customer_id FK ──────►    │ account_id    FK ──────────┼──► account.id
│ type  CHECKING|SAV │       │ type          DEBIT|CREDIT │
│ balance DECIMAL(15,2)      │ amount        > 0 always   │
│   CHECK >= 0       │       │ description                │
│ version (optimistic│       │ reference  ◄── correlation │──► same UUID on both
│   lock counter)    │       │ occurred_at                │    halves of a transfer
└────────────────────┘       └────────────────────────────┘

┌────────────────────┐        ┌────────────────────────────┐
│        loan        │        │          transfer          │  (stage 5 only,
├────────────────────┤        ├────────────────────────────┤   payment-service)
│ id          PK     │        │ id            PK           │
│ customer_id FK ────┼──►     │ reference     UNIQUE       │──► "xfer-<uuid>"
│ principal          │        │ from_account_id  (value!)  │──► account.id in ANOTHER db
│ annual_rate        │        │ to_account_id    (value!)  │
│ term_months        │        │ amount                     │
│ status APPLIED|    │        │ status PENDING|COMPLETED|  │
│   APPROVED|REJECTED│        │   FAILED|COMPENSATED|      │
│   |PAID_OFF        │        │   COMPENSATION_FAILED      │
│ monthly_payment    │        │ reason                     │
│ applied_at         │        │ initiator_customer_id      │
└────────────────────┘        │ created_at / updated_at    │
                              └────────────────────────────┘
```

## Ownership by stage

| Table | Stage 1-4 (monolith db) | Stage 5 (database per service) |
|---|---|---|
| customer | `securebank.customer` | `accountdb.customer` |
| account | `securebank.account` | `accountdb.account` |
| transactions | `securebank.transactions` | `accountdb.transactions` |
| app_user | `securebank.app_user` | `authdb.app_user` |
| loan | `securebank.loan` | `loandb.loan` |
| transfer | — | `paymentdb.transfer` (new in stage 5) |

## Invariants (enforced WHERE, and why)

| Rule | Enforced by | Layer |
|---|---|---|
| balance never negative | `CHECK (balance >= 0)` | database (last line of defense) |
| balance never negative | `Account.withdraw()` guard | domain (clear error message) |
| ledger matches balance | seed discipline + reconciliation query (`queries/03`) | operational |
| transfer amounts positive | `@DecimalMin` on DTOs | API edge (400 before any work) |
| one login per username | `UNIQUE (username)` | database |
| plaintext passwords never stored | BCrypt at write time | application |
| ownership: token cid == row's customer | service-layer checks | application (OWASP broken access control) |
| saga never loses money | compensation + COMPENSATION_FAILED alarm | payment-service |

## Cross-context references (stage 5)

- `app_user.customer_id`, `loan.customer_id`, `transfer.from/to_account_id`
  are **values, not foreign keys** — the referenced rows live in another
  service's database. Consequences taught explicitly: no joins, no FK
  integrity, reconciliation via ids/events, seeders must agree on ids.
