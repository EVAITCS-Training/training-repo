package com.evaitcs.securebank.notification.events;

import com.evaitcs.securebank.notification.store.NotificationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Listener unit tests — @KafkaListener methods invoked like plain methods
 * (no broker, no Spring context). This is why the listener takes its
 * dependencies via constructor and parses hand-written JSON strings: the
 * async pipeline's logic stays as testable as any service class.
 */
class NotificationListenerTest {

    NotificationStore store;
    NotificationListener listener;

    @BeforeEach
    void setUp() {
        store = new NotificationStore();
        listener = new NotificationListener(store, JsonMapper.builder().build());
    }

    @Test
    @DisplayName("TransferCompleted becomes a human receipt")
    void transferCompleted() {
        listener.onTransferEvent("""
                {"type":"TransferCompleted","reference":"xfer-42","fromAccountId":1,
                 "toAccountId":2,"amount":100.00,"at":"2026-01-01T10:00:00Z"}
                """);

        assertThat(store.recent()).singleElement().satisfies(n -> {
            assertThat(n.kind()).isEqualTo("transfer");
            assertThat(n.message()).contains("xfer-42").contains("100.00")
                    .contains("from account 1").contains("to account 2");
        });
    }

    @Test
    @DisplayName("TransferFailed carries the reason through")
    void transferFailed() {
        listener.onTransferEvent("""
                {"type":"TransferFailed","reference":"xfer-43","fromAccountId":1,
                 "toAccountId":2,"amount":9999.00,"reason":"Insufficient funds","at":"2026-01-01T10:00:00Z"}
                """);

        assertThat(store.recent()).singleElement()
                .satisfies(n -> assertThat(n.message()).contains("FAILED").contains("Insufficient funds"));
    }

    @Test
    @DisplayName("LoanApproved formats the monthly payment")
    void loanApproved() {
        listener.onLoanEvent("""
                {"type":"LoanApproved","loanId":7,"customerId":1,"principal":10000.00,
                 "termMonths":36,"monthlyPayment":311.11,"at":"2026-01-01T10:00:00Z"}
                """);

        assertThat(store.recent()).singleElement()
                .satisfies(n -> assertThat(n.message()).contains("APPROVED").contains("311.11"));
    }

    @Test
    @DisplayName("poison pills are logged and skipped, never thrown")
    void malformedPayloadSurvives() {
        listener.onTransferEvent("this is not json {{{");
        listener.onTransferEvent("""
                {"type":"SomethingWeird"}
                """);

        assertThat(store.recent()).isEmpty(); // nothing stored, nothing thrown
    }
}
