# AGENTS.md — build/test commands for AI assistants & contributors

Repo: EVAITCS SecureBank teaching monorepo. Stages are INDEPENDENT projects;
always run commands from inside the stage folder.

| Stage | Stack | Build / verify |
|---|---|---|
| `stage-1-database` | PostgreSQL 17 (Docker) | `docker compose config -q`; `docker compose up -d` |
| `stage-2-monolith` | Spring Boot 4.1.1, Java 21 | `.\mvnw.cmd test` (Windows) / `./mvnw test` |
| `stage-3-frontend` | React 19 + TS + Vite | `npm install`, `npm run build` (tsc + vite) |
| `stage-4-docker-ci` | Docker/compose, GitHub Actions | `docker compose config -q` |
| `stage-5-microservices` | 8 Spring Boot 4 + Spring Cloud 2025.1.3 services | per service: `.\mvnw.cmd test`; stack: `docker compose config -q` |
| `infra` | K8s YAML, Terraform | `terraform fmt -check` / `terraform validate` (if installed) |

Conventions:
- Java: constructor injection, records for DTOs, RFC 7807 ProblemDetail errors,
  no Lombok. Comments are TEACHING material — keep the connective explanations.
- Boot 4 / Cloud 2025.1 specifics: see `docs/refs/*_CHEATSHEET.md` BEFORE writing
  framework code (starter names, test-annotation packages, gateway properties
  all differ from Boot 3 material).
- Money is always `BigDecimal` (scale 2); timestamps are `Instant`/`TIMESTAMPTZ`.
- Never commit secrets; demo credentials only (`password123` seeded users).
