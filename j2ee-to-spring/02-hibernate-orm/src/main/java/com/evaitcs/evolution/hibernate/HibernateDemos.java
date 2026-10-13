package com.evaitcs.evolution.hibernate;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 02-hibernate-orm compile exec:java
 *
 * The order is the argument: first BOOTSTRAP (how the SessionFactory gets built at all —
 * the persistence.xml museum beside the Java that replaces it), then THE BARGAIN (dirty
 * checking writes your UPDATEs), then THE PRICE: the lifecycle state machine, lazy
 * loading and its deadline, the counted N+1 and its join-fetch fix, optimistic locking
 * via @Version — and finally the QUERY TOUR (JPQL, Criteria, native SQL, record
 * projections) that shows what asking the database looks like once rows are objects.
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 02-hibernate-orm compile exec:java "-Dexec.mainClass=com.evaitcs.evolution.hibernate.OptimisticLockingDemo"
 */
public class HibernateDemos {

    public static void main(String[] args) {
        section("BootstrapDemo — building the SessionFactory by hand: registry, metadata, the persistence.xml we skipped");
        BootstrapDemo.main(args);

        section("DirtyCheckingDemo — the ORM bargain: change an object, commit, watch Hibernate write the UPDATE");
        DirtyCheckingDemo.main(args);

        section("LifecycleStatesDemo — the price: transient, persistent, detached, removed, and merge()'s gotcha");
        LifecycleStatesDemo.main(args);

        section("LazyAndNPlusOneDemo — lazy is a promise with a deadline; N+1 counted, then fixed with join fetch");
        LazyAndNPlusOneDemo.main(args);

        section("OptimisticLockingDemo — @Version: the second committer fails fast instead of losing an update");
        OptimisticLockingDemo.main(args);

        section("QueryTourDemo — JPQL, Criteria, native SQL, and a constructor expression into a Java 21 record");
        QueryTourDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
