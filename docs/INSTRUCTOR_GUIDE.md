# Instructor Guide — teaching the 9 weeks against this repo

Every module in the EVAITCS study guide has a **home** here: a file students
can open while you talk. This guide maps week → artifact → demo → discussion,
so lecture time goes to explanation, not typing.

Prereqs on the instructor machine: Docker Desktop, JDK 21+, Node 24+.
Everything else is `mvnw`/`npm` inside the stages.

## Week-by-week map

### Week 1 — Foundations + Frontend begins
| Topic | Open this | Demo |
|---|---|---|
| Processes/threads, races | stage-2 `domain/Account.java` (@Version comment), stage-1 `queries/04` | run the lost-update choreography in two psql windows |
| HTTP/DNS/statelessness | stage-3 `api/client.ts` header comment | `curl -v` a login; point at the missing session between calls |
| DB types, tables, FKs | `docs/ERD.md`, stage-1 `schema.sql` | draw the ERD live, then `\d account` |
| Git | repo history itself | `git log --oneline --graph`, show stage tags as milestones |

### Week 2 — JS/TS/React
| Topic | Open this | Demo |
|---|---|---|
| TS types as contracts | stage-3 `api/types.ts` | rename a DTO field in stage-2, watch `tsc -b` catch it |
| React state/hooks | `pages/TransferPage.tsx` (controlled form + mutation) | break the invalidation (`invalidateQueries`) and show stale UI |
| Derived state vs effects | `pages/StatementsPage.tsx` header comment | show the oxlint rule that forced the redesign |
| Build tooling | `vite.config.ts` (proxy), `Dockerfile` (stage-3) | `npm run dev` vs `npm run preview` |

### Week 3 — SQL
Teach straight from `stage-1-database/queries/01..05` in order. The
reconciliation query in `03` is the week's centerpiece; the deadlock
choreography in `04` is the week's showpiece. Finish by opening stage-2's
`AccountRepository` and matching each SQL to its derived query/`@Query`.

### Weeks 4-5 — Core/Advanced Java
| Topic | Open this | Demo |
|---|---|---|
| OOP pillars | stage-2 `domain/*` (rich entities) | refactor a setter-based version live and break an invariant |
| Collections/streams | controllers' `.stream().map().toList()` | — |
| Concurrency | `service/TransferService.java` (lock ordering) | stage-5 `TransferSagaTest` — the branch matrix as a story |
| BigDecimal discipline | `Account.deposit/withdraw`, `Loan.approve` | show the 311.06 test that caught a wrong expectation |

### Week 6 — Spring, Security, JPA
Teach from stage-2 top-down: `SecureBankApplication` (scan rules) →
`SecurityConfig` (filter chain) → `JwtService`/`AuthService` (issue) →
repositories (three query styles) → `GlobalExceptionHandler` (ProblemDetail).
Demo: Swagger UI login → Authorize → call endpoints; then delete the token
header and watch 401.

### Week 7 — REST & Microservices
The big transition week. Sequence:
1. stage-2 Swagger = the contract.
2. stage-5 `config-repo/api-gateway.yml` = routes; `StripPrefix` math on the board.
3. `payment-service/saga/TransferSagaService.java` vs stage-2
   `TransferService.java` — SAME logic, two worlds. Put them side by side.
4. Eureka dashboard live; stop a service, watch 503s and circuit states
   (`/actuator/health` on payment).
5. Kafka console consumer while a transfer happens (stage-5 README script).

### Week 8 — Testing, frontend state, cloud
| Topic | Open this |
|---|---|
| Test pyramid | stage-2's three test classes + stage-5 matrix in `ci.yml` |
| React Query | stage-3 `DashboardPage` (parallel queries, cache keys) |
| Containers | stage-2/3 `Dockerfile`s (layer caching, non-root, healthcheck) |
| IaC | `infra/terraform/*.tf` (read vpc.tf as the guide's VPC diagram) |
| K8s | `infra/k8s/20-microservices.yaml` probes + `50-hpa.yaml` |

### Week 9 — DevOps, AI, system design, hardening
- CI: walk `.github/workflows/ci.yml`; discuss why images aren't built per PR.
- Hardening: `infra/k8s/40-networkpolicy.yaml` closes stage-5's named gap;
  OWASP pass = grep the repo for the controls (validation, ownership checks,
  BCrypt, problem+json, no secrets).
- System design interview prep: `docs/ARCHITECTURE.md` request walkthrough is
  a model answer; have students present it from memory at the whiteboard.
- AI-assisted development: this repo's comments are the artifact — show how
  every "why" comment maps to a decision an AI could not have made alone.

## Recurring demo scripts (tested end-to-end on this repo)

1. **Monolith in 60 seconds**: `stage-2-monolith> .\mvnw.cmd spring-boot:run`,
   Swagger login alice/password123, transfer, statement.
2. **Full stack**: `stage-4-docker-ci> docker compose up -d --build`,
   http://localhost:8081.
3. **Microservices**: `stage-5-microservices> docker compose up -d --build`,
   then the 6-step script in `stage-5-microservices/README.md` (Eureka →
   config → gateway → saga → resilience → events).
4. **The failure lecture**: `docker compose stop account-service` mid-demo,
   attempt a transfer, read payment's logs (retry → open circuit → 503),
   restart, watch half-open recovery. Budget 15 minutes; it's the week's ROI.

## Known gotchas (hit during construction — save your class time)

- Eureka registry propagation ≈ 30-60s after (re)starting a service: the
  gateway answers 503 "Unable to find instance" until caches refresh. BY DESIGN.
- `pg_isready` without `-d` logs scary FATALs when the default db name is
  absent (stage-5 compose comment).
- YAML flow lists split on commas: gateway predicates must be quoted
  (`["Path=/a/**,/b/**"]`) — unquoted becomes two ANDed predicates = dead route.
- A `@LoadBalanced RestClient.Builder` bean hijacks the Eureka client's own
  transport (chicken-and-egg registration failure) — see
  `PaymentServiceApplication`'s war-story comment.
- Mockito: `verify()` inside another `verify()`'s arguments = UnfinishedVerification.
- JPA: mutating an entity AFTER `save()` outside a transaction silently loses
  the change (detached entity) — stage-2 `DataSeeder` comment tells the tale.
