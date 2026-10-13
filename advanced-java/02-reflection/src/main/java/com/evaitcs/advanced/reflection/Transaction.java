package com.evaitcs.advanced.reflection;

import java.math.BigDecimal;

/**
 * A SecureBank transaction — this module's element type for the ONE generic field on
 * {@link Account}. It exists so InspectDemo can prove that a field's generic signature
 * (List&lt;Transaction&gt;) survives erasure and is readable with getGenericType().
 * That is the exact leftover module 01 promised reflection would find.
 *
 * WHY a record here (module 01 used a full class): records are the modern default on
 * Java 21, and reflection reads a record's components the same way it reads any fields —
 * the shape-dumping code in InspectDemo does not care which ceremony declared them.
 */
public record Transaction(String id, String description, BigDecimal amount) {
}
