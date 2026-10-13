package com.evaitcs.advanced.locks;

import java.math.BigDecimal;

/**
 * A SecureBank transaction — the item that flows through this module's bounded buffer.
 *
 * WHY an immutable record: BoundedBufferDemo hands Transactions from a producer thread to
 * a consumer thread. Because a record's components are FINAL, the Java Memory Model
 * publishes them safely (module 04, "safe publication") — the consumer always sees a
 * fully-built transaction, so the buffer's lock only has to guard the QUEUE, not the items.
 * Immutable payloads keep concurrent code small.
 *
 * Money is BigDecimal scale 2 (house rule); duplicated per module on purpose.
 */
public record Transaction(String id, String description, BigDecimal amount) {

    /** Convenience factory that keeps the money scale at exactly 2 (house rule). */
    public static Transaction of(String id, String description, String amount) {
        return new Transaction(id, description, new BigDecimal(amount).setScale(2));
    }
}
