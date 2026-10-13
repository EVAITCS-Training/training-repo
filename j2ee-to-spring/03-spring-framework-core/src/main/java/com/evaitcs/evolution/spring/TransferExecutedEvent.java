package com.evaitcs.evolution.spring;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * "A transfer happened" — an immutable fact, as a record.
 * (README section "Events: the container's in-process message bus".)
 *
 * A plain object: no base class, no interface, no registration — since Spring 4.2 any
 * object can be an event. The service publishes it through ApplicationEventPublisher;
 * every @EventListener whose parameter type matches hears it, synchronously, in-process.
 *
 * WHY a bank app wants this: the transfer itself should not grow an if-chain of side
 * effects (notify, audit, report...). Publishers state facts; subscribers decide what to
 * do with them — new subscribers never touch the service. stage-5 grows this exact shape
 * into Kafka events crossing service boundaries; the record travels almost unchanged.
 */
public record TransferExecutedEvent(
        String reference,
        long fromAccountId,
        long toAccountId,
        BigDecimal amount,
        Instant at) {
}
