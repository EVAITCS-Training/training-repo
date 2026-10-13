package com.evaitcs.evolution.j2ee;

/**
 * Launcher for the whole module — runs every demo main() in teaching order.
 *
 *   .\mvnw.cmd -pl 01-j2ee-the-world-before-spring compile exec:java
 *
 * The order is the argument: first the MUSEUM (what J2EE ceremony looked like and why
 * 2002 revolted), then the PLUMBING live (the hand-cranked transaction that actually
 * works — and refuses-and-rolls-back correctly), then the WHOLE STACK on real HTTP
 * (embedded Tomcat, a real servlet, the five things we hand-wired that Boot deletes).
 *
 * To run a single demo while pointing at it:
 *   .\mvnw.cmd -pl 01-j2ee-the-world-before-spring compile exec:java "-Dexec.mainClass=com.evaitcs.evolution.j2ee.LiveServletDemo"
 */
public class J2eeDemos {

    public static void main(String[] args) throws Exception {
        section("TheCeremonyDemo — five artifacts to move money once: the EJB 2.x museum");
        TheCeremonyDemo.main(args);

        section("ManualJdbcPainDemo — the hand-cranked transaction: commit, rollback, close, all yours");
        ManualJdbcPainDemo.main(args);

        section("LiveServletDemo — the whole J2EE stack live: real HTTP, embedded Tomcat, ephemeral port");
        LiveServletDemo.main(args);
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("############################################################");
        System.out.println("# " + title);
        System.out.println("############################################################");
    }
}
