package com.evaitcs.evolution.app.era2;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * ERA 2 — the standalone-Hibernate bootstrap, copied from module 02's BootstrapDemo
 * newFactory() pattern: registry (connection settings) -> metadata (the annotated
 * classes) -> SessionFactory (the ONE heavyweight every request thread shares).
 *
 * Two era notes:
 *   - In 2006 this wiring lived in persistence.xml (module 02 keeps the museum exhibit)
 *     and a ServletContextListener built the factory at deploy time, parking it in the
 *     ServletContext. Here EvolutionServer.start() calls newFactory() and publishes the
 *     product through the SAME ServiceLocator era 1 used — Hibernate fixed row-mapping,
 *     NOT wiring; wiring is era 3's deletion list.
 *   - Only the three entities the transfer touches are registered (Customer, Account,
 *     LedgerEntry). Module 02 also mapped Loan; an unmapped table is simply invisible
 *     to Hibernate — the schema DemoDb created still has it, and era 1/era 3 could
 *     still query it with SQL.
 *
 * stage-2 counterpart: spring.datasource.url + spring.jpa.* replace every line below
 * (HibernateJpaAutoConfiguration builds the EntityManagerFactory bean for you).
 */
public final class HibernateBootstrap {

    private HibernateBootstrap() {
    }

    /**
     * Walkthrough — what happens when this runs (module 02's "Bootstrap" walkthrough, live):
     *   1. the registry builder collects settings under the SAME property names
     *      persistence.xml used (jakarta.persistence.jdbc.url, hibernate.show_sql);
     *   2. build() creates the StandardServiceRegistry — connection provider,
     *      transaction coordinator, JDBC services;
     *   3. MetadataSources parses the three annotated classes into Metadata: tables,
     *      columns, associations — no XML, no scanning, just the class list;
     *   4. buildSessionFactory() probes the database once for its dialect (H2 here),
     *      wires the pooled services together, and returns the thread-safe heavyweight.
     * No user/password settings on purpose: DemoDb opened this H2 database with EMPTY
     * credentials, and H2 checks credentials on every connection — matching them matters.
     */
    public static SessionFactory newFactory(String dbUrl) {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("jakarta.persistence.jdbc.url", dbUrl)
                .applySetting("hibernate.show_sql", false)   // quiet: the SQL lesson is module 02's; here the plumbing is
                .build();
        return new MetadataSources(registry)
                .addAnnotatedClass(Customer.class)
                .addAnnotatedClass(Account.class)
                .addAnnotatedClass(LedgerEntry.class)
                .buildMetadata()
                .buildSessionFactory();
    }
}
