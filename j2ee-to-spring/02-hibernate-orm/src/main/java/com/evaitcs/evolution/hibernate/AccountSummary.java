package com.evaitcs.evolution.hibernate;

import java.math.BigDecimal;

/**
 * A PROJECTION, not an entity: three account columns flattened into one immutable
 * Java 21 record. (README section "Queries: four ways to ask"; built by the JPQL
 * constructor expression in QueryTourDemo banner 5.)
 *
 * WHY a record is right HERE but wrong for entities: a projection is read once and
 * never written back — no dirty checking, no lifecycle, no session. Immutability is a
 * feature. An entity is the opposite: Hibernate must mutate its fields in place across
 * a flush, which a final record forbids (Customer.java's javadoc, the README's Java 21
 * corner). Before records (Java 16+), this class was a 40-line DTO with getters.
 *
 * The JPQL that fills it:
 *   select new com.evaitcs.evolution.hibernate.AccountSummary(a.id, a.balance) from Account a
 * — a constructor expression: the query names the class, Hibernate calls the canonical
 * constructor per row. No entity, no session, no lazy surprises afterwards.
 */
public record AccountSummary(Long id, BigDecimal balance) {
}
