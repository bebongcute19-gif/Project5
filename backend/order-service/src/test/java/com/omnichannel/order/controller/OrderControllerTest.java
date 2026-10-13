package com.omnichannel.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichannel.order.config.TestSecurityConfig;
import com.omnichannel.order.constant.OrderStatus;
import com.omnichannel.order.dto.request.CreateOrderRequest;
import com.omnichannel.order.dto.request.UpdateOrderStatusRequest;
import com.omnichannel.order.dto.response.OrderDTO;
import com.omnichannel.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private OrderService orderService;
    
    private UUID testCustomerId;
    private UUID testOrderId;
    private UUID testShippingAddressId;
    
    @BeforeEach
    void setUp() {
        testCustomerId = UUID.randomUUID();
        testOrderId = UUID.randomUUID();
        testShippingAddressId = UUID.randomUUID();
    }
    
    private RequestPostProcessor addUserIdAttribute(UUID userId) {
        return request -> {
            request.setAttribute("userId", userId);
            request.setAttribute("roles", List.of());  // Empty list for non-admin
            return request;
        };
    }
    
    private RequestPostProcessor addAdminRoles(UUID userId) {
        return request -> {
            request.setAttribute("userId", userId);
            request.setAttribute("roles", List.of("ROLE_ADMIN"));
            return request;
        };
    }
    
    @Test
    @WithMockUser
    void testCreateOrder_Success() throws Exception {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddressId(testShippingAddressId)
                .build();
        
        OrderDTO orderDTO = OrderDTO.builder()
                .id(testOrderId)
                .orderNumber("ORD-123456-ABC")
                .customerId(testCustomerId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100.00))
                .shippingAddressId(testShippingAddressId)
                .items(new ArrayList<>())
                .build();
        
        when(orderService.createOrder(any(UUID.class), any(CreateOrderRequest.class))).thenReturn(orderDTO);
        
        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(addUserIdAttribute(testCustomerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testOrderId.toString()))
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING));
    }
    
    @Test
    @WithMockUser
    void testGetOrder_Success() throws Exception {
        // Given
        OrderDTO orderDTO = OrderDTO.builder()
                .id(testOrderId)
                .orderNumber("ORD-123456-ABC")
                .customerId(testCustomerId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100.00))
                .shippingAddressId(testShippingAddressId)
                .items(new ArrayList<>())
                .build();
        
        when(orderService.getOrderById(testOrderId)).thenReturn(orderDTO);
        
        // When & Then
        mockMvc.perform(get("/api/v1/orders/" + testOrderId)
                .with(addUserIdAttribute(testCustomerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrderId.toString()));
    }
    
    @Test
    @WithMockUser
    void testGetOrders_Customer() throws Exception {
        // Given
        OrderDTO orderDTO = OrderDTO.builder()
                .id(testOrderId)
                .orderNumber("ORD-123456-ABC")
                .customerId(testCustomerId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100.00))
                .build();
        
        Page<OrderDTO> page = new PageImpl<>(List.of(orderDTO));
        when(orderService.getUserOrders(any(UUID.class), any())).thenReturn(page);
        
        // When & Then
        mockMvc.perform(get("/api/v1/orders?page=0&size=10")
                .with(addUserIdAttribute(testCustomerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(testOrderId.toString()));
    }
    
    @Test
    @WithMockUser
    void testCancelOrder_Success() throws Exception {
        // Given
        OrderDTO orderDTO = OrderDTO.builder()
                .id(testOrderId)
                .orderNumber("ORD-123456-ABC")
                .customerId(testCustomerId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100.00))
                .build();
        
        when(orderService.getOrderById(testOrderId)).thenReturn(orderDTO);
        doNothing().when(orderService).cancelOrder(testOrderId);
        
        // When & Then
        mockMvc.perform(put("/api/v1/orders/" + testOrderId + "/cancel")
                .with(addUserIdAttribute(testCustomerId)))
                .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateOrderStatus_Success() throws Exception {
        // Given
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();
        
        OrderDTO orderDTO = OrderDTO.builder()
                .id(testOrderId)
                .orderNumber("ORD-123456-ABC")
                .customerId(testCustomerId)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(BigDecimal.valueOf(100.00))
                .build();
        
        when(orderService.updateOrderStatus(any(UUID.class), any(UpdateOrderStatusRequest.class))).thenReturn(orderDTO);
        
        // When & Then
        mockMvc.perform(put("/api/v1/orders/" + testOrderId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(addAdminRoles(testCustomerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OrderStatus.CONFIRMED));
    }
}
