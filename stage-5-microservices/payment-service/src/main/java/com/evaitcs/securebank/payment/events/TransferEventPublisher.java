package com.evaitcs.securebank.payment.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * ============================================================================
 * ASYNC EVENTS — the decoupled half of microservice communication (Week 7:
 * synchronous vs asynchronous; Messaging & Caching).
 * ============================================================================
 *
 * WHY PUBLISH AT ALL? The saga's callers (notification-service, analytics,
 * audit...) don't need to ANSWER the transfer — they need to KNOW about it.
 * Sync calls couple services in TIME (callee down = caller waits/fails);
 * Kafka decouples: we fire the event and move on, consumers process when
 * ready, and the broker RETAINS events so a consumer that was down catches
 * up on restart (durable, replayable — the two properties HTTP callbacks
 * don't give you).
 *
 * WIRE FORMAT — hand-serialized JSON strings via KafkaTemplate<String,String>:
 *   {"type":"TransferCompleted","reference":"xfer-7","from":1,"to":2,
 *    "amount":100.00,"at":"2026-..."}
 * We inject Jackson 3's JsonMapper and serialize ourselves instead of using
 * Kafka's JsonSerializer — explicit is better than magic while learning, and
 * it dodges producer/consumer type-header coupling entirely. Consumers parse
 * by the "type" field (see notification-service's listener).
 *
 * THE HONESTY NOTE (outbox): publishing AFTER the DB commit opens a tiny
 * window — crash between commit and send, and the event is lost. Production
 * fix = transactional OUTBOX: write the event row in the SAME local
 * transaction as the transfer, a relay ships it to Kafka at-least-once,
 * consumers dedupe by `reference`. We log send failures loudly instead;
 * the pattern is named so students recognize it in the wild.
 * ============================================================================
 */
public final class TransferEventPublisher {

    /** Topic names are contracts — consumers subscribe by these strings. */
    public static final String TOPIC = "transfers";

    // --- event payloads (records -> clean JSON via Jackson 3) -----------------
    public record TransferCompleted(String type, String reference, Long fromAccountId,
                                    Long toAccountId, BigDecimal amount, Instant at) {
        public static TransferCompleted of(String reference, Long from, Long to, BigDecimal amount) {
            return new TransferCompleted("TransferCompleted", reference, from, to, amount, Instant.now());
        }
    }

    public record TransferFailed(String type, String reference, Long fromAccountId,
                                 Long toAccountId, BigDecimal amount, String reason, Instant at) {
        public static TransferFailed of(String reference, Long from, Long to, BigDecimal amount, String reason) {
            return new TransferFailed("TransferFailed", reference, from, to, amount, reason, Instant.now());
        }
    }

    @Component
    public static class Publisher {

        private static final Logger log = LoggerFactory.getLogger(Publisher.class);

        private final KafkaTemplate<String, String> kafka;
        private final JsonMapper json;

        public Publisher(KafkaTemplate<String, String> kafka, JsonMapper json) {
            this.kafka = kafka;
            this.json = json;
        }

        public void publishCompleted(String reference, Long from, Long to, BigDecimal amount) {
            send(reference, TransferCompleted.of(reference, from, to, amount));
        }

        public void publishFailed(String reference, Long from, Long to, BigDecimal amount, String reason) {
            send(reference, TransferFailed.of(reference, from, to, amount, reason));
        }

        private void send(String key, Object event) {
            try {
                String payload = json.writeValueAsString(event);
                // KEY = reference: Kafka guarantees same-key messages land on
                // the same PARTITION -> consumers see one transfer's events IN ORDER.
                kafka.send(TOPIC, reference(key), payload)
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                // Outbox pattern would prevent this loss — see class comment.
                                log.error("Kafka send FAILED for {} (event lost without outbox): {}",
                                        TOPIC, payload, error);
                            } else {
                                log.info("Published to {} [partition {}, offset {}]: {}",
                                        TOPIC, result.getRecordMetadata().partition(),
                                        result.getRecordMetadata().offset(), payload);
                            }
                        });
            } catch (Exception e) {
                // Jackson 3 throws unchecked; still guard so a serialization
                // bug can never fail the money flow itself.
                log.error("Could not serialize/publish transfer event", e);
            }
        }

        private static String reference(String reference) {
            return reference; // key = the saga reference (partition-ordering contract)
        }
    }

    /** Declarative topic creation — Boot's KafkaAdmin applies these at startup
     *  so first publish doesn't rely on broker auto-create settings. */
    @Configuration
    public static class Topics {

        @Bean
        NewTopic transfersTopic() {
            return org.springframework.kafka.config.TopicBuilder
                    .name(TOPIC)
                    .partitions(1)   // 1 partition = total ordering (fine for the demo;
                    .replicas(1)     // scale partitions when throughput demands it)
                    .build();
        }
    }
}
