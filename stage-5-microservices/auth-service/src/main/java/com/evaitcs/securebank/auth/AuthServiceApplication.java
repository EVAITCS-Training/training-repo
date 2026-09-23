package com.evaitcs.securebank.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * AUTH SERVICE — the Identity bounded context (Week 7: decomposition).
 * ============================================================================
 *
 * WHAT MOVED HERE from stage-2's security/ package: login, BCrypt checking,
 * JWT issuing, the app_user table. What DIDN'T: token VERIFICATION — every
 * resource service verifies tokens itself (see their SecurityConfig). This is
 * the standard split: one service MINTS identity, everyone else CHECKS it
 * with the shared secret from the Config Server.
 *
 * OWNED DATA: authdb.app_user ONLY. Note the customer_id column has NO
 * foreign key — the customer table lives in account-service's database.
 * Cross-context references are BY ID (value), never by FK/join. That is
 * "database per service" made concrete: a JOIN across two services' tables is
 * impossible by construction, which is exactly what keeps them decoupled.
 *
 * PUBLIC API (via gateway):
 *   POST /api/auth/login  ->  gateway strips /api  ->  POST /auth/login here
 *
 * DISCOVERY: registers itself with Eureka as "auth-service" so the gateway's
 * lb://auth-service route can find it (config-repo/application.yml enables
 * the client; locally it stays off — see application.yml).
 * ============================================================================
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
