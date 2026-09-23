package com.evaitcs.securebank.loan.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Loan lifecycle events -> Kafka topic "loans".
 *
 * Same wire discipline as payment-service's publisher (hand-serialized JSON
 * strings, KafkaTemplate<String,String>, loud logging on send failure, outbox
 * pattern named-but-not-built — see TransferEventPublisher's class comment
 * for the full lesson; repeating it here would just be noise).
 *
 * WHO LISTENS: notification-service (customer messages). Later, without
 * touching this code: a risk-scoring service, an audit archive, a reporting
 * warehouse. THAT extensibility — new consumers, zero producer changes — is
 * the real reason eventing exists.
 */
public final class LoanEventPublisher {

    public static final String TOPIC = "loans";

    public record LoanApproved(String type, Long loanId, Long customerId, BigDecimal principal,
                               int termMonths, BigDecimal monthlyPayment, Instant at) {
        public static LoanApproved of(Long loanId, Long customerId, BigDecimal principal,
                                      int termMonths, BigDecimal monthlyPayment) {
            return new LoanApproved("LoanApproved", loanId, customerId, principal, termMonths,
                    monthlyPayment, Instant.now());
        }
    }

    public record LoanRejected(String type, Long loanId, Long customerId, Instant at) {
        public static LoanRejected of(Long loanId, Long customerId) {
            return new LoanRejected("LoanRejected", loanId, customerId, Instant.now());
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

        public void publishApproved(Long loanId, Long customerId, BigDecimal principal,
                                    int termMonths, BigDecimal monthlyPayment) {
            send(loanId, LoanApproved.of(loanId, customerId, principal, termMonths, monthlyPayment));
        }

        public void publishRejected(Long loanId, Long customerId) {
            send(loanId, LoanRejected.of(loanId, customerId));
        }

        private void send(Long key, Object event) {
            try {
                String payload = json.writeValueAsString(event);
                // key = loanId: all events for one loan share a partition ->
                // consumers see them in order (approved can't overtake applied).
                kafka.send(TOPIC, String.valueOf(key), payload)
                        .whenComplete((r, err) -> {
                            if (err != null) {
                                log.error("Kafka send FAILED for {}: {}", TOPIC, payload, err);
                            } else {
                                log.info("Published to {}: {}", TOPIC, payload);
                            }
                        });
            } catch (Exception e) {
                log.error("Could not serialize/publish loan event", e);
            }
        }
    }

    @Configuration
    public static class Topics {

        @Bean
        NewTopic loansTopic() {
            return TopicBuilder.name(TOPIC).partitions(1).replicas(1).build();
        }
    }
}
