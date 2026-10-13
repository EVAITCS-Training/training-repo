package com.evaitcs.advanced.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The RowMapper pattern: one function that turns the row a ResultSet cursor
 * currently sits on into a domain object (README section "Reading rows").
 *
 * This tiny interface IS the thing Spring's JdbcTemplate hands you as a lambda
 * and the thing Hibernate automates with ResultSetMetaData plus reflection
 * (MetadataDemo shows the metadata Hibernate reads). Naming it once here makes
 * the automation recognizable later: JdbcTemplate.query(sql, rowMapper) is a
 * for-loop over rs.next() that calls exactly this function — nothing more.
 *
 * A functional interface (one abstract method), so a lambda can be passed where
 * a RowMapper is expected — the Java 8 payoff (java8-features, module 02).
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps the CURRENT row (the one rs.next() just advanced onto) to a T.
     * Declared to throw SQLException so implementers can call rs getters
     * directly — the loop that drives the mapper handles the exception once.
     */
    T map(ResultSet rs) throws SQLException;
}
