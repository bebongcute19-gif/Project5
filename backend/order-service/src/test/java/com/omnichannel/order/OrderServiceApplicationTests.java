package com.omnichannel.order;

import com.omnichannel.order.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrderServiceApplicationTests {

    @Test
    void healthControllerCheck() {
        HealthController controller = new HealthController();
        ResponseEntity<Map<String, Object>> response = controller.checkHealth();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("order-service", response.getBody().get("service"));
    }
}
