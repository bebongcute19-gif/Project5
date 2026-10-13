package com.omnichannel.product;

import com.omnichannel.product.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductServiceApplicationTests {

    @Test
    void healthControllerCheck() {
        HealthController controller = new HealthController();
        ResponseEntity<Map<String, Object>> response = controller.checkHealth();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("product-service", response.getBody().get("service"));
    }
}
