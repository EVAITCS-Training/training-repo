package com.evaitcs.securebank.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================================
 * NOTIFICATION SERVICE — the consumer side of the eventing story (Week 7:
 * Messaging & Caching; async communication).
 * ============================================================================
 *
 * IT OWNS NO DATABASE. Its inputs are Kafka topics ("transfers", "loans");
 * its state is an in-memory list of recent notifications (a real system
 * persists them and dispatches email/SMS/push through provider APIs).
 *
 * THE DECOUPLING PAYOFF, concretely: payment-service and loan-service have
 * NO IDEA this service exists. They publish events; whoever subscribes is
 * the broker's business. Delete notification-service: transfers still work.
 * Add a fraud-scoring subscriber tomorrow: producers unchanged. Compare
 * with the monolith, where "send a receipt" would have been one more
 * dependency injected into TransferService — and one more thing its tests
 * and outage radius had to cover.
 *
 * CONSUMER GROUPS (the scaling knob): every instance shares group-id
 * "notification-service"; Kafka gives each MESSAGE to exactly ONE member of
 * the group (load balancing), while OTHER groups (audit, analytics...) each
 * get their own full copy (fan-out). Same topic, two orthogonal behaviors.
 * ============================================================================
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
