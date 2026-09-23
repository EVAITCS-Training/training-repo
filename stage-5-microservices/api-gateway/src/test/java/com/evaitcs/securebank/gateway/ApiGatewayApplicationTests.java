package com.evaitcs.securebank.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Context-load smoke test: proves the route YAML parses and the gateway
 * wiring boots. Route BEHAVIOR needs a live Eureka + services, which is what
 * the compose stack smoke test (stage README) demonstrates.
 */
@SpringBootTest
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
