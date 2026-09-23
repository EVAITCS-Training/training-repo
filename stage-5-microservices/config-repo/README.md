# config-repo — the CONFIGURATION BACKEND of the Config Server

This folder is what `config-server` serves to every other service. In
production it would be a **separate Git repository** (`spring.cloud.config
.server.git.uri=...`) so config changes are versioned, reviewed via PR, and
deployed without rebuilding images. For the classroom we run the Config
Server in `native` mode reading THIS folder — same protocol, same client
code, zero Git ceremony.

## How a service consumes it

Each service's local `application.yml` contains:

```yaml
spring:
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}
```

At startup the service asks the Config Server for *its* documents:
`application.yml` (shared by everyone) **plus** `<spring.application.name>.yml`
(its own file here). Values from the Config Server OVERRIDE the local
defaults — that's how one image runs standalone on a laptop (H2, no Eureka)
and in the compose stack (Postgres, Eureka, Kafka) with zero rebuilds.

`optional:` means the service still boots when the Config Server is down —
it falls back to its local defaults. Drop `optional:` in production for
fail-fast instead.

## Layering of configuration (highest wins)

```
environment variables (SPRING_DATASOURCE_URL, JWT_SECRET, ...)   <- platform/ops
config-repo/<service>.yml                                        <- environment team
config-repo/application.yml                                      <- shared defaults
service-local application.yml                                    <- developer defaults
```

## Files

| File | Applies to | Notable contents |
|---|---|---|
| `application.yml` | every service | Eureka URL, Kafka brokers, shared JWT secret |
| `auth-service.yml` | auth-service | authdb datasource |
| `account-service.yml` | account-service | accountdb datasource |
| `payment-service.yml` | payment-service | paymentdb datasource, Resilience4j tuning |
| `loan-service.yml` | loan-service | loandb datasource |
| `notification-service.yml` | notification-service | consumer group tuning |
| `api-gateway.yml` | api-gateway | the route table with `lb://` discovery URIs |

Note the shared JWT secret: auth-service SIGNS tokens with it, every resource
service VERIFIES with it — one value, distributed to eight services, changed
in one place. That is the Config Server's whole pitch.
