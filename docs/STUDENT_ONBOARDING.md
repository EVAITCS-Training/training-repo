# Student Onboarding — from zero to running all five stages

## One-time setup

| Tool | Check | Needed for |
|---|---|---|
| Docker Desktop | `docker --version` | stages 1, 4, 5 |
| JDK 21+ | `java -version` | stages 2, 5 (via `mvnw`) |
| Node 24 | `node --version` | stage 3 |
| Git | `git --version` | everything |

No Maven/Gradle install needed — every Java project ships the Maven wrapper
(`mvnw` / `mvnw.cmd`) which downloads the exact Maven version the repo pins.

## The five runs, in order

```powershell
# STAGE 1 — the database (2 min)
cd stage-1-database
docker compose up -d
docker compose exec db psql -U securebank -d securebank   # then: \i /queries/02-joins.sql? no—
#   (files aren't inside the container; instead from the repo root:)
Get-Content queries\02-joins.sql | docker compose exec -T db psql -U securebank -d securebank

# STAGE 2 — the monolith (first run ~3 min: downloads dependencies)
cd ..\stage-2-monolith
.\mvnw.cmd test                 # 18 tests, all green before you even run it
.\mvnw.cmd spring-boot:run      # http://localhost:8080/swagger-ui.html

# STAGE 3 — the frontend (first run ~1 min)
cd ..\stage-3-frontend
npm install
npm run dev                     # http://localhost:5173 (keep stage 2 running!)

# STAGE 4 — everything as containers (build ~5-10 min first time)
cd ..\stage-4-docker-ci
docker compose up -d --build    # http://localhost:8081 (stop stages 2/3 first: port 8080)

# STAGE 5 — microservices (build ~10-20 min first time; boot ~2-3 min)
cd ..\stage-5-microservices
docker compose up -d --build    # http://localhost:8090 frontend, :8761 Eureka
```

Logins everywhere: **alice/password123**, **bob/password123** (customers),
**teller/password123** (staff), **admin/password123**.

## Port map (avoid "address already in use")

| Port | Owner |
|---|---|
| 5432 | Postgres (stage 1, 4) / stage-5 db |
| 8080 | stage-2 monolith OR stage-4 backend OR stage-5 gateway |
| 5173 | stage-3 Vite dev server |
| 8081 | stage-4 frontend / stage-5 auth-service |
| 8082-8085 | stage-5 account/payment/loan/notification |
| 8090 | stage-5 frontend |
| 8761 / 8888 | stage-5 Eureka / Config Server |

Only ONE stack that owns 8080/5432 should run at a time:
`docker compose down` in the others first.

## How to read this repo (suggested first hour)

1. `docs/ARCHITECTURE.md` — the five-stage story and the transfer walkthrough.
2. `stage-2-monolith/.../SecureBankApplication.java` — the layer map comment.
3. Follow ONE request: `TransferController` → `TransferService` →
   `AccountRepository.findByIdForUpdate` → `Account.withdraw`.
4. `stage-5-microservices/payment-service/.../TransferSagaService.java` —
   the same request after decomposition.
5. `docs/refs/*_CHEATSHEET.md` — the Boot 4 / Cloud 2025.1 facts that differ
   from older tutorials you'll find online.

## When something breaks

| Symptom | Cause / fix |
|---|---|
| `address already in use :8080` | another stack owns it — `docker compose down` there |
| gateway 503 "Unable to find instance" | Eureka cache warming (~30-60s) — wait, retry |
| backend won't boot, "Schema-validation" | stage-1 schema and entities drifted — read the named column |
| stale demo data | `docker compose down -v` wipes volumes (init scripts re-run) |
| tests fail on a fresh clone | run `.\mvnw.cmd test` once per service; first run downloads the world |
| FATAL "database securebank does not exist" in stage-5 db logs | old image pre-healthcheck-fix — `docker compose pull`/rebuild |

## House rules (they're graded habits, not preferences)

- Money is `BigDecimal` with explicit scale/rounding. Never `double`.
- Identity comes from the verified JWT, never from request bodies.
- Errors are RFC 7807 `ProblemDetail`; statuses carry meaning (400 business
  refusal vs 503 technical failure vs 409 state conflict).
- Secrets never in git; demo credentials only.
- Comments explain WHY and HOW-IT-CONNECTS. Keep them when you refactor.
