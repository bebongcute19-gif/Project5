package com.omnichannel.order.config;

import com.omnichannel.order.dto.response.InventoryDTO;
import com.omnichannel.order.security.JwtAuthenticationFilter;
import com.omnichannel.order.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.omnichannel.order.controller.InventoryController.class)
@Import(com.omnichannel.order.config.SecurityConfig.class)
public class InventorySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.context.TestConfiguration
    static class NoOpJwtFilterConfig {
        @org.springframework.context.annotation.Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new JwtAuthenticationFilter(null) {
                @Override
                protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                                jakarta.servlet.http.HttpServletResponse response,
                                                jakarta.servlet.FilterChain filterChain) throws jakarta.servlet.ServletException, java.io.IOException {
                    // simply continue the chain without authentication
                    filterChain.doFilter(request, response);
                }
            };
        }
    }

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private com.omnichannel.order.repository.InventoryRepository inventoryRepository;

    @MockBean
    private com.omnichannel.order.security.SecurityContextUtil securityContextUtil;

    @Test
    void getSku_shouldBePublic() throws Exception {
        InventoryDTO dto = InventoryDTO.builder()
                .id(UUID.randomUUID())
                .skuCode("TEST-SKU-001")
                .totalQuantity(10)
                .availableQuantity(5)
                .reservedQuantity(5)
                .version(1L)
                .build();

        when(inventoryService.getInventory("TEST-SKU-001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/TEST-SKU-001"))
                .andExpect(status().isOk());
    }

    @Test
    void getAdminInventory_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/admin/inventory"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(sc == 401 || sc == 403, "Expected 401 or 403 but was " + sc);
                });
    }

    @Test
    void postAdjust_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/adjust"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(sc == 401 || sc == 403, "Expected 401 or 403 but was " + sc);
                });
    }

    @Test
    void someProtectedEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(sc == 401 || sc == 403, "Expected 401 or 403 but was " + sc);
                });
    }
}
