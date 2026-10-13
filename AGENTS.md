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
| `core-java` | Plain Java 8 level, classic idiom (Maven multi-module, 11 topic modules) | `.\mvnw.cmd test`; per module: `.\mvnw.cmd -pl <module> test` or `... compile exec:java` |
| `collections-framework` | Plain Java 8, classic idiom (Maven multi-module, 10 topic modules incl. concurrency deep-dive) | `.\mvnw.cmd test`; per module: `.\mvnw.cmd -pl <module> test` or `... compile exec:java` |
| `java8-features` | Plain Java 8 (Maven multi-module, 9 topic modules) | `.\mvnw.cmd test`; per module: `.\mvnw.cmd -pl <module> test` or `... compile exec:java` |
| `advanced-java` | Java 21, modern idiom (Maven multi-module, 12 topic modules: erasure → reflection/annotations → JMM/locks/executors → GC → IO/NIO → patterns incl. beyond-GoF → exceptions → JDBC/PostgreSQL) | `.\mvnw.cmd test`; per module: `.\mvnw.cmd -pl <module> test` or `... compile exec:java` |
| `infra` | K8s YAML, Terraform | `terraform fmt -check` / `terraform validate` (if installed) |

Conventions:
- Java: constructor injection, records for DTOs, RFC 7807 ProblemDetail errors,
  no Lombok. Comments are TEACHING material — keep the connective explanations.
- Boot 4 / Cloud 2025.1 specifics: see `docs/refs/*_CHEATSHEET.md` BEFORE writing
  framework code (starter names, test-annotation packages, gateway properties
  all differ from Boot 3 material).
- Money is always `BigDecimal` (scale 2); timestamps are `Instant`/`TIMESTAMPTZ`.
- Teaching material in ALL four Java course repos follows `docs/refs/TEACHING_STYLE.md`
  (the clarity rubric): terms glossed at first use, "Read the diagram:" bullets under every
  complex mermaid diagram, numbered walkthroughs for every runtime mechanism,
  "In one sentence:" takeaways. Apply it when writing or editing any module README/demo/test.
- `core-java` teaches the CLASSIC (pre-lambda) idiom on purpose — anonymous inner classes,
  explicit loops — with forward pointers into `java8-features`. Course order: core-java →
  collections-framework → java8-features → advanced-java → stage-2.
- `collections-framework` follows the same rules as `core-java` (release 8, classic idiom,
  per-module domain duplication, mermaid-first READMEs). Concurrency demos/tests must stay
  DETERMINISTIC: CountDownLatch start guns + join, scripted interleavings for compound-action
  bugs, exact counts asserted only where a lock guarantees them.
- `advanced-java` is the exception to release 8: Java 21, modern idiom ENCOURAGED (records,
  sealed, pattern matching, virtual threads). Same determinism rules; GC-dependent tests use
  bounded-retry polling with deadlines (System.gc() is a hint), never assert heap sizes or
  timings, and use only public management APIs (no --add-opens, no com.sun internals).
- `advanced-java/12-jdbc` is the ONLY course module with a third-party dep (`org.postgresql:
  postgresql`, module-scoped) and an external service (stage-1 PostgreSQL). It MUST degrade
  gracefully: tests SKIP via JUnit assumptions and demos return normally when the DB is down,
  so `.\mvnw.cmd test` stays green without Docker. It is strictly NON-DESTRUCTIVE — writes run
  in rolled-back transactions or session temp tables, never mutating stage-1 seed data.
  Connection overrides: `JDBC_URL`/`JDBC_USER`/`JDBC_PASSWORD` (default localhost:5432/securebank).
- `java8-features` compiles with `--release 8`: no records/var/post-Java-8 APIs there;
  domain classes are duplicated per module on purpose. Each module README leads with
  mermaid diagrams (concept first, then code).
- Never commit secrets; demo credentials only (`password123` seeded users).
