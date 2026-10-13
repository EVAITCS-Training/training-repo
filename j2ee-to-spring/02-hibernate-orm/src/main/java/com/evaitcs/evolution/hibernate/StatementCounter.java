package com.evaitcs.evolution.hibernate;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Counts the SQL strings Hibernate is about to prepare — the instrument that makes the
 * N+1 lesson ASSERTABLE instead of vibes. (README section "Lazy loading and N+1";
 * used by LazyAndNPlusOneDemo and by HibernateOrmTest.nPlusOneStatementCountsAreExact.)
 *
 * A StatementInspector is Hibernate's official hook: it sees every SQL string on the way
 * to the JDBC driver and returns the string to actually use. We change nothing — we only
 * record. Registration is one configuration property on the factory:
 *
 *   hibernate.session_factory.statement_inspector = <an instance of this class>
 *
 * WHY a factory-wide hook and not show_sql: show_sql PRINTS (great in demos, spam in
 * tests); the inspector COUNTS (exact numbers, assertEquals(4, counter.count())). Same
 * truth, two instruments. The list is a CopyOnWriteArrayList because a SessionFactory is
 * shared across threads — the counter must be safe even when two sessions log at once
 * (house rule: determinism first).
 */
public final class StatementCounter implements StatementInspector {

    private final List<String> statements = new CopyOnWriteArrayList<>();

    /** Hibernate calls this for every SQL string it prepares; we record it and pass it through unchanged. */
    @Override
    public String inspect(String sql) {
        statements.add(sql);
        return sql;
    }

    /** How many SQL statements have run through this counter since the last reset(). */
    public int count() {
        return statements.size();
    }

    /** The recorded SQL, oldest first — tests print it as evidence. */
    public List<String> statements() {
        return List.copyOf(statements);
    }

    /** Forget everything recorded so far (between the naive loop and the join-fetch fix). */
    public void reset() {
        statements.clear();
    }
}
