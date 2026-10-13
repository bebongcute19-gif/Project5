package com.omnichannel.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
