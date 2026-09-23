# Architecture — how the five stages relate, and how a request flows

This repo tells ONE story five times, each stage adding a layer of reality:

```
stage-1  SQL only            schema + seed + query lessons
stage-2  monolith            one Spring Boot app: all contexts, one DB, one deployable
stage-3  frontend            React SPA that consumes stage-2's (or stage-5's!) API
stage-4  containers + CI     the monolith + db + nginx as images; pipeline guards main
stage-5  microservices       the monolith split along its own package seams
infra    production shapes   K8s + Terraform answers to "but in real life...?"
```

The crucial design decision: **stage-5 is not a rewrite.** It is stage-2's
packages promoted to services. `security/` → auth-service, accounts →
account-service, transfers → payment-service (now a saga), loans →
loan-service. Open both side by side and the diff IS the lecture.

## Stage 2 — modular monolith

```
browser ──> Spring Boot :8080
             │ SecurityFilterChain (JWT verify)
             │ controllers (DTOs, validation, ProblemDetail errors)
             │ services (@Transactional rules; TransferService = ACID transfer)
             │ repositories (Spring Data JPA)
             └─> PostgreSQL (H2 in dev) — one schema, FKs everywhere
```

Everything is a method call. Failures are exceptions. Atomicity is free.

## Stage 5 — the same system, distributed

```
browser ──> nginx ──> api-gateway :8080 ──(Eureka lb://)──┐
                        │ Path predicates, StripPrefix     │
        ┌───────────────┼──────────────┬──────────────┬────┘
        v               v              v              v
   auth-service    account-service  payment-service  loan-service
   (mint JWT)      (money+ledger)   (saga orchestr.) (lending)
        │               ▲              │  REST /internal/**   │
        │               └──────────────┤  (Resilience4j)      │
        │                              v                      v
        │                        Kafka: transfers, loans ──> notification-service
        └── config-server (shared secret, eureka url, per-service cfg)
            discovery-service (Eureka registry)
            Postgres: authdb | accountdb | paymentdb | loandb   Kafka (KRaft)
```

### What changed, concept by concept

| Concept | Monolith | Microservices |
|---|---|---|
| Identity | SecurityConfig verifies JWT | same, but now in EVERY service (duplicated kit) |
| Call to "accounts" | `accountRepository.findById(...)` | HTTP via `LoadBalancerClient` + retries + circuit breaker |
| Transfer atomicity | one `@Transactional` | saga: debit → credit → compensate; states persisted |
| Consistency | immediate (ACID) | eventual (between saga steps; via events) |
| Config | application.yml | Config Server (shared + per-service), env overrides |
| Addressing | package names | Eureka service names / k8s DNS |
| Failure | exception | 4xx/5xx + timeouts + circuit states — typed and handled |
| Audit trail | one transactions table | same table + `reference` correlation across services/events |
| Deploy | one jar | 8 images; each service deploys independently |
| Test | 18 tests, one context | 28 tests, each service standalone (no infra needed) |

### The request walkthrough (transfer, stage 5)

1. React `api/client.ts` attaches `Authorization: Bearer <jwt>` (from login).
2. nginx (frontend image) proxies `/api/*` → gateway (network alias `backend`).
3. Gateway matches `Path=/api/transfers/**`, strips `/api`, resolves
   `lb://payment-service` via Eureka, forwards.
4. payment-service's SecurityConfig verifies the JWT (shared HS256 secret
   from Config Server); controller extracts `cid` claim.
5. Saga persists `transfer` row PENDING (reference `xfer-<uuid>`).
6. `AccountClient.view()` → ownership check against account-service's
   `/internal/accounts/{id}` (network-only authorization; no gateway route).
7. `debit` (retry → circuit breaker → pessimistic lock in account-service,
   ledger row written) — business refusal (400) ends the saga as FAILED.
8. `credit` — on technical failure: compensating credit with reference
   `-comp`, state COMPENSATED (or COMPENSATION_FAILED = human alarm).
9. State COMPLETED; `TransferCompleted` published to Kafka (key = reference
   → partition ordering).
10. notification-service consumes, stores a receipt; `GET /api/notifications`
    shows it. The frontend never knew any of this happened.

### Deliberate simplifications (named in code, repeated here)

- HS256 shared secret (all services could mint tokens) — RS256 is the
  production answer.
- One Postgres container, four databases — logical boundary correct, blast
  radius not; production = instance per service (see infra/terraform).
- `/internal/**` guarded by routing absence in compose; closed properly by
  NetworkPolicies in infra/k8s.
- No idempotency keys / transactional outbox — both patterns are named in
  comments where they would live; retries-after-timeout can double-apply.
- Config Server native (folder) backend instead of Git.
