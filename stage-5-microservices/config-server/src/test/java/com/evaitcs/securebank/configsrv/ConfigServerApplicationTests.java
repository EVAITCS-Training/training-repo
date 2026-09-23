package com.evaitcs.securebank.configsrv;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Context-load smoke test. native profile + a search-locations path that may
 * not exist on CI is FINE: the server boots regardless and answers requests
 * from whatever the location resolves to at runtime.
 */
@SpringBootTest
class ConfigServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
