package com.evaitcs.advanced.io;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A SecureBank transaction — this module's payload for streams, buffers, files and sockets.
 *
 * WHY a RECORD that implements Serializable:
 * - Records are serializable BY DESIGN: deserialization runs the canonical constructor, so the
 *   invariants are re-checked and there are no settable fields to tamper with. TryWithResourcesDemo
 *   round-trips one through ObjectOutputStream to make that concrete (and to warn about the
 *   classic-serialization security trap).
 * - StreamDecoratorDemo writes its fields through a DataOutputStream decorator chain and reads
 *   them back, proving the Decorator composition works end to end.
 *
 * The compact constructor enforces the house money rule: an amount is ALWAYS BigDecimal scale 2.
 * Whatever scale a caller passes, the stored amount is normalized to two decimal places.
 */
public record Transaction(String id, String description, BigDecimal amount) implements Serializable {

    public Transaction {
        amount = amount.setScale(2, RoundingMode.HALF_UP);   // money is scale 2, always
    }
}
