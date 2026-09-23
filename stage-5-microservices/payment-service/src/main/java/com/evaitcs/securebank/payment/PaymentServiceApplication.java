package com.evaitcs.securebank.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * PAYMENT SERVICE — the Payments bounded context, and the home of the SAGA.
 * ============================================================================
 *
 * In stage-2, TransferService did debit+credit in ONE database transaction
 * (SELECT FOR UPDATE, ACID, done). Here the money lives in ANOTHER SERVICE's
 * database — no shared transaction is possible. This service replaces ACID
 * with a SAGA: a sequence of local steps + COMPENSATING actions that undo
 * completed steps when a later step fails (Week 7: database-per-service,
 * sagas, eventual consistency).
 *
 * ORCHESTRATION vs CHOREOGRAPHY (the two saga styles):
 *   This is an ORCHESTRATOR — payment-service actively calls debit, then
 *   credit, then compensates on failure. The flow is readable in ONE class
 *   (TransferSagaService), which is why most teams start here.
 *   The alternative, CHOREOGRAPHY, has services react to each other's Kafka
 *   events with no conductor (the study guide sketches it). We use events
 *   too — but only for NOTIFICATIONS after the fact (async, decoupled),
 *   never to drive the money flow. Mixing both styles is normal; driving
 *   money by choreography alone makes failures hard to trace.
 *
 * RESILIENCE: calls to account-service pass through Resilience4j
 * (@Retry + @CircuitBreaker on AccountClient) — the network is now a
 * first-class source of failure, which is the price of the split.
 *
 * OWNED DATA: paymentdb.transfer ONLY (the saga's state machine). It stores
 * account ids as VALUES — no FKs across databases, ever.
 *
 * A WAR STORY ABOUT DISCOVERY (kept because it WILL happen to students):
 *   The first version of this app exposed a @Bean @LoadBalanced
 *   RestClient.Builder — the textbook way to get lb:// resolution. It broke
 *   Eureka registration with "No instances available for discovery-service":
 *   the Eureka client's OWN HTTP transport picked up our load-balanced
 *   builder bean, so its calls to http://discovery-service:8761/eureka/ were
 *   themselves resolved THROUGH the registry — which is empty until
 *   registration succeeds. Chicken and egg. The gateway docs warn about the
 *   same trap (see docs/refs/SPRING_CLOUD_2025.1_CHEATSHEET.md §A).
 *   The fix: AccountClient injects LoadBalancerClient and resolves instances
 *   EXPLICITLY — no global builder bean, no interception of infrastructure
 *   clients, and discovery is visible in the code instead of implicit.
 * ============================================================================
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
