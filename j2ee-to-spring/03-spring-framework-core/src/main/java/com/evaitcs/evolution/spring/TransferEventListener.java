package com.evaitcs.evolution.spring;

import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The subscriber half of the container's in-process publish/subscribe.
 * (README section "Events: the container's in-process message bus".)
 *
 * @EventListener on a method whose PARAMETER is the event type = "deliver every
 * TransferExecutedEvent here". No interface to implement (pre-4.2 Spring had
 * ApplicationListener<E>), no registration code — the container scans bean methods at
 * refresh and wires publisher to listener by type. Synchronous by default: the listener
 * runs on the publishing thread, INSIDE the publisher's transaction (the nuance
 * TransferService's javadoc spells out; @TransactionalEventListener(AFTER_COMMIT) is the
 * production-grade variant, and @Async makes it truly background).
 *
 * WHY it matters beyond the demo: the service states a fact and stays closed for
 * modification — a notification feature tomorrow is a NEW listener bean, zero edits here.
 * stage-5: same fact, same shape, but the bus is Kafka and the listeners live in other
 * services.
 *
 * Static RECEIVED list, cleared per demo/test — the assertion target ("the listener saw
 * exactly one event / none at all"), thread-safe per the house determinism rule.
 */
public class TransferEventListener {

    public static final List<TransferExecutedEvent> RECEIVED = new CopyOnWriteArrayList<>();

    @EventListener
    public void onTransferExecuted(TransferExecutedEvent event) {
        RECEIVED.add(event);
        System.out.println("  [listener] received TransferExecutedEvent: " + event.amount().toPlainString()
                + " from " + event.fromAccountId() + " to " + event.toAccountId()
                + " (reference " + event.reference() + ")");
    }

    public static void clearReceived() {
        RECEIVED.clear();
    }
}
