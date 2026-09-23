package com.evaitcs.securebank.configsrv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * ============================================================================
 * CONFIG SERVER — centralized, externalized configuration (Week 7).
 * ============================================================================
 *
 * THE PROBLEM: eight services each need datasource URLs, the Eureka address,
 * Kafka brokers, the shared JWT secret... Baking those into eight jars means
 * rebuilding everything to change one value, and eight copies WILL drift.
 *
 * THE MECHANISM: each service declares
 *     spring.config.import: optional:configserver:http://localhost:8888
 * and at STARTUP (before its own beans are created) it fetches two documents
 * from this server, keyed by its spring.application.name:
 *     application.yml          <- shared by everyone
 *     <service-name>.yml       <- just for it
 * Fetched values OVERRIDE the service's local defaults. One place to change
 * the JWT secret; eight services pick it up on next boot.
 *
 * BACKEND MODES:
 *   git     — production standard: config lives in a VERSIONED Git repo
 *             (spring.cloud.config.server.git.uri=...). History + PR review
 *             for config changes; rollback = git revert.
 *   native  — what we run here: plain folder on disk (../config-repo), so the
 *             whole lesson is visible in this monorepo. Clients can't tell
 *             the difference — same HTTP protocol, same YAML payloads.
 *
 * WHY IT DOESN'T USE EUREKA/CONFIG ITSELF: bootstrap ordering. The config
 * server must be reachable at a KNOWN address before anything else starts —
 * discovery-based config lookup is a chicken-and-egg problem (solvable with
 * spring.cloud.config.discovery.enabled, at the cost of startup order
 * acrobatics; we keep it simple and deterministic).
 * ============================================================================
 */
@SpringBootApplication
@EnableConfigServer // adds the /{name}/{profile} REST endpoints clients import
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
