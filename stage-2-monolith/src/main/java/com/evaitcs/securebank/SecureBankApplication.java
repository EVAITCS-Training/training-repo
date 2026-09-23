package com.evaitcs.securebank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * STAGE 2 — THE SECUREBANK MODULAR MONOLITH (Weeks 4-6 of the program)
 * ============================================================================
 *
 * WHAT THIS APP IS
 *   One deployable Spring Boot application that exposes the whole SecureBank
 *   REST API: authentication (JWT), customers, accounts, transfers, statements
 *   and loans. In Stage 5 this exact functionality is split into microservices
 *   — compare the two to see WHY and HOW decomposition works.
 *
 * HOW A REQUEST FLOWS THROUGH THE LAYERS (the "connection map")
 *
 *   HTTP request
 *      │
 *      ▼
 *   security/SecurityConfig ── Spring Security filter chain (JWT validation,
 *      │                        authorization rules). Runs BEFORE any controller.
 *      ▼
 *   web/*Controller ────────── translates HTTP <-> Java. Validates input DTOs,
 *      │                        extracts the caller identity from the JWT.
 *      ▼
 *   service/*Service ───────── business rules + @Transactional boundaries.
 *      │                        This is where "a transfer is atomic" is enforced.
 *      ▼
 *   repository/*Repository ─── Spring Data JPA interfaces. Spring generates the
 *      │                        implementation at runtime from method names/@Query.
 *      ▼
 *   domain/* ───────────────── JPA @Entity classes = rows in the database
 *                               (the same tables you created in stage-1).
 *
 * WHY THE PACKAGE IS STRUCTURED THIS WAY
 *   @SpringBootApplication implies @ComponentScan on THIS package
 *   (com.evaitcs.securebank). Everything in sub-packages (domain, repository,
 *   service, security, web, config) is discovered automatically — that is why
 *   the main class lives at the ROOT of the package tree. Move it into a
 *   sub-package and beans silently stop being found (classic beginner bug).
 *
 * "MODULAR" MONOLITH
 *   Even though it deploys as one jar, the packages are drawn along BUSINESS
 *   boundaries (accounts, transfers, loans, auth). Those same boundaries become
 *   the microservices in Stage 5 (bounded contexts / Domain-Driven Design).
 * ============================================================================
 */
@SpringBootApplication
public class SecureBankApplication {

    public static void main(String[] args) {
        // Boots the embedded Tomcat server, builds the Spring ApplicationContext
        // (the "container" holding all beans), runs auto-configuration based on
        // what is on the classpath (JPA because of starter-data-jpa, Security
        // because of starter-security, OpenAPI docs because of springdoc, ...).
        SpringApplication.run(SecureBankApplication.class, args);
    }
}
