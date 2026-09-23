package com.evaitcs.securebank.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Context-load smoke test. The server-specific settings in application.yml
 * (register-with-eureka=false, fetch-registry=false) are what make a Eureka
 * SERVER bootable in a test without a cluster to talk to.
 */
@SpringBootTest
class DiscoveryServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
