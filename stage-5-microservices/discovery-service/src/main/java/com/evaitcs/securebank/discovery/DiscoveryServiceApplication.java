package com.evaitcs.securebank.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * ============================================================================
 * DISCOVERY SERVICE — Netflix Eureka, the phone book of the system.
 * ============================================================================
 *
 * THE PROBLEM IT SOLVES: services run in containers that come and go; IPs and
 * ports are NOT stable. Hardcoding "account-service lives at 10.0.3.7:8082"
 * breaks on every redeploy, scale-up, or crash.
 *
 * THE MECHANISM (Week 7: Service Discovery):
 *   1. REGISTER — every service starts, announces itself here
 *      ("I am account-service at 10.0.3.7:8082"), and renews the lease with a
 *      heartbeat every ~10s (see config-repo/application.yml).
 *   2. LOOKUP — callers ask for "account-service"; Eureka answers with the
 *      list of healthy instances; Spring Cloud LoadBalancer picks one.
 *      That's what makes `lb://account-service` work in the gateway and
 *      payment-service's RestClient.
 *   3. EVICT — heartbeats stop (crash/scale-down); after the lease expires
 *      the instance leaves the registry.
 *
 * SELF-PRESERVATION (the famous Eureka quirk): if MANY leases lapse at once,
 * Eureka suspects a NETWORK problem rather than mass deaths, and temporarily
 * stops evicting — keeping stale entries is safer than routing to nobody.
 * For local demos this "stale registry" behavior confuses people, so we
 * disable it below (eureka.server.enable-self-preservation=false) — a DEV
 * choice; production keeps it ON.
 *
 * WHY THIS SERVICE IMPORTS NEITHER CONFIG SERVER NOR EUREKA CLIENT:
 * bootstrapping is circular — the config server would need discovery to find
 * discovery. Infrastructure services carry their config locally. (K8s
 * replaces this whole service with kube-dns + Services; see infra/k8s.)
 *
 * Dashboard: http://localhost:8761 — watch instances appear as you start the
 * stack. That page is the single best "microservices are real now" moment
 * in the whole course.
 * ============================================================================
 */
@SpringBootApplication
@EnableEurekaServer // flips this app from eureka CLIENT to eureka SERVER
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
