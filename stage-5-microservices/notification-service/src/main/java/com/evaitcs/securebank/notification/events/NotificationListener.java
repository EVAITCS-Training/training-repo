package com.evaitcs.securebank.notification.events;

import com.evaitcs.securebank.notification.store.NotificationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * THE CONSUMER — @KafkaListener methods are just methods that Spring Kafka
 * invokes per record on a listener-container thread. That ordinariness is
 * the testability win: NotificationListenerTest calls them DIRECTLY, no
 * broker involved.
 *
 * MESSAGE FORMAT (contract with the producers):
 *   topic "transfers": {"type":"TransferCompleted"|"TransferFailed", "reference":..., ...}
 *   topic "loans":     {"type":"LoanApproved"|"LoanRejected", ...}
 * We parse into a JsonNode and switch on "type" — the consumer-side twin of
 * the producers' hand-serialized JSON. (Schema registries + Avro formalize
 * this contract at scale; named here so students recognize the gap.)
 *
 * POISON-PILL HONESTY: a malformed message is LOGGED and skipped here. Real
 * systems route failures to a DEAD LETTER TOPIC (spring-kafka:
 * DefaultErrorHandler + DeadLetterPublishingRecoverer) so bad messages are
 * inspectable and replayable instead of silently dropped.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationStore store;
    private final JsonMapper json;

    public NotificationListener(NotificationStore store, JsonMapper json) {
        this.store = store;
        this.json = json;
    }

    @KafkaListener(topics = "transfers", groupId = "notification-service")
    public void onTransferEvent(String payload) {
        try {
            JsonNode event = json.readTree(payload);
            String type = event.path("type").asString("unknown");
            switch (type) {
                case "TransferCompleted" -> store.add("transfer",
                        "Transfer %s completed: %s from account %s to account %s".formatted(
                                event.path("reference").asString("?"),
                                money(event),
                                event.path("fromAccountId").asString("?"),
                                event.path("toAccountId").asString("?")));
                case "TransferFailed" -> store.add("transfer",
                        "Transfer %s FAILED: %s".formatted(
                                event.path("reference").asString("?"),
                                event.path("reason").asString("no reason given")));
                default -> log.warn("Unknown transfer event type '{}' — ignoring ({})", type, payload);
            }
        } catch (Exception e) {
            // Poison pill: log + skip (see class comment re: dead-letter topics).
            log.error("Unparseable transfer event ignored: {}", payload, e);
        }
    }

    @KafkaListener(topics = "loans", groupId = "notification-service")
    public void onLoanEvent(String payload) {
        try {
            JsonNode event = json.readTree(payload);
            String type = event.path("type").asString("unknown");
            switch (type) {
                case "LoanApproved" -> store.add("loan",
                        "Loan #%s APPROVED — monthly payment %s for %s months".formatted(
                                event.path("loanId").asString("?"),
                                moneyOf(event.path("monthlyPayment")),
                                event.path("termMonths").asString("?")));
                case "LoanRejected" -> store.add("loan",
                        "Loan #%s was rejected".formatted(event.path("loanId").asString("?")));
                default -> log.warn("Unknown loan event type '{}' — ignoring ({})", type, payload);
            }
        } catch (Exception e) {
            log.error("Unparseable loan event ignored: {}", payload, e);
        }
    }

    private static String money(JsonNode event) {
        return moneyOf(event.path("amount"));
    }

    private static String moneyOf(JsonNode amountNode) {
        // Amounts arrive as JSON numbers (BigDecimal semantics on the producer
        // side); render with 2 decimals for the human-facing message.
        return amountNode.isMissingNode() || amountNode.isNull()
                ? "?"
                : String.format("%,.2f", amountNode.decimalValue());
    }
}
