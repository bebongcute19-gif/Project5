package com.omnichannel.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnichannel.order.config.TestSecurityConfig;
import com.omnichannel.order.dto.request.AddToCartRequest;
import com.omnichannel.order.dto.response.CartDTO;
import com.omnichannel.order.dto.response.CartItemDTO;
import com.omnichannel.order.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import(TestSecurityConfig.class)
class CartControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private CartService cartService;
    
    private UUID testUserId;
    
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
    }
    
    private RequestPostProcessor addUserIdAttribute(UUID userId) {
        return request -> {
            request.setAttribute("userId", userId);
            request.setAttribute("roles", List.of());  // Empty list for non-admin
            return request;
        };
    }
    
    @Test
    @WithMockUser
    void testAddToCart_Success() throws Exception {
        // Given
        AddToCartRequest request = AddToCartRequest.builder()
                .skuCode("SKU-001")
                .quantity(2)
                .build();
        
        doNothing().when(cartService).addItemToCart(any(UUID.class), anyString(), anyInt());
        
        // When & Then
        mockMvc.perform(post("/api/v1/cart/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(addUserIdAttribute(testUserId)))
                .andExpect(status().isCreated());
    }
    
    @Test
    @WithMockUser
    void testGetCart_Success() throws Exception {
        // Given
        CartItemDTO cartItem = CartItemDTO.builder()
                .itemId(UUID.randomUUID())
                .skuCode("SKU-001")
                .productName("Test Product")
                .price(BigDecimal.valueOf(19.99))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(39.98))
                .build();
        
        CartDTO cartDTO = CartDTO.builder()
                .items(List.of(cartItem))
                .subtotal(BigDecimal.valueOf(39.98))
                .total(BigDecimal.valueOf(39.98))
                .itemCount(2)
                .build();
        
        when(cartService.getCart(any(UUID.class))).thenReturn(cartDTO);
        
        // When & Then
        mockMvc.perform(get("/api/v1/cart")
                .with(addUserIdAttribute(testUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.items[0].skuCode").value("SKU-001"));
    }
    
    @Test
    @WithMockUser
    void testClearCart_Success() throws Exception {
        // Given
        doNothing().when(cartService).clearCart(any(UUID.class));
        
        // When & Then
        mockMvc.perform(delete("/api/v1/cart")
                .with(addUserIdAttribute(testUserId)))
                .andExpect(status().isNoContent());
    }
}
