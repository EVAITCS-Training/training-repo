package com.evaitcs.advanced.jdbc;

import java.math.BigDecimal;

/**
 * One row of stage-1's `account` table as a Java 21 record — the row DTO that
 * ReadingDemo's RowMapper produces and JdbcTest asserts on.
 *
 * Column mapping (schema.sql is the source of truth):
 *   id          BIGINT          -> long
 *   customer_id BIGINT          -> long
 *   type        VARCHAR(20)     -> String ("CHECKING" or "SAVINGS")
 *   balance     DECIMAL(15,2)   -> BigDecimal, scale 2 — NEVER float for money
 *   version     BIGINT          -> long (Hibernate's @Version optimistic lock, stage-2)
 *
 * Domain classes are duplicated per module on purpose (course convention) so this
 * module stands alone on screen.
 */
public record Account(long id, long customerId, String type, BigDecimal balance, long version) {
}
