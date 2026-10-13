package com.evaitcs.evolution.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Demo 1 — BOOTSTRAP: building the SessionFactory by hand, the way the 2006 era did it
 * minus the XML file. (README section "Bootstrap: one heavyweight, many cheap sessions".)
 *
 * This class is also the module's power outlet: every other demo and test builds its
 * SessionFactory through newFactory(...)/newQuietFactory(...) below — one line each, so
 * each TEST METHOD can afford a fresh database AND a fresh factory (house determinism
 * rule; Hibernate bootstrap on H2 is fast).
 *
 * Era notes:
 *   - 2006 (JPA 1.0): the persistence.xml exhibit below was mandatory; the container
 *     found it in META-INF and built the EntityManagerFactory for you.
 *   - Here: the same wiring in ~10 lines of Java — registry (connection settings),
 *     metadata (the annotated classes), factory (the heavyweight product).
 *   - stage-2: Spring builds all this from application properties — spring.datasource.url
 *     replaces the registry lines, @EntityScan replaces the addAnnotatedClass lines, and
 *     Boot hands you the EntityManagerFactory as a bean. Module 03 shows Spring managing
 *     this exact SessionFactory as a bean in between.
 */
public class BootstrapDemo {

    /**
     * JUL loggers are held WEAKLY by the LogManager — setting a level and dropping the
     * reference lets the logger be garbage-collected and the level lost. This list keeps
     * ours alive for the life of the JVM (the same trap J2eeServer handled in module 01).
     * Hibernate logs through JBoss Logging, which on this classpath routes to JUL.
     */
    private static final List<Logger> HELD_LOGGERS = new ArrayList<>();

    static {
        // Hibernate's startup chatter (version banner, dialect probing, pool warnings)
        // drowns the lesson in class. show_sql prints through System.out directly, so
        // silencing the LOGGERS never hides the SQL that IS the lesson.
        for (String name : new String[] { "org.hibernate", "org.jboss.logging" }) {
            Logger logger = Logger.getLogger(name);
            logger.setLevel(Level.SEVERE);
            logger.setUseParentHandlers(false);
            HELD_LOGGERS.add(logger);
        }
    }

    /**
     * The museum exhibit: what bootstrapping JPA looked like on disk in 2006. Every line
     * of this file has a counterpart in newFactory() below — read them side by side.
     * Spring deletes the file: stage-2's application.properties carries the same four
     * properties as spring.datasource.* / spring.jpa.*, and Boot assembles the rest.
     */
    public static final String PERSISTENCE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.0">
              <persistence-unit name="securebank">
                <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
                <class>com.evaitcs.evolution.hibernate.Customer</class>
                <class>com.evaitcs.evolution.hibernate.Account</class>
                <class>com.evaitcs.evolution.hibernate.LedgerEntry</class>
                <class>com.evaitcs.evolution.hibernate.Loan</class>
                <properties>
                  <property name="jakarta.persistence.jdbc.url"      value="jdbc:postgresql://localhost:5432/securebank"/>
                  <property name="jakarta.persistence.jdbc.user"     value="postgres"/>
                  <property name="jakarta.persistence.jdbc.password" value="(never commit real ones)"/>
                  <property name="hibernate.show_sql"                value="true"/>
                </properties>
              </persistence-unit>
            </persistence>
            """;

    public static void main(String[] args) {
        String db = DemoDb.freshDatabase();

        banner("1. The museum exhibit: persistence.xml (JPA 1.0, 2006) — the file we do NOT need");
        System.out.println(PERSISTENCE_XML);
        System.out.println("note: Spring builds all this from application properties in stage-2");
        System.out.println("      (spring.datasource.url + one starter deletes the file entirely).");

        banner("2. Programmatic bootstrap: registry -> metadata -> SessionFactory");
        System.out.println("step 1: StandardServiceRegistryBuilder holds the CONNECTION settings (the H2 url)");
        System.out.println("        — the <properties> block of the exhibit, as Java.");
        System.out.println("step 2: MetadataSources reads @Entity annotations off four classes");
        System.out.println("        — the <class> lines of the exhibit, as Java.");
        System.out.println("step 3: buildSessionFactory() parses the mappings, probes the DB for its dialect,");
        System.out.println("        and returns the ONE heavyweight object every thread will share.");
        try (SessionFactory factory = newFactory(db)) {
            System.out.println("step 4: built " + factory.getClass().getSimpleName());
            System.out.println("        verdict: thread-safe, expensive, built ONCE per application");
            System.out.println("        (stage-2: this object is the EntityManagerFactory bean Boot auto-configures).");

            banner("3. A Session is the opposite: cheap, short-lived, NOT thread-safe");
            try (Session session = factory.openSession()) {
                var tx = session.beginTransaction();
                Account first = session.find(Account.class, 1L);
                System.out.println("step 1: find(Account.class, 1L) printed ONE SELECT above; balance = " + first.getBalance());
                Account second = session.find(Account.class, 1L);
                System.out.println("step 2: find AGAIN printed NO new SELECT — the session's first-level cache");
                System.out.println("        (the persistence context) already holds id=1 with its loaded snapshot.");
                System.out.println("step 3: same object? " + (first == second)
                        + " -> one session, one identity per row (== works INSIDE a session; never across sessions).");
                tx.commit();
            }
            System.out.println("verdict: factory = the bank's vault infrastructure (built once);");
            System.out.println("         session = one teller window per unit of work (opened, used, closed).");

            banner("4. The bargain vs module 01");
            System.out.println("module 01 mapped every row by hand, every column, every entity:");
            System.out.println("    account.setBalance(rs.getBigDecimal(\"balance\"));");
            System.out.println("    account.setVersion(rs.getLong(\"version\"));   // ...x every column");
            System.out.println("module 02:");
            System.out.println("    Account a = session.find(Account.class, 1L);   // Hibernate wrote that SELECT");
            System.out.println("verdict: hand row-mapping is DEAD — but every object it returns is now MANAGED.");
            System.out.println("         Demos 2-5 are the price of that word: a state machine you must respect.");
        }
    }

    // ---- the module's power outlet ---------------------------------------------------

    /** A factory with show_sql ON — for demos, where the emitted SQL is the lesson. */
    public static SessionFactory newFactory(String dbUrl) {
        return newFactory(dbUrl, true, null);
    }

    /** show_sql ON plus a StatementInspector — for demos that also COUNT their trips (N+1). */
    public static SessionFactory newFactory(String dbUrl, StatementInspector inspector) {
        return newFactory(dbUrl, true, inspector);
    }

    /** A factory with show_sql OFF — for tests, where the inspector counts and printing would spam. */
    public static SessionFactory newQuietFactory(String dbUrl) {
        return newFactory(dbUrl, false, null);
    }

    /** show_sql OFF plus the counting inspector — the N+1 test's instrument. */
    public static SessionFactory newQuietFactory(String dbUrl, StatementInspector inspector) {
        return newFactory(dbUrl, false, inspector);
    }

    /**
     * Walkthrough — what happens when this runs (README "Bootstrap" walkthrough, live):
     *   1. the registry builder collects settings under the SAME property names the
     *      persistence.xml exhibit uses (jakarta.persistence.jdbc.url, hibernate.show_sql);
     *   2. build() creates the StandardServiceRegistry — connection provider, transaction
     *      coordinator, JDBC services (the statement inspector plugs in here);
     *   3. MetadataSources parses the four annotated classes into Metadata: tables,
     *      columns, associations, the works — no XML, no scanning, just the class list;
     *   4. buildSessionFactory() probes the database once for its dialect (H2 here),
     *      wires the pooled services together, and returns the thread-safe heavyweight.
     * No user/password settings on purpose: DemoDb opened this H2 database with EMPTY
     * credentials, and H2 checks credentials on every connection — matching them matters.
     * Closing the factory (try-with-resources everywhere) releases the registry and pool.
     */
    private static SessionFactory newFactory(String dbUrl, boolean showSql, StatementInspector inspector) {
        StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
                .applySetting("jakarta.persistence.jdbc.url", dbUrl)
                .applySetting("hibernate.show_sql", showSql);
        if (inspector != null) {
            // The N+1 instrument: every SQL string Hibernate prepares flows through it.
            registryBuilder.applySetting("hibernate.session_factory.statement_inspector", inspector);
        }
        StandardServiceRegistry registry = registryBuilder.build();
        return new MetadataSources(registry)
                .addAnnotatedClass(Customer.class)
                .addAnnotatedClass(Account.class)
                .addAnnotatedClass(LedgerEntry.class)
                .addAnnotatedClass(Loan.class)
                .buildMetadata()
                .buildSessionFactory();
    }

    static void banner(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
