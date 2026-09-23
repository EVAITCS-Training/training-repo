package com.evaitcs.securebank;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * SMOKE TEST — "does the whole application context start?"
 *
 * @SpringBootTest boots EVERYTHING: embedded Tomcat-less mock environment,
 * all beans, H2 database, security chain, DataSeeder. If any wiring is broken
 * (a missing bean, a bad property, a circular dependency), this fails first
 * and with the clearest error. Every other test may assume the app boots.
 *
 * CI (Stage 4) runs this on every push — the cheapest possible guarantee
 * that main is deployable.
 */
@SpringBootTest
class SecureBankApplicationTests {

    @Test
    void contextLoads() {
        // No assertions needed — a failing context load IS the failure.
    }
}
