package com.evaitcs.securebank.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Context-load smoke test — proves the wiring (security kit, JsonMapper
 * injection into the listener, store bean) even though the listener logic is
 * unit-tested without Spring. Kafka listener containers are disabled in the
 * test application.yml (auto-startup: false), so no broker is needed.
 */
@SpringBootTest
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
