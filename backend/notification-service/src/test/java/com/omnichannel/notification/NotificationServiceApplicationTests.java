package com.omnichannel.notification;

import com.omnichannel.notification.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationServiceApplicationTests {

    @Test
    void healthControllerCheck() {
        HealthController controller = new HealthController();
        ResponseEntity<Map<String, Object>> response = controller.checkHealth();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("notification-service", response.getBody().get("service"));
    }
}
