package com.evaitcs.securebank.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * API GATEWAY — the single front door (Week 7: Gateway, routing, filters).
 * ============================================================================
 *
 * WHY A GATEWAY: without one, the browser must know every service's address,
 * and cross-cutting concerns (auth headers, rate limits, retries, CORS,
 * logging) get reimplemented in each service. The gateway is ONE entry point
 * that MATCHES requests by predicates (Path=...), APPLIES filters
 * (StripPrefix, rate limit...), and FORWARDS via load-balanced uris
 * (lb://service-name resolved through Eureka).
 *
 * THIS ONE IS THE *MVC* GATEWAY (spring-cloud-starter-gateway-server-webmvc,
 * servlet stack) — in Spring Cloud 2025.1 "gateway" defaults to MVC; the
 * reactive/WebFlux flavor is a separate starter. Routes are pure CONFIG (see
 * application.yml here and config-repo/api-gateway.yml) — this class has zero
 * routing code, which is exactly the point.
 *
 * SECURITY POSTURE — validation happens in the SERVICES, not here:
 * every resource service verifies the JWT itself (its own SecurityConfig).
 * A gateway that checks tokens and forwards "trust me" headers creates a
 * fatal insider assumption: anything that can reach a service directly
 * bypasses all security. Defense in depth: the perimeter (no /internal/**
 * route exists) AND each service enforce. (Discussion: gateways are still
 * the right place for RATE LIMITING and TLS termination.)
 *
 * THE FRONTEND DOESN'T KNOW THIS EXISTS: stage-3 calls /api/... exactly as
 * against the monolith; nginx points at :8080 either way. Same contract,
 * radically different topology underneath — that compatibility is what makes
 * incremental decomposition possible (Week 7: strangler fig pattern).
 * ============================================================================
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
