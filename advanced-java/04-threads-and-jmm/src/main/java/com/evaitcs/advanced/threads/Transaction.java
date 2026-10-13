package com.evaitcs.advanced.threads;

import java.math.BigDecimal;

/**
 * A SecureBank transaction — this module's SAFE-PUBLICATION poster child.
 *
 * WHY it is an immutable record (Java 21):
 * The Java Memory Model guarantees that the FINAL fields of a correctly-constructed
 * object are visible to every thread WITHOUT synchronization — a rule called *safe
 * publication* (README section "Safe publication: why immutable records are free").
 * A record's components are all final, so handing a Transaction to another thread
 * through a plain data race still shows the right id, description and amount. There
 * is no half-built window to catch. Immutable records are inherently thread-safe.
 *
 * Money is BigDecimal scale 2 (house rule); duplicated per module on purpose so this
 * module stands alone on screen.
 */
public record Transaction(String id, String description, BigDecimal amount) {

    /** Convenience factory that keeps the money scale at exactly 2 (house rule). */
    public static Transaction of(String id, String description, String amount) {
        return new Transaction(id, description, new BigDecimal(amount).setScale(2));
    }
}
