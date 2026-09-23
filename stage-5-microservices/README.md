# Stage 5 — SecureBank Microservices (Weeks 7-9: REST→Microservices, Resilience, Messaging, Cloud)

The stage-2 monolith, decomposed along its bounded contexts into **8 deployable
services** on Spring Boot 4.1.1 + Spring Cloud 2025.1.3 (Oakwood): Eureka
discovery, Config Server, MVC API gateway, Kafka events, Resilience4j, and an
orchestrated transfer **saga** with compensation. The stage-3 frontend runs
against it **completely unchanged**.

Read [`docs/refs/SPRING_CLOUD_2025.1_CHEATSHEET.md`](../docs/refs/SPRING_CLOUD_2025.1_CHEATSHEET.md)
before touching framework code — Boot 4/Cloud 2025.1 renamed starters and
moved property namespaces (gateway routes are NOT under `spring.cloud.gateway.routes` anymore).

## The architecture

```
                         ┌─────────────────────────────────────────────────┐
 browser ──> frontend    │              compose network                    │
             (nginx)     │                                                  │
            :8090 ───────┼──/api──> api-gateway :8080                      │
                         │             │  routes by Path, StripPrefix=1     │
                         │             │  uri lb://<service> (Eureka lookup)│
                         │   ┌─────────┼──────────┬───────────┬──────────┐  │
                         │   v         v          v           v          v  │
                         │ auth     account   payment       loan   notification
                         │ :8081    :8082     :8083         :8084      :8085│
                         │   │        │          │  saga     │          ^   │
                         │   v        v          │  REST ────┘          │   │
                         │ authdb  accountdb     v          loandb   Kafka   │
                         │                   paymentdb   ("transfers","loans")
                         │                                                  │
                         │ discovery-service :8761 (Eureka registry)        │
                         │ config-server :8888 (serves ./config-repo)       │
                         │ db: PostgreSQL 17 (4 databases) · kafka: KRaft   │
                         └─────────────────────────────────────────────────┘
```

| Service | Context | Owns | Calls | Events |
|---|---|---|---|---|
| discovery-service | infra | Eureka registry | — | — |
| config-server | infra | `config-repo/` YAML | — | — |
| api-gateway | edge | route table | all (lb://) | — |
| auth-service | Identity | `authdb.app_user` | — | — |
| account-service | Accounts | `accountdb` (customer, account, transactions) | — | — |
| payment-service | Payments | `paymentdb.transfer` | account-service (saga) | publishes `transfers` |
| loan-service | Lending | `loandb.loan` | — | publishes `loans` |
| notification-service | Engagement | in-memory store | — | consumes both topics |

## Run it

```powershell
docker compose up -d --build        # builds 8 images; full boot ~2-3 min
docker compose ps                   # wait until every service is healthy
```

| Entry point | URL |
|---|---|
| **Frontend (stage-3, unchanged)** | http://localhost:8090 — login alice/password123 |
| API gateway | http://localhost:8080/api/... |
| Eureka dashboard | http://localhost:8761 |
| Config Server | http://localhost:8888/payment-service/default |
| Gateway route table | http://localhost:8080/actuator/gateway/routes |

Watch the event pipeline live (Kafka is intentionally not host-exposed):

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic transfers --from-beginning
```

...then make a transfer in the UI and see `TransferCompleted` arrive, then
`GET /api/notifications` (with a Bearer token) to see notification-service's
receipt. `docker compose down -v` wipes everything.

## The class demo script (~20 min)

1. **Eureka first**: open :8761, watch registrations trickle in as services
   boot. Kill one (`docker compose stop loan-service`) — it disappears after
   lease expiry; the gateway's `lb://loan-service` starts answering 503.
2. **Config Server**: `curl :8888/payment-service/default` — show the merged
   shared+service YAML. Edit `config-repo/payment-service.yml`, restart
   payment-service, show the new value live. One file, eight consumers.
3. **Gateway**: `GET /actuator/gateway/routes`; trace one UI click through
   nginx → gateway predicate → StripPrefix → Eureka lookup → service. Show
   that `/internal/**` has NO route (the perimeter).
4. **The saga, happy path**: transfer in the UI; read payment-service logs
   (debit → credit → COMPLETED → Kafka publish); see the `xfer-<uuid>`
   reference in account-service's ledger rows via the statement screen.
5. **The saga, failure**: `docker compose stop account-service` mid-lecture,
   attempt a transfer → retries → circuit opens → 503 `Account service
   unavailable`; `GET :8083/actuator/health` shows the circuit state. Restart
   account-service; half-open probe succeeds; transfers work again. This is
   resilience as a *behavior you can see*, not a slide.
6. **Events**: approve a loan as teller; watch `LoanApproved` on the console
   consumer; open notifications. Stop notification-service, approve another
   loan, restart it — the consumer catches up (Kafka retention = the "async
   decoupling" argument made visible).

## Tests

```powershell
.\mvnw.cmd test        # per service; or from this folder:
foreach ($s in Get-ChildItem -Directory -Filter *-service,config-server,api-gateway) { ... }
```

28 tests: saga branch coverage (payment: happy / business refusal /
compensated / COMPENSATION_FAILED / ownership / guard rails), JWT contract
(auth: token minting+claims), public+internal API surfaces (account),
annuity math & state machine (loan), listener parsing incl. poison pills
(notification), context loads (discovery/config/gateway). Every service tests
**without infrastructure** — H2, Eureka off, config client off, Kafka mocked
or auto-startup disabled (see each `src/test/resources/application.yml`).

## What this stack deliberately cheats on (lecture: name the gaps)

- **One Postgres container, four databases** — production is one instance per
  service (the logical boundary here is identical; the blast radius isn't).
- **HS256 shared secret** — every service could MINT tokens. Production:
  RS256 keypair; only auth-service holds the private key.
- **`/internal/**` protected by routing absence** — production adds
  NetworkPolicies + mTLS (see `infra/k8s`).
- **No idempotency keys / outbox** — retries after timeouts can double-apply;
  events can be lost in the commit→publish window. Both patterns are named
  in code comments where they'd live.
- **Native (folder) config backend** — production uses Git (versioned, PR-reviewed).
- **`ddl-auto: update`** — production runs Flyway per service.

## Why the duplication? (the shared-library question)

The security kit (`SecurityConfig`, `JwtClaims`) and DTO shapes are copied
into each service. A `securebank-commons` jar would DRY it — and couple every
service's release train to it. Real estates start with copies, extract once a
pattern proves stable, and keep the shared artifact tiny and boring. See
`Role.java`'s comment for the enum case specifically.

## Exercises

1. Scale for real: `docker compose up -d --scale account-service=2` (remove
   `container_name` first) — watch both instances in Eureka and the gateway
   round-robin. What breaks? (Hint: the fixed host-port mapping.)
2. Add a `GET /api/transfers/{reference}` endpoint to payment-service and a
   "look up a transfer" box to the frontend.
3. Implement the idempotency key: reject a second debit with the same
   `reference` in account-service's internal API; write the test first.
4. Add a dead-letter topic to notification-service using spring-kafka's
   `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`.
