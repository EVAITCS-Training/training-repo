# Spring Cloud 2025.1.3 (Oakwood) + Spring Boot 4 Cheat Sheet

> Verified against: Spring Cloud 2025.1 release-notes wiki, module sources at exact tags
> (Gateway v5.0.3, Config v5.0.5, Netflix v5.0.2, Commons v5.0.2, CircuitBreaker v5.0.3,
> spring-kafka v4.1.1, Boot v4.1.1), Maven Central, and live start.spring.io metadata.
> Compatibility: 2025.1.x ↔ Boot 4.0.x/4.1.x (since 2025.1.2).

**BOM:** `org.springframework.cloud:spring-cloud-dependencies:2025.1.3` (import in
`dependencyManagement`). `spring-cloud-starter-parent` was REMOVED in 2025.1.

## start.spring.io dependency id → Maven artifact

All groupId `org.springframework.cloud` unless noted:

| start.spring.io id | artifactId |
|---|---|
| cloud-gateway | `spring-cloud-starter-gateway-server-webmvc` (plain "Gateway" = MVC now) |
| cloud-gateway-reactive | `spring-cloud-starter-gateway-server-webflux` |
| cloud-eureka | `spring-cloud-starter-netflix-eureka-client` |
| cloud-eureka-server | `spring-cloud-starter-netflix-eureka-server` |
| cloud-config-server | `spring-cloud-config-server` (⚠ NOT a "starter" — no `spring-cloud-starter-config-server` exists) |
| cloud-config-client | `spring-cloud-starter-config` |
| cloud-loadbalancer | `spring-cloud-starter-loadbalancer` |
| cloud-resilience4j | `spring-cloud-starter-circuitbreaker-resilience4j` (reactive: `...-reactor-resilience4j`) |
| kafka | `org.springframework.boot:spring-boot-starter-kafka` (pulls spring-kafka 4.1.x) |
| data-redis | `org.springframework.boot:spring-boot-starter-data-redis` |

## A. Gateway (Servlet/MVC flavor — what this repo uses)

- Artifact: `spring-cloud-starter-gateway-server-webmvc` (5.0.3).
  Old `spring-cloud-starter-gateway-mvc` ended at 4.3.x — renamed.
- Property namespace: **`spring.cloud.gateway.server.webmvc`** with `routes` (list) or
  `routes-map` (map). Old `spring.cloud.gateway.routes` is GONE in 5.x
  (WebFlux flavor: `spring.cloud.gateway.server.webflux.routes` + `default-filters`).
- `lb://service-name` URIs work via config (auto-adds highest-precedence LB filter; needs
  loadbalancer starter + discovery client). 503 if no instance registered.
- **No `default-filters` property for the MVC gateway** (WebFlux has it).
- StripPrefix: `filters: [StripPrefix=1]`. Java DSL: `BeforeFilterFunctions.stripPrefix(1)`;
  `lb()` must come after `stripPrefix()` in the Java DSL.
- Rate limiting: MVC has **no Redis `RequestRateLimiter`** (that is WebFlux-only). MVC has a
  Bucket4j `RateLimiter` filter — Java DSL only (`Bucket4jFilterFunctions.rateLimit(...)`,
  requires an `AsyncProxyManager<String>` bean).
- Java DSL packages: routes `org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route(...)`,
  `...handler.HandlerFunctions.http()`, filters `...mvc.filter.{BeforeFilterFunctions,LoadBalancerFilterFunctions}`.
- ⚠ Do NOT define a `@LoadBalanced RestClient.Builder` bean inside the gateway app itself —
  the gateway's internal RestClient breaks ("Service Instance cannot be null").

```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: accounts
              uri: lb://account-service
              predicates: [Path=/api/accounts/**]
```

## B. Config Server / Client

- Server artifact: `spring-cloud-config-server`; `@EnableConfigServer` still required —
  package `org.springframework.cloud.config.server.EnableConfigServer`.
- Native (filesystem) profile: run with `spring.profiles.active=native`;
  `spring.cloud.config.server.native.search-locations` (`classpath:/config`, `file:./config-repo`;
  supports `{application}/{profile}/{label}` placeholders).
- Git backend: `spring.cloud.config.server.git.uri` (Windows abs path: `file:///C:/...`),
  plus `search-paths`, `clone-on-start=true`, `force-pull=true`, `default-label` (defaults `main`).
- Client artifact: `spring-cloud-starter-config`. Wiring:
  `spring.config.import=optional:configserver:http://localhost:8888`
  (drop `optional:` for fail-fast; `spring.cloud.config.fail-fast=true` + `spring.cloud.config.retry.*`
  also works). Import URL beats `spring.cloud.config.uri`.
- Other client props: `spring.cloud.config.{name,profile,label,discovery.enabled,discovery.serviceId}`.
- **bootstrap.yml is legacy/dead** — only via `spring-cloud-starter-bootstrap`. Use `spring.config.import`.

## C. Eureka

- Server: `spring-cloud-starter-netflix-eureka-server` + `@EnableEurekaServer`
  (package `org.springframework.cloud.netflix.eureka.server`). Works on Boot 4 (5.0.x line).
  Server does NOT support AOT/native; client does (with `spring.cloud.refresh.enabled=false`).
- Client: `spring-cloud-starter-netflix-eureka-client`; no annotation needed (auto-registers).
- Standalone server YAML:

```yaml
eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
```

- `defaultZone` key is camelCase-mandatory (it's a Map key).
- Client props: `eureka.instance.{hostname,non-secure-port,prefer-ip-address,instance-id,lease-renewal-interval-in-seconds,metadata-map.*}`,
  `eureka.client.{enabled,healthcheck.enabled,service-url.defaultZone}`.
- Self-preservation: `eureka.server.enable-self-preservation` (default true),
  `eureka.server.renewal-percent-threshold` (0.85).
- If Spring Security is on the Eureka server classpath, CSRF must ignore `/eureka/**`.

## D. LoadBalancer

- Artifact: `spring-cloud-starter-loadbalancer` (the Eureka client starter already includes it).
- `@LoadBalanced` = `org.springframework.cloud.client.loadbalancer.LoadBalanced`;
  the `@LoadBalanced @Bean RestClient.Builder` pattern is current.
- Props: `spring.cloud.loadbalancer.{enabled,eager-load.clients,cache.ttl,retry.enabled,...}`.
  Default impl `RoundRobinLoadBalancer`.
- New in 5.0: HTTP Service Clients (`@HttpExchange` interfaces) get LB auto-config.

## E. Resilience4j

- Starter: `spring-cloud-starter-circuitbreaker-resilience4j` (transitively resilience4j 2.3.0
  incl. `resilience4j-spring-boot3`, so annotations work out of the box).
- Annotations: `io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker`,
  `...retry.annotation.Retry`, `...timelimiter.annotation.TimeLimiter`, `...bulkhead.annotation.Bulkhead`.
  `@CircuitBreaker(name = "svc", fallbackMethod = "fb")` — fallback in same class with extra
  trailing `Throwable` param.
- Programmatic: inject `org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory`;
  customize with `Customizer<Resilience4JCircuitBreakerFactory>` + `Resilience4JConfigBuilder`.
- YAML: `resilience4j.circuitbreaker.instances.<name>.{sliding-window-size,failure-rate-threshold,wait-duration-in-open-state,...}`,
  `resilience4j.timelimiter.instances.<name>.timeout-duration` (⚠ TimeLimiter default = 1s —
  raise it or disable via `spring.cloud.circuitbreaker.resilience4j.disable-time-limiter=true`),
  `resilience4j.retry.instances.*`.

## F. spring-kafka 4 (Boot 4.1 manages spring-kafka 4.1.x)

- Packages UNCHANGED: `org.springframework.kafka.core.KafkaTemplate`,
  `org.springframework.kafka.annotation.KafkaListener`, `org.springframework.kafka.config.TopicBuilder`,
  `org.apache.kafka.clients.admin.NewTopic`.
- Starter: `org.springframework.boot:spring-boot-starter-kafka`. Kafka client 4.x = KRaft only
  (ZooKeeper gone; `@EmbeddedKafka` lost its `kraft`/`zk` attributes).
- Boot props unchanged: `spring.kafka.bootstrap-servers`, `spring.kafka.consumer.{group-id,auto-offset-reset}`,
  `spring.kafka.producer/consumer.{key,value}-serializer` (Boot defaults: String ser/deser).
- Jackson 3 serializers live in `org.springframework.kafka.support.serializer`:
  **`JacksonJsonSerializer`/`JacksonJsonDeserializer`** (old `JsonSerializer`/`JsonDeserializer`
  deprecated). Simplest robust pattern: send JSON **strings** via `KafkaTemplate<String,String>`
  and serialize payloads yourself with the injected `JsonMapper`.
- `NewTopic` bean pattern unchanged: `TopicBuilder.name("x").partitions(1).replicas(1).build()`
  (picked up by Boot's `KafkaAdmin`).
- Spring Retry dropped: `@RetryableTopic` uses spring-kafka's own `@BackOff`.

## G. Removed/renamed in 2025.1 that matter here

1. `spring-cloud-starter-parent` REMOVED — use the `spring-cloud-dependencies` BOM.
2. Gateway starters renamed (see A).
3. Gateway property namespaces moved under `spring.cloud.gateway.server.{webmvc,webflux}.*`.
4. Config server has no starter artifact; bootstrap.yml legacy-only; `spring.config.import` is the way.
5. Jackson 3 + JSpecify + Framework 7 adaptations across all modules; Jackson-2-based
   spring-kafka classes deprecated.
