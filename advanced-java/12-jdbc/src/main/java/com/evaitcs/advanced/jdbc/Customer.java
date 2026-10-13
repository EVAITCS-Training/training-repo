package com.evaitcs.advanced.jdbc;

import java.time.OffsetDateTime;

/**
 * One row of stage-1's `customer` table as a record — including the java.time
 * mapping this module teaches (README "Reading rows", step 4).
 *
 *   created_at TIMESTAMPTZ -> OffsetDateTime via rs.getObject("created_at", OffsetDateTime.class)
 *
 * TIMESTAMPTZ stores a UTC instant; OffsetDateTime is the JDBC 4.2 type that
 * carries the same information without the mutability and pre-1970 confusion of
 * java.sql.Timestamp (ReadingDemo shows the legacy bridge once, and says why
 * java.time is preferred — tie-in: java8-features module 08-java-time).
 */
public record Customer(long id, String name, String email, OffsetDateTime createdAt) {
}
