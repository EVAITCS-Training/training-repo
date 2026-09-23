# EVAITCS SecureBank — the 9-week program as one runnable monorepo

A teaching monorepo that builds the **same banking product five times**, each
stage adding one layer of professional reality. Every file is commented as
teaching material: not just *what* the code does, but *why* it's shaped that
way and *how it connects* to the rest of the system.

```
stage-1-database     PostgreSQL schema, seed data, 5 SQL lesson scripts
stage-2-monolith     Spring Boot 4.1.1 modular monolith (JWT, JPA, RFC 7807) — 18 tests
stage-3-frontend     React 19 + TypeScript + Vite SPA (router, React Query) — lint+build clean
stage-4-docker-ci    multi-stage Dockerfiles, nginx, full-stack compose, GitHub Actions CI
stage-5-microservices  8 services on Spring Cloud 2025.1.3: Eureka, Config Server,
                     MVC gateway, transfer SAGA with compensation, Resilience4j,
                     Kafka events — 28 tests, 11-container stack verified end-to-end
infra/               Kubernetes manifests + Terraform AWS samples (annotated, not applied)
docs/                ERD, architecture narrative, instructor guide, onboarding,
                     refs/ = Boot 4 & Cloud 2025.1 cheat sheets (fetched from live docs)
```

## Start here

| You are... | Read |
|---|---|
| a student | [`docs/STUDENT_ONBOARDING.md`](docs/STUDENT_ONBOARDING.md) |
| the instructor | [`docs/INSTRUCTOR_GUIDE.md`](docs/INSTRUCTOR_GUIDE.md) |
| curious about the design | [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) + [`docs/ERD.md`](docs/ERD.md) |
| migrating from Boot 3 tutorials | [`docs/refs/BOOT4_CHEATSHEET.md`](docs/refs/BOOT4_CHEATSHEET.md) |

## Quick tour (each stage runs standalone)

```powershell
stage-1-database>  docker compose up -d                     # Postgres + lessons
stage-2-monolith>  .\mvnw.cmd spring-boot:run               # :8080 + Swagger UI
stage-3-frontend>  npm install ; npm run dev                # :5173 (needs stage 2)
stage-4-docker-ci> docker compose up -d --build             # :8081 full monolith stack
stage-5-microservices> docker compose up -d --build         # :8090 + Eureka :8761
```

Demo logins: `alice/password123`, `bob/password123` (customers),
`teller/password123`, `admin/password123` (staff).

## Verify everything (what CI runs)

```powershell
stage-2-monolith> .\mvnw.cmd test          # 18 tests
stage-3-frontend> npm run lint ; npm run build
stage-5-microservices> foreach ($s in dir -dir) { .\$s\mvnw.cmd -f $s\pom.xml test }   # 28 tests
docker compose -f stage-4-docker-ci/docker-compose.yml config -q
docker compose -f stage-5-microservices/docker-compose.yml config -q
```

## Git history as a curriculum

Tags mark the finished state of each stage — `git checkout stage-2-monolith`
gives students exactly the codebase that week's lectures assume:

```
stage-1-database → stage-2-monolith → stage-3-frontend → stage-4-docker-ci → stage-5-microservices → infra
```

## The one idea that ties it together

Stage 5 is **not a rewrite** of stage 2 — it is stage 2's package structure
promoted to services (`security/` → auth-service, accounts → account-service,
transfers → payment-service's saga, loans → loan-service). The frontend never
changes. The API contract never changes. What changes is *where the failures
live* — and that difference is the entire microservices curriculum, made
visible by diffing two folders.

## Honesty section (deliberate simplifications, named everywhere)

HS256 shared JWT secret · one Postgres container hosting four service
databases · `/internal/**` protected by routing absence in compose (closed
properly in `infra/k8s`) · no idempotency keys or transactional outbox
(both patterns commented where they'd live) · Config Server in native mode.
Each is flagged in code comments and in `docs/ARCHITECTURE.md` so students
learn the gap along with the pattern.
