# Stage 3 — SecureBank Frontend (Weeks 1-2 + 8: HTML/CSS, React, TypeScript, State Management)

React 19 + TypeScript + Vite single-page app for SecureBank: login, dashboard,
transfers, paginated statements, and a role-aware loans screen. Talks to the
stage-2 API (or the stage-5 gateway — same port, same paths).

## Run it

```powershell
# 1. backend first (stage-2):  ..\stage-2-monolith> .\mvnw.cmd spring-boot:run
# 2. frontend:
npm install
npm run dev          # http://localhost:5173
```

Log in with **alice / password123** (customer) or **teller / password123**
(staff — sees the loan approval queue).

```powershell
npm run build        # tsc type-check + production bundle -> dist/
npm run lint         # oxlint — must stay clean (CI enforces it)
npm run preview      # serve the production build locally
```

## How it connects to the backend

```
browser :5173 ──/api/...──> Vite dev proxy ──> Spring Boot :8080
                                                  │
                          (Stage 4: nginx container replaces the dev proxy)
```

- `vite.config.ts` proxies `/api` → `localhost:8080`, so the app only ever
  calls its OWN origin (no CORS friction in dev; backend CORS config exists
  for the direct-call case — both approaches are commented there).
- `src/api/client.ts` is the single fetch wrapper: attaches `Authorization:
  Bearer <token>`, parses RFC 7807 `problem+json` into typed `ApiError`s, and
  turns a mid-session 401 into an automatic logout.
- `src/api/types.ts` mirrors the backend DTO records by hand (the contract —
  Week 7 discusses OpenAPI codegen as the automation of this).

## Source map (reading order for class)

| Path | Teaches |
|---|---|
| `src/main.tsx` | Provider composition: QueryClient → Router → Auth |
| `src/App.tsx` | Route table, nested layout routes, catch-all |
| `src/api/client.ts` | Stateless auth on every request, problem+json error handling |
| `src/api/endpoints.ts` | One typed function per backend route — the HTTP inventory |
| `src/auth/` | Context split (context / provider / hook), localStorage session, route guard ("UX, not security") |
| `src/pages/LoginPage.tsx` | Controlled forms, error UX |
| `src/pages/DashboardPage.tsx` | `useQuery`, parallel fetches, cache keys, when float money math is OK |
| `src/pages/TransferPage.tsx` | `useMutation` + `invalidateQueries` — the write path and cache consistency |
| `src/pages/StatementsPage.tsx` | Query-key pagination, derived state instead of effects |
| `src/pages/LoansPage.tsx` | Role-based UI; server still enforces the rules |

## Deliberate teaching points

1. **Derive, don't effect** — `StatementsPage` computes the selected account
   during render; an earlier draft used `useEffect(setState)` and oxlint's
   `set-state-in-effect` rule rejected it. The file's header comment tells the story.
2. **Fast-refresh hygiene** — auth context is split into
   `AuthContext.ts` / `AuthProvider.tsx` / `useAuth.ts` because files mixing
   component + non-component exports break hot reload (`only-export-components`).
3. **Money display** — `Intl.NumberFormat` in `components/format.ts`; the ONE
   place a float sum happens (dashboard total) carries a comment on why that's
   acceptable there and where the real arithmetic lives (backend BigDecimal).
4. **Token storage trade-off** — `api/client.ts` documents localStorage-vs-cookie
   (XSS vs CSRF) and why Bearer+localStorage+short-TTL is the common SPA choice.

## Exercises

1. Add a "Deposit" page calling `POST /api/accounts/{id}/deposit` (endpoint
   exists in stage 2; invalidate `['accounts']` afterwards).
2. Show the customer's name from `/api/customers/me` in the top bar for staff too.
3. Break the backend contract on purpose (rename a DTO field in stage 2) and
   observe what `tsc -b` catches at build time vs what would fail silently.
