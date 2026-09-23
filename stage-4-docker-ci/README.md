# Stage 4 — Docker & CI (Week 8: Containers, Week 9: DevOps)

Assembles stages 1-3 into one reproducible stack and guards every commit with
a CI pipeline. No new application code lives here — this stage is the
*packaging and automation* layer: compose wiring, plus the CI contract
(`.github/workflows/ci.yml` at the repo root).

## Run the whole stack

```powershell
docker compose up -d --build      # first run builds images (several minutes)
```

| Service  | URL | Notes |
|---|---|---|
| frontend | http://localhost:8081 | nginx serving `dist/`, proxies `/api` → backend |
| backend  | http://localhost:8080 | Spring Boot, `postgres` profile, `ddl-auto: validate` |
| swagger  | http://localhost:8081/swagger-ui.html | OpenAPI docs through the same origin |
| db       | localhost:5432 | PostgreSQL 17 with stage-1 schema + seed (user/pass/db: `securebank`) |

```powershell
docker compose ps                      # health status of all three
docker compose logs -f backend         # watch startup (validate + seeder)
docker compose down                    # stop
docker compose down -v                 # stop AND wipe the database volume
```

Login as before: **alice / password123** or **teller / password123**.

## What gets proven on every `up`

1. **Stage 1 ↔ Stage 2 contract** — the backend boots with
   `spring.jpa.hibernate.ddl-auto=validate` against the SQL-created schema.
   If an entity and `schema.sql` drift, the stack refuses to start.
2. **Startup ordering** — `depends_on: condition: service_healthy` chains
   db → backend → frontend via real healthchecks (`pg_isready`,
   `/actuator/health`, wget), not naive "container started" waits.
3. **One-origin production topology** — nginx (frontend) reverse-proxies
   `/api`, so the browser never makes cross-origin calls; the same pattern
   the Vite dev proxy provides in development (`stage-3-frontend/vite.config.ts`).

## Where the build artifacts live

| File | Purpose |
|---|---|
| `docker-compose.yml` | The stack: db + backend + frontend, healthchecks, env injection |
| `../stage-2-monolith/Dockerfile` | Multi-stage: JDK+mvnw build → JRE-only runtime, non-root, HEALTHCHECK |
| `../stage-3-frontend/Dockerfile` | Multi-stage: node build (`npm ci` + vite) → nginx runtime |
| `../stage-3-frontend/nginx.conf` | SPA fallback + `/api` reverse proxy (heavily commented) |
| `../.github/workflows/ci.yml` | backend tests, frontend typecheck/lint/build, compose config validation |

Dockerfiles live WITH their projects (industry convention: the repo that
builds an image owns its recipe); this stage owns the composition.

## CI pipeline (GitHub Actions)

Every push/PR to `main` runs three parallel jobs:

- **backend** — `./mvnw -B verify`: compile + all 18 tests on Temurin 21
- **frontend** — `npm ci && npm run lint && npm run build` on Node 24
- **compose** — `docker compose config -q`: catches broken stack wiring

Caching (`~/.m2`, npm) keys off the lockfiles; concurrency cancels superseded
runs. The stage-5 commit extends this file with a microservices matrix job.

## Troubleshooting

- **Port already in use (8080/5432)** — a locally-run stage-2 or stage-1
  compose still holds the port. `docker compose down` in both places, or stop
  the local `mvnw spring-boot:run`.
- **Backend restarts / "Schema-validation: missing column"** — stage-1 schema
  and stage-2 entities drifted; the error names the column. Fix one side and
  `docker compose down -v && docker compose up -d --build`.
- **Stale seed data after editing seed.sql** — init scripts only run on an
  EMPTY volume: `docker compose down -v` first.
- **Frontend 502s** — backend still starting; check
  `docker compose logs backend` and wait for the healthcheck to pass.

## Discussion points for class

1. Why does the runtime stage install `curl` but the build stage's Maven
   never ships? (attack surface, image size — `docker history` both stages)
2. Why is `JWT_SECRET` injected via environment instead of baked into the
   image? (12-factor config; the same image must run in every environment)
3. Why does CI validate the compose file but NOT build the images? Where
   should image builds live instead? (release pipeline, registry, tags)
4. Trace one login click through: React → nginx → Spring Security → Postgres
   and back. Every hop is a file in this repo.
