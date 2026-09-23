# Stage 2 — SecureBank Modular Monolith (Weeks 4-6: Core Java → Spring, Security, JPA)

One Spring Boot application exposing the entire SecureBank API: JWT auth,
customers, accounts, transfers, statements, loans. Built on **Spring Boot
4.1.1 / Java 21 / Spring Security 7 / Jackson 3** — the current modern stack
(see [`docs/refs/BOOT4_CHEATSHEET.md`](../docs/refs/BOOT4_CHEATSHEET.md) for
what changed from the Boot 3.x code you may have seen in older material).

## Run it

Zero-setup mode (H2 in-memory DB, seeded demo data):

```powershell
.\mvnw.cmd spring-boot:run
```

Against the real stage-1 PostgreSQL:

```powershell
cd ..\stage-1-database ; docker compose up -d ; cd ..\stage-2-monolith
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Then open:

| URL | What |
|---|---|
| http://localhost:8080/swagger-ui.html | Interactive API docs — login, click **Authorize**, paste the token |
| http://localhost:8080/h2-console | Live H2 database (default profile; JDBC URL `jdbc:h2:mem:securebank`, user `sa`) |
| http://localhost:8080/actuator/health | Health probe (Docker/K8s will use this in stages 4-5) |

Demo logins (seeded by `config/DataSeeder.java`): **alice / password123**
(customer), **bob / password123** (customer), **teller / password123**
(staff who approves loans), **admin / password123**.

Quick smoke test from PowerShell:

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
  -ContentType application/json -Body '{"username":"alice","password":"password123"}'
$auth  = @{ Authorization = "Bearer $($login.token)" }
Invoke-RestMethod -Uri http://localhost:8080/api/accounts -Headers $auth          # my accounts
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/transfers -Headers $auth `
  -ContentType application/json -Body '{"fromAccountId":1,"toAccountId":2,"amount":25.00}'
```

## Tests

```powershell
.\mvnw.cmd test          # 18 tests: unit + JPA slice + full HTTP integration
```

| Test | Layer | What it proves |
|---|---|---|
| `service/TransferServiceTest` | Unit (Mockito) | Transfer logic, lock ordering, overdraft rejection — no Spring, milliseconds |
| `repository/AccountRepositoryTest` | `@DataJpaTest` slice | Derived queries + JPQL + locking finder against real H2 |
| `SecureBankApiTest` | `@SpringBootTest` + MockMvc | Login→JWT→accounts→transfer→RBAC→problem+json, the whole stack |
| `SecureBankApplicationTests` | Smoke | The context wires up at all |

## The architecture (package = layer)

```
HTTP → security/ (filter chain, JWT)  →  web/ (controllers, DTOs, errors)
     →  service/ (rules, transactions)  →  repository/ (Spring Data JPA)
     →  domain/ (entities = stage-1 tables)
```

Read the class-level comment in `SecureBankApplication.java` for the full
connection map. Start a code-reading session there, then follow one request
(POST /api/transfers) down the layers: `TransferController` →
`TransferService` → `AccountRepository.findByIdForUpdate` → `Account.withdraw`.

## Teaching landmarks (where each week's theory becomes code)

- **OOP pillars (W4)** — `Account.deposit/withdraw`: encapsulated invariants;
  `PasswordEncoder` interface: polymorphism; `UserDetails`: abstraction.
- **Collections/Streams (W4)** — DTO mapping with `.stream().map(...).toList()`
  in every controller.
- **Concurrency (W5)** — `TransferService`: pessimistic locks in ascending-id
  order (deadlock prevention); `Account.@Version`: optimistic locking.
- **Spring/JPA (W6)** — repositories (derived vs `@Query` vs locking),
  `@Transactional` boundaries, dirty-checking, the N+1 fix (`JOIN FETCH`).
- **Security (W6)** — `SecurityConfig` filter-chain walkthrough, stateless
  JWT (issue in `JwtService`, verify via `JwtDecoder`), claims→authorities
  (`RoleClaimJwtConverter`), `@PreAuthorize` RBAC, 404-not-403 ownership.
- **REST (W7)** — resource URLs, RFC 7807 `ProblemDetail` errors
  (`GlobalExceptionHandler`), pagination envelope (`PageDto`), generated
  OpenAPI docs (`OpenApiConfig`).
- **Testing (W8)** — the pyramid in one module: unit → slice → integration.

## Known pitfalls this code deliberately demonstrates

1. `config/DataSeeder` — why `@Transactional` self-invocation silently does
   nothing (and the detached-entity bug it caused during development; the
   class comment tells the story).
2. `Account.requirePositive` — `compareTo` vs `equals` on BigDecimal (scale!).
3. `Loan.approve` — state-machine guards instead of public setters.

## What changes in Stage 5

This monolith is decomposed along its own package seams: `security` →
auth-service, accounts/statements → account-service, transfers →
payment-service (now a cross-service SAGA), loans → loan-service. Keep both
stages open side by side — the diff between `TransferService` here and
`payment-service` there is the true cost/benefit of microservices.
